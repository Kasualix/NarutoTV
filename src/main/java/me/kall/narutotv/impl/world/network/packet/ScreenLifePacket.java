package me.kall.narutotv.impl.world.network.packet;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenLifePacket extends ScreenPacket {
    public ScreenLifePacket(BlockScreen blockScreen) {
        super(blockScreen);
    }

    public ScreenLifePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                ClientRenderers.add(this.blockScreen).ifPresent(AbstractRenderer::shutdown);

                AbstractRenderer<?> renderer = ClientRenderers.get(this.blockScreen);

                if (renderer == null) return;
                renderer.setup(0D);

                MediaArgs mediaArgs = renderer.mediaArgs();
                if (mediaArgs == null) return;

                this.blockScreen.video = Paths.relative(mediaArgs.absVideoPath());
                this.blockScreen.audio = Paths.relative(mediaArgs.absAudioPath());

                NarutoPackets.INSTANCE.sendToServer(new ScreenUpdatePacket(this.blockScreen));
            } catch (Throwable throwable) {
                LOGGER.error("Error handing ScreenLifePacket", throwable);
            }
        });
    }
}
