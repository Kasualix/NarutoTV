package me.kall.narutotv.app;

import me.kall.narutotv.app.data.Downloaded;
import me.kall.narutotv.data.system.AppProps;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YtDlp {
    public static final String VIDEO_DOWNLOADING_FILE = "video-downloading.txt";
    public static final String AUDIO_DOWNLOADING_FILE = "audio-downloading.txt";

    private static final ExecutorService DOWNLOADER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoDownloadThread");
        thread.setDaemon(true);
        return thread;
    });

    @Contract("_, _ -> new")
    public static @NotNull CompletableFuture<Downloaded> download(String url, Path outputDir) {
        return CompletableFuture.supplyAsync(() -> {
            Path videoDownloading = outputDir.resolve(VIDEO_DOWNLOADING_FILE);
            Path audioDownloading = outputDir.resolve(AUDIO_DOWNLOADING_FILE);

            try {
                Files.createDirectories(outputDir);
                Files.writeString(videoDownloading, url);
                Files.writeString(audioDownloading, url);
            } catch (Throwable throwable) {
                System.err.println("Exception storing download source. Url: " + url + ". Output directory: " + outputDir);
                throwable.printStackTrace(System.err);
                return null;
            }

            Executable.PRINT.set(true);

            boolean audioSuccess = Executable.runCommand(AppProps.ytDlpPath(), url, "-o", outputDir.resolve("audio.%(ext)s").toString(), "-x", "--audio-format", "mp3", "--audio-quality", "0", "--no-playlist", "--progress", "--ffmpeg-location", AppProps.ffmpegPath()) != null;
            boolean videoSuccess = Executable.runCommand(AppProps.ytDlpPath(), url, "-o", outputDir.resolve("video.%(ext)s").toString(), "--format", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", "--merge-output-format", "mp4", "--no-playlist", "--progress", "--ffmpeg-location", AppProps.ffmpegPath()) != null;

            Executable.PRINT.set(false);

            if (!audioSuccess || !videoSuccess) return null;

            try {
                Files.move(videoDownloading, outputDir.resolve("video-downloaded.txt"));
                Files.move(audioDownloading, outputDir.resolve("audio-downloaded.txt"));
            } catch (Throwable throwable) {
                System.err.println("Exception validating downloaded source url. Output directory: " + outputDir);
                throwable.printStackTrace(System.err);
                return null;
            }

            return new Downloaded(outputDir.resolve("video.mp4"), outputDir.resolve("audio.mp3"));
        }, DOWNLOADER);
    }
}
