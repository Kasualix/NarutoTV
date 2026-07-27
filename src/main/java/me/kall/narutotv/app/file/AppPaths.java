package me.kall.narutotv.app.file;

import me.kall.narutotv.base.data.NarutoPaths;

import java.nio.file.Path;

public class AppPaths {
    private static final String ABS_FFMPEG_PATH = "app.ffmpeg";
    private static final String ABS_FFPROBE_PATH = "app.ffprobe";
    private static final String ABS_YTDLP_PATH = "app.ytdlp";

    static {
        Path ffmpegBin = NarutoPaths.GAME.resolve("ffmpeg").resolve("bin");
        AppPaths.setAbsFFmpegPath(ffmpegBin.resolve("ffmpeg.exe").toAbsolutePath().toString());
        AppPaths.setAbsFFprobePath(ffmpegBin.resolve("ffprobe.exe").toAbsolutePath().toString());
        AppPaths.setAbsYtDlpPath(NarutoPaths.GAME.resolve("yt-dlp").resolve("yt-dlp.exe").toAbsolutePath().toString());
    }

    public static String absFFmpegPath() {
        return System.getProperty(ABS_FFMPEG_PATH);
    }

    public static String absFFprobePath() {
        return System.getProperty(ABS_FFPROBE_PATH);
    }

    public static String absYtDlpPath() {
        return System.getProperty(ABS_YTDLP_PATH);
    }

    public static void setAbsFFmpegPath(String absFFmpegPath) {
        System.setProperty(ABS_FFMPEG_PATH, absFFmpegPath);
    }

    public static void setAbsFFprobePath(String absFFprobePath) {
        System.setProperty(ABS_FFPROBE_PATH, absFFprobePath);
    }

    public static void setAbsYtDlpPath(String absYtDlpPath) {
        System.setProperty(ABS_YTDLP_PATH, absYtDlpPath);
    }
}
