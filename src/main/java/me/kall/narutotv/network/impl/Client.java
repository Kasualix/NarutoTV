package me.kall.narutotv.network.impl;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.world.WallTV;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.WallUpdatePacket;
import me.kall.narutotv.screen.NarutoWorldScreen;
import me.kall.narutotv.util.AudioZipGenerator;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Client {
    public static void removeWall(Wall wall) {
        ClientWalls.remove(wall).ifPresent(WallTV.DEATH);
    }

    public static void configWall(Wall wall) {
        Minecraft.getInstance().setScreen(new NarutoWorldScreen(Minecraft.getInstance().screen, wall));
    }

    public static void updateWall(Wall wall) {
        WallTV<?> current = ClientWalls.get(wall);
        if (current == null) return;
        Wall existing = current.wall;
        if (Objects.equals(existing.video, wall.video) && Objects.equals(existing.audio, wall.audio) && existing.localSound.equals(wall.localSound) && wall.volume != existing.volume) {
            current.setVolume(wall.volume);
            existing.volume = wall.volume;
        } else {
            ClientWalls.add(wall).ifPresent(outdated -> {
                WallTV<?> latest = ClientWalls.get(wall);
                assert latest != null;
                latest.mediaArgs = outdated.mediaArgs;
                outdated.shutdownEntire(false);
                latest.setup(0D);
            });
        }
    }

    public static void newWall(Wall wall) {
        ClientWalls.add(wall).ifPresent(tv -> {
            throw new IllegalStateException("Existing WallTV Found.");
        });

        WallTV<?> tv = ClientWalls.get(wall);
        assert tv != null;
        tv.setup(0D);

        MediaArgs mediaArgs = tv.mediaArgs;
        assert mediaArgs != null;

        wall.video = GamePaths.relConfig(mediaArgs.absVideoPath());
        wall.audio = GamePaths.relConfig(mediaArgs.absAudioPath());

        NarutoPackets.INSTANCE.sendToServer(new WallUpdatePacket(wall));
    }

    public static void syncWalls(@NotNull ObjectCollection<ObjectOpenHashSet<Wall>> walls) {
        walls.forEach(set -> set.forEach(wall -> {
            Runnable validation = () -> {
                ClientWalls.add(wall).ifPresent(WallTV.DEATH);

                WallTV<?> tv = ClientWalls.get(wall);
                assert tv != null;

                tv.mediaArgs = Sources.get(GamePaths.absConfig(wall.video));
                tv.setup(0D);
            };

            if (wall.hasLocalSound()) {
                FFmpeg.toMonoOgg(GamePaths.absConfig(wall.audio)).whenCompleteAsync((converted, convertFailure) -> {
                    if (convertFailure != null) {
                        convertFailure.printStackTrace(System.err);
                        throw new RuntimeException(convertFailure);
                    }
                    try {
                        AudioZipGenerator.get(converted).generate(id -> validation.run());
                    }  catch (Throwable throwable) {
                        NarutoPackets.LOGGER.error("Failed to generate audio zip", throwable);
                        throw new RuntimeException(throwable);
                    }
                });
            } else {
                validation.run();
            }
        }));
    }
}
