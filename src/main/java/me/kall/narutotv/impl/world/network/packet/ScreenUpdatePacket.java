package me.kall.narutotv.impl.world.network.packet;

import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.network.impl.Server;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenUpdatePacket extends ScreenPacket {
    public ScreenUpdatePacket(BlockScreen blockScreen) {
        super(blockScreen);
    }

    public ScreenUpdatePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Server.updateScreen(this.blockScreen, context);
            } catch (Throwable throwable) {
                LOGGER.error("Error handling ScreenUpdatePacket", throwable);
            }
        });
    }
}
