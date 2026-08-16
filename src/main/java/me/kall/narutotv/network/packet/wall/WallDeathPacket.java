package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class WallDeathPacket extends WallPacket {
    public WallDeathPacket(Wall wall) {
        super(wall);
    }

    public WallDeathPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Client.removeWall(this.wall);
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handing ScreenDeathPacket", throwable);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return NarutoPackets.WALL_DEATH_PACKET_TYPE;
    }
}
