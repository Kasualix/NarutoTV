package me.kall.narutotv.impl.world.network.packet;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.network.impl.Client;
import me.kall.narutotv.impl.world.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class WallSyncPacket {
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
                screens.add(new Wall(corners, dimension, localSound, video, audio, volume));
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
            });
        });
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Client.syncWalls(this.walls);
            } catch (Throwable throwable) {
                WallPacket.LOGGER.error("Error handing ScreenSyncPacket", throwable);
            }
        });
    }
}
