package me.kall.narutotv.base.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static me.kall.narutotv.app.YtDlp.*;

public class Sources {
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    private static final ObjectList<MediaArgs> SOURCES = ObjectLists.synchronize(new ObjectArrayList<>());
    private static final ObjectList<MediaArgs> PLAYED = ObjectLists.synchronize(new ObjectArrayList<>());

    private static final Random RANDOM = ThreadLocalRandom.current();

    public static MediaArgs roll() {
        Sources.scan();
        if (SOURCES.isEmpty()) throw new IllegalArgumentException("No sources found");
        MediaArgs one = SOURCES.get(RANDOM.nextInt(SOURCES.size()));
        if (SOURCES.size() > 1) {
            while (PLAYED.contains(one)) {
                one = SOURCES.get(RANDOM.nextInt(SOURCES.size()));
            }
        }
        PLAYED.add(one);
        return one;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static void scan() {
        SOURCES.clear();

        try (Stream<Path> sourcesStream = Files.list(Paths.SOURCES)) {
            Spliterator<Path> sourcesDirectories = sourcesStream.filter(Files::isDirectory).spliterator();
            while (sourcesDirectories.tryAdvance(subDirectory -> {
                if (Files.exists(subDirectory.resolve(VIDEO_DOWNLOADING_FILE)) || Files.exists(subDirectory.resolve(AUDIO_DOWNLOADING_FILE))) return;
                Source source = new Source();
                try (Stream<Path> sourceStream = Files.list(subDirectory)) {
                    Spliterator<Path> sourceDirectory = sourceStream.filter(Files::isRegularFile).spliterator();
                    while (sourceDirectory.tryAdvance(file -> {
                        String name = file.getFileName().toString();
                        if (name.toLowerCase().endsWith(".txt")) return;
                        if (name.startsWith(VIDEO_FILE_NAME)) source.video = file.toString();
                        if (name.endsWith(AUDIO_FILE_NAME)) source.audio = file.toString();
                    }));
                } catch (IOException exception) {
                    exception.printStackTrace(System.err);
                }

                if (source.video != null) {
                    if (source.audio == null) source.audio = source.video;
                    SOURCES.add(source.toMediaArgs());
                }

            }));
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }

        if (SOURCES.size() <= PLAYED.size()) PLAYED.clear();
    }

    static final class Source {
        @Nullable String video, audio;

        @NotNull
        MediaArgs toMediaArgs() {
            assert this.video != null;
            assert this.audio != null;
            return AppInstances.ffmpeg().read(this.video, this.audio);
        }
    }
}