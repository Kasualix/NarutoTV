package me.kall.narutotv.impl.world.network.packet.wall;

import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.impl.Server;
import me.kall.narutotv.impl.world.network.packet.wall.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class WallCleanPacket extends WallPacket {
    public WallCleanPacket(Wall wall) {
        super(wall);
    }

    public WallCleanPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Server.cleanWall(this.wall, context);
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling ScreenCleanPacket", throwable);
            }
        });
    }
}
