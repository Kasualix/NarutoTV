package me.kall.narutotv.impl.world.network.impl;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.ScreenUpdatePacket;
import me.kall.narutotv.impl.world.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class Client {
    public static void removeScreen(BlockScreen blockScreen) {
        ClientRenderers.remove(blockScreen).ifPresent(AbstractRenderer::shutdown);
    }

    public static void setScreen(BlockScreen blockScreen) {
        Minecraft.getInstance().setScreen(new NarutoWorldScreen(Minecraft.getInstance().screen, blockScreen));
    }

    public static void addScreen(BlockScreen blockScreen) {
        ClientRenderers.add(blockScreen).ifPresent(AbstractRenderer::shutdown);

        AbstractRenderer<?> renderer = ClientRenderers.get(blockScreen);

        assert renderer != null;
        renderer.setup(0D);

        MediaArgs mediaArgs = renderer.mediaArgs();
        assert mediaArgs != null;

        blockScreen.video = Paths.relative(mediaArgs.absVideoPath());
        blockScreen.audio = Paths.relative(mediaArgs.absAudioPath());

        NarutoPackets.INSTANCE.sendToServer(new ScreenUpdatePacket(blockScreen));
    }

    public static void syncScreens(@NotNull ObjectCollection<ObjectOpenHashSet<BlockScreen>> blockScreens) {
        blockScreens.forEach(screens -> screens.forEach(screen -> {
            Runnable validation = () -> {
                ClientRenderers.add(screen).ifPresent(AbstractRenderer::shutdown);

                AbstractRenderer<?> renderer = ClientRenderers.get(screen);
                assert renderer != null;

                Sources.cutInLine(Paths.absolute(screen.video), Paths.absolute(screen.audio));

                renderer.shutdown();
                renderer.setup(0D);
            };

            if (screen.hasLocalSound()) {
                AppInstances.ffmpeg().convertAudio(Paths.absolute(screen.audio)).whenCompleteAsync((converted, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace(System.err);
                        throw new RuntimeException(throwable);
                    }
                    AudioZipGenerator.get(converted).generate(id -> validation.run());
                });
            } else {
                validation.run();
            }
        }));
    }
}
