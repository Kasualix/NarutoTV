package me.kall.narutotv.app.file;

import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.YtDlp;

import java.util.function.Supplier;

public class AppInstances {
    private static final FFmpeg FFMPEG = FFmpeg.create(AppPaths.absFFmpegPath(), AppPaths.absFFprobePath());
    private static final YtDlp YT_DLP = YtDlp.create(AppPaths.absFFmpegPath(), AppPaths.absYtDlpPath());

    public static FFmpeg ffmpeg() {
        return FFMPEG;
    }

    public static YtDlp ytDlp() {
        return YT_DLP;
    }
}
