package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.impl.Server;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
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
                if (context.getDirection().equals(NetworkDirection.PLAY_TO_CLIENT)) {
                    Client.updateWall(this.wall);
                } else {
                    Server.updateWall(this.wall, context);
                }
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling ScreenUpdatePacket", throwable);
            }
        });
    }
}
