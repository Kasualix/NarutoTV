package me.kall.narutotv.impl.world.network.packet;

import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.network.impl.Client;
import me.kall.narutotv.impl.world.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class WallLifePacket extends WallPacket {
    public WallLifePacket(Wall wall) {
        super(wall);
    }

    public WallLifePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Client.addWall(this.wall);
            } catch (Throwable throwable) {
                LOGGER.error("Error handing ScreenLifePacket", throwable);
            }
        });
    }
}
