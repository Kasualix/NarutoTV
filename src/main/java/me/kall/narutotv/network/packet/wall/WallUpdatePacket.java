package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.impl.Server;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class WallUpdatePacket extends WallPacket {
    public WallUpdatePacket(Wall wall) {
        super(wall);
    }

    public WallUpdatePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                if (!(context.player() instanceof ServerPlayer)) {
                    Client.updateWall(this.wall);
                } else {
                    Server.updateWall(this.wall, context);
                }
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling ScreenUpdatePacket", throwable);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return NarutoPackets.WALL_UPDATE_PACKET_TYPE;
    }
}
