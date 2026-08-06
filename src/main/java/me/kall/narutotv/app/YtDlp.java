package me.kall.narutotv.app;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.Downloaded;
import me.kall.narutotv.data.system.AppProps;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class YtDlp {
    public static final String VIDEO_DOWNLOADING_FILE = "video-downloading.txt";
    public static final String AUDIO_DOWNLOADING_FILE = "audio-downloading.txt";

    private static final String VIDEO_DOWNLOADED_FILE = "video-downloaded.txt";
    private static final String AUDIO_DOWNLOADED_FILE = "audio-downloaded.txt";

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

            String outputTemplate = outputDir.resolve("audio.%(ext)s").toString();

            boolean audioSuccess = Executable.runCommand(AppProps.ytDlpPath(), url, "-o", outputTemplate, "-x", "--audio-format", "mp3", "--audio-quality", "0", "--no-playlist", "--progress", "--ffmpeg-location", AppProps.ffmpegPath()) != null;
            boolean videoSuccess = Executable.runCommand(AppProps.ytDlpPath(), url, "-o", outputTemplate, "--format", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best", "--merge-output-format", "mp4", "--no-playlist", "--progress", "--ffmpeg-location", AppProps.ffmpegPath()) != null;

            if (!audioSuccess || !videoSuccess) return null;

            try {
                Files.move(videoDownloading, outputDir.resolve(VIDEO_DOWNLOADED_FILE));
                Files.move(audioDownloading, outputDir.resolve(AUDIO_DOWNLOADED_FILE));
            } catch (Throwable throwable) {
                System.err.println("Exception validating downloaded source url. Output directory: " + outputDir);
                throwable.printStackTrace(System.err);
                return null;
            }

            return new Downloaded(outputDir.resolve("video.mp4"), outputDir.resolve("audio.mp3"));
        }, NarutoTV.io());
    }
}
