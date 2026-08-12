package me.kall.narutotv.data.file;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Spliterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static me.kall.narutotv.app.YtDlp.*;

public class Sources {
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    private static final Object2ObjectOpenHashMap<String, String> SOURCES = new Object2ObjectOpenHashMap<>();

    public static @NotNull MediaArgs random(boolean scan) {
        if (scan) Sources.scan();

        ObjectArrayList<Object2ObjectMap.Entry<String, String>> sources = new ObjectArrayList<>(SOURCES.object2ObjectEntrySet());
        Object2ObjectMap.Entry<String, String> target = sources.get(ThreadLocalRandom.current().nextInt(sources.size()));

        String video = target.getKey();
        String audio = target.getValue();

        return FFmpeg.read(video, audio);
    }

    public static @NotNull MediaArgs get(String video) {
        Sources.scan();

        String audio = SOURCES.get(video);

        if (audio == null) throw new IllegalStateException("Failed to find media source for " + video);

        return FFmpeg.read(video, audio);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public static void scan() {
        SOURCES.clear();

        try (Stream<Path> sourcesStream = Files.list(GamePaths.SOURCES)) {
            Spliterator<Path> sourcesDirectories = sourcesStream.filter(Files::isDirectory).spliterator();
            while (sourcesDirectories.tryAdvance(subDirectory -> {
                if (Files.exists(subDirectory.resolve(VIDEO_DOWNLOADING_FILE)) || Files.exists(subDirectory.resolve(AUDIO_DOWNLOADING_FILE))) return;
                Source source = new Source();
                try (Stream<Path> sourceStream = Files.list(subDirectory)) {
                    Spliterator<Path> sourceDirectory = sourceStream.filter(Files::isRegularFile).spliterator();
                    while (sourceDirectory.tryAdvance(file -> {
                        String name = file.getFileName().toString();
                        if (name.toLowerCase(Locale.ROOT).endsWith(".txt")) return;
                        if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) return;
                        if (name.startsWith(VIDEO_FILE_NAME)) source.video = file.toString();
                        if (name.startsWith(AUDIO_FILE_NAME)) source.audio = file.toString();
                    }));
                } catch (IOException exception) {
                    exception.printStackTrace(System.err);
                    SOURCES.clear();
                }

                if (source.video != null) {
                    if (source.audio == null) source.audio = source.video;
                    SOURCES.put(source.video, source.audio);
                }

            }));
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
            SOURCES.clear();
        }

        if (SOURCES.isEmpty()) throw new IllegalArgumentException("Sources Not Found.");
    }

    private static final class Source {
        @Nullable String video;
        @Nullable String audio;
    }
}
