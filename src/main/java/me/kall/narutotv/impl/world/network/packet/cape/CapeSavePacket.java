package me.kall.narutotv.impl.world.network.packet.cape;

import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.impl.world.data.SavedCapePaths;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Supplier;

public class CapeSavePacket {
    private final UUID player;
    private final String absVideoPath;

    public CapeSavePacket(UUID player, String absVideoPath) {
        this.player = player;
        this.absVideoPath = absVideoPath;
    }

    public CapeSavePacket(@NotNull FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readUtf());
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUUID(this.player);
        buffer.writeUtf(this.absVideoPath);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                ServerPlayer sender = context.getSender();
                if (sender == null) return;
                SavedCapePaths.get(sender.serverLevel()).add(this.player, NarutoPaths.relative(this.absVideoPath));
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling CapeSavePacket", throwable);
            }
        });
    }
}
