package me.kall.narutotv.network.packet.wall;


import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.packet.base.Handleable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WallSyncPacket extends Handleable {
    private final ObjectCollection<ObjectOpenHashSet<Wall>> walls;

    public WallSyncPacket(ObjectCollection<ObjectOpenHashSet<Wall>> walls) {
        this.walls = walls;
    }

    public WallSyncPacket(@NotNull FriendlyByteBuf buffer) {
        int outerSize = buffer.readVarInt();
        this.walls = new ObjectOpenHashSet<>(outerSize);
        for (int outerIndex = 0; outerIndex < outerSize; outerIndex++) {
            int innerSize = buffer.readVarInt();
            ObjectOpenHashSet<Wall> screens = new ObjectOpenHashSet<>();
            for (int innerIndex = 0; innerIndex < innerSize; innerIndex++) {
                long[] corners = buffer.readLongArray();
                ResourceLocation dimension = buffer.readResourceLocation();
                ResourceLocation localSound = buffer.readResourceLocation();
                String video = buffer.readUtf();
                String audio = buffer.readUtf();
                float volume = buffer.readFloat();
                boolean light = buffer.readBoolean();
                screens.add(new Wall(corners, dimension, localSound, video, audio, volume, light));
            }
            this.walls.add(screens);
        }
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.walls.size());
        this.walls.forEach(set -> {
            buffer.writeVarInt(set.size());
            set.forEach(wall -> {
                buffer.writeLongArray(wall.toLongArray());
                buffer.writeResourceLocation(wall.dimension);
                buffer.writeResourceLocation(wall.localSound);
                buffer.writeUtf(wall.video);
                buffer.writeUtf(wall.audio);
                buffer.writeFloat(wall.volume);
                buffer.writeBoolean(wall.light);
            });
        });
    }

    @Override
    public void handle(@Nullable ServerPlayer player, boolean s2c) {
        try {
            Client.syncWalls(this.walls);
        } catch (Throwable throwable) {
            NarutoPackets.LOGGER.error("Error handing ScreenSyncPacket", throwable);
        }
    }
}
