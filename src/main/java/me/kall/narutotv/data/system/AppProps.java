package me.kall.narutotv.data.system;

import me.kall.narutotv.data.file.GamePaths;

import java.nio.file.Path;

public class AppProps {
    private static final String FFMPEG_PROPERTY = "app.ffmpeg";
    private static final String FFPROBE_PROPERTY = "app.ffprobe";
    private static final String YTDLP_PROPERTY = "app.ytdlp";

    static {
        Path ffmpegBin = GamePaths.GAME.resolve("ffmpeg").resolve("bin");
        AppProps.setFFmpegPath(ffmpegBin.resolve("ffmpeg.exe").toAbsolutePath().toString());
        AppProps.setFFprobePath(ffmpegBin.resolve("ffprobe.exe").toAbsolutePath().toString());
        AppProps.setYtDlpPath(GamePaths.GAME.resolve("yt-dlp").resolve("yt-dlp.exe").toAbsolutePath().toString());
    }

    public static String ffmpegPath() {
        return System.getProperty(FFMPEG_PROPERTY);
    }

    public static String ffprobePath() {
        return System.getProperty(FFPROBE_PROPERTY);
    }

    public static String ytDlpPath() {
        return System.getProperty(YTDLP_PROPERTY);
    }

    public static void setFFmpegPath(String ffmpegPath) {
        System.setProperty(FFMPEG_PROPERTY, ffmpegPath);
    }

    public static void setFFprobePath(String ffprobePath) {
        System.setProperty(FFPROBE_PROPERTY, ffprobePath);
    }

    public static void setYtDlpPath(String ytDlpPath) {
        System.setProperty(YTDLP_PROPERTY, ytDlpPath);
    }
}
