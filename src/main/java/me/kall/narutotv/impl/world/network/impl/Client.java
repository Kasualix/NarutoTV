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
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import me.kall.narutotv.impl.world.data.client.ClientWalls;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.wall.WallUpdatePacket;
import me.kall.narutotv.impl.world.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Client {
    public static void removeWall(Wall wall) {
        ClientWalls.remove(wall).ifPresent(AbstractRenderer::shutdown);
    }

    public static void configureWall(Wall wall) {
        Minecraft.getInstance().setScreen(new NarutoWorldScreen(Minecraft.getInstance().screen, wall));
    }

    public static void updateWall(Wall wall) {
        AbstractRenderer<?> current = ClientWalls.get(wall);
        if (current == null) return;
        Wall existing = ((InWorld)current).wall();
        if (Objects.equals(existing.video, wall.video) && Objects.equals(existing.audio, wall.audio) && existing.localSound.equals(wall.localSound) && wall.volume != existing.volume) {
            current.setVolume(wall.volume);
            existing.volume = wall.volume;
        } else {
            Sources.cutInLine(NarutoPaths.absConfig(wall.video), NarutoPaths.absConfig(wall.audio));
            ClientWalls.add(wall).ifPresent(AbstractRenderer::shutdown);
            AbstractRenderer<?> updated = ClientWalls.get(wall);
            assert updated != null;
            updated.setup(0D);
        }
    }

    public static void addWall(Wall wall) {
        ClientWalls.add(wall).ifPresent(AbstractRenderer::shutdown);

        AbstractRenderer<?> renderer = ClientWalls.get(wall);

        assert renderer != null;
        renderer.setup(0D);

        MediaArgs mediaArgs = renderer.mediaArgs;
        assert mediaArgs != null;

        wall.video = NarutoPaths.relConfig(mediaArgs.absVideoPath());
        wall.audio = NarutoPaths.relConfig(mediaArgs.absAudioPath());

        NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(wall));
    }

    public static void syncWalls(@NotNull ObjectCollection<ObjectOpenHashSet<Wall>> walls) {
        walls.forEach(set -> set.forEach(wall -> {
            Runnable validation = () -> {
                ClientWalls.add(wall).ifPresent(AbstractRenderer::shutdown);

                AbstractRenderer<?> renderer = ClientWalls.get(wall);
                assert renderer != null;

                Sources.cutInLine(NarutoPaths.absConfig(wall.video), NarutoPaths.absConfig(wall.audio));

                renderer.shutdown();
                renderer.setup(0D);
            };

            if (wall.hasLocalSound()) {
                AppInstances.ffmpeg().convertAudio(NarutoPaths.absConfig(wall.audio)).whenCompleteAsync((converted, throwable) -> {
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

    public static void syncCapes(@NotNull List<UUID> players, @NotNull List<String> capes) {
        for (int index = 0; index < players.size(); index++) {
            ClientVideoCapes.register(players.get(index), NarutoPaths.absConfig(capes.get(index)));
        }
    }
}
