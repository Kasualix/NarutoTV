package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Server;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class WallCleanPacket extends WallPacket {
    public WallCleanPacket(Wall wall) {
        super(wall);
    }

    public WallCleanPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Server.cleanWall(this.wall, context);
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling ScreenCleanPacket", throwable);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return NarutoPackets.WALL_CLEAN_PACKET_TYPE;
    }
}
