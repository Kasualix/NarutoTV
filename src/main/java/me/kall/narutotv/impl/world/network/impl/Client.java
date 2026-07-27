package me.kall.narutotv.impl.world.network.impl;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.base.data.NarutoPaths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.screen.NarutoWorldScreen;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.WallUpdatePacket;
import me.kall.narutotv.impl.world.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class Client {
    public static void removeWall(Wall wall) {
        ClientRenderers.remove(wall).ifPresent(AbstractRenderer::shutdown);
    }

    public static void configureWall(Wall wall) {
        Minecraft.getInstance().setScreen(new NarutoWorldScreen(Minecraft.getInstance().screen, wall));
    }

    public static void addWall(Wall wall) {
        ClientRenderers.add(wall).ifPresent(AbstractRenderer::shutdown);

        AbstractRenderer<?> renderer = ClientRenderers.get(wall);

        assert renderer != null;
        renderer.setup(0D);

        MediaArgs mediaArgs = renderer.mediaArgs();
        assert mediaArgs != null;

        wall.video = NarutoPaths.relative(mediaArgs.absVideoPath());
        wall.audio = NarutoPaths.relative(mediaArgs.absAudioPath());

        NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(wall));
    }

    public static void syncWalls(@NotNull ObjectCollection<ObjectOpenHashSet<Wall>> walls) {
        walls.forEach(set -> set.forEach(wall -> {
            Runnable validation = () -> {
                ClientRenderers.add(wall).ifPresent(AbstractRenderer::shutdown);

                AbstractRenderer<?> renderer = ClientRenderers.get(wall);
                assert renderer != null;

                Sources.cutInLine(NarutoPaths.absolute(wall.video), NarutoPaths.absolute(wall.audio));

                renderer.shutdown();
                renderer.setup(0D);
            };

            if (wall.hasLocalSound()) {
                AppInstances.ffmpeg().convertAudio(NarutoPaths.absolute(wall.audio)).whenCompleteAsync((converted, throwable) -> {
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
