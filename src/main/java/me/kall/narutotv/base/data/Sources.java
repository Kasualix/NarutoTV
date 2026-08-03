package me.kall.narutotv.base.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static me.kall.narutotv.app.YtDlp.AUDIO_DOWNLOADING_FILE;
import static me.kall.narutotv.app.YtDlp.VIDEO_DOWNLOADING_FILE;

public class Sources {
    private static final String VIDEO_FILE_NAME = "video";
    private static final String AUDIO_FILE_NAME = "audio";

    private static final ObjectList<Source> SOURCES = new ObjectArrayList<>();
    private static final ObjectList<Source> PLAYED = new ObjectArrayList<>();

    private static final Random RANDOM = new Random();

    private static final AtomicReference<Source> LINE_CUTTER = new AtomicReference<>();

    public static synchronized void cutInLine(@NotNull Path video, @Nullable Path audio) {
        Source source = new Source();
        source.video = video.toString();
        source.audio = audio != null ? audio.toString() : source.video;
        LINE_CUTTER.set(source);
    }

    public static synchronized void cutInLine(String video, String audio) {
        Source source = new Source();
        source.video = video;
        source.audio = audio != null && !audio.isBlank() ? audio : video;
        LINE_CUTTER.set(source);
    }

    public static synchronized void noLineCut() {
        LINE_CUTTER.set(null);
    }

    public static synchronized @NotNull MediaArgs get() {
        Source lineCutter = LINE_CUTTER.getAndSet(null);
        if (lineCutter != null) {
            PLAYED.add(lineCutter);
            return lineCutter.toMediaArgs();
        }

        Sources.scan();
        if (SOURCES.isEmpty()) throw new IllegalArgumentException("No sources found");
        Source target = SOURCES.get(RANDOM.nextInt(SOURCES.size()));
        if (SOURCES.size() > 1) {
            while (PLAYED.contains(target)) {
                target = SOURCES.get(RANDOM.nextInt(SOURCES.size()));
            }
        }
        PLAYED.add(target);
        return target.toMediaArgs();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static void scan() {
        SOURCES.clear();

        try (Stream<Path> sourcesStream = Files.list(NarutoPaths.SOURCES)) {
            Spliterator<Path> sourcesDirectories = sourcesStream.filter(Files::isDirectory).spliterator();
            while (sourcesDirectories.tryAdvance(subDirectory -> {
                if (Files.exists(subDirectory.resolve(VIDEO_DOWNLOADING_FILE)) || Files.exists(subDirectory.resolve(AUDIO_DOWNLOADING_FILE))) return;
                Source source = new Source();
                try (Stream<Path> sourceStream = Files.list(subDirectory)) {
                    Spliterator<Path> sourceDirectory = sourceStream.filter(Files::isRegularFile).spliterator();
                    while (sourceDirectory.tryAdvance(file -> {
                        String name = file.getFileName().toString();
                        if (name.toLowerCase().endsWith(".txt")) return;
                        if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) return;
                        if (name.startsWith(VIDEO_FILE_NAME)) source.video = file.toString();
                        if (name.startsWith(AUDIO_FILE_NAME)) source.audio = file.toString();
                    }));
                } catch (IOException exception) {
                    exception.printStackTrace(System.err);
                }

                if (source.video != null) {
                    if (source.audio == null) source.audio = source.video;
                    SOURCES.add(source);
                }

            }));
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }

        if (SOURCES.size() <= PLAYED.size()) PLAYED.clear();
    }

    static final class Source {
        String video, audio;

        @NotNull
        MediaArgs toMediaArgs() {
            return AppInstances.ffmpeg().read(this.video, this.audio);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Source other)) return false;
            return Objects.equals(this.video, other.video) && Objects.equals(this.audio, other.audio);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.video, this.audio);
        }
    }
}