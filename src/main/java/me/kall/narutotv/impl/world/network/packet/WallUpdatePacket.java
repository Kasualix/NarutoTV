package me.kall.narutotv.impl.world.network.packet;

import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.network.impl.Server;
import me.kall.narutotv.impl.world.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class WallUpdatePacket extends WallPacket {
    public WallUpdatePacket(Wall wall) {
        super(wall);
    }

    public WallUpdatePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Server.updateWall(this.wall, context);
            } catch (Throwable throwable) {
                LOGGER.error("Error handling ScreenUpdatePacket", throwable);
            }
        });
    }
}
