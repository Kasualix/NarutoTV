package me.kall.narutotv.impl.world.network.packet;

import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenDeathPacket extends ScreenPacket {
    public ScreenDeathPacket(BlockScreen blockScreen) {
        super(blockScreen);
    }

    public ScreenDeathPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                ClientRenderers.remove(this.blockScreen).ifPresent(AbstractRenderer::shutdown);
            } catch (Throwable throwable) {
                LOGGER.error("Error handing ScreenDeathPacket", throwable);
            }
        });
    }
}
