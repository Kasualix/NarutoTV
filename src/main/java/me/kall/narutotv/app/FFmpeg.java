package me.kall.narutotv.app;

import com.google.common.io.Files;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.data.system.AppProps;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL11;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class FFmpeg {
    private static final Pattern VIDEO_STREAM = Pattern.compile("\\{[^}]*\"codec_type\"\\s*:\\s*\"video\"[^}]*}");
    private static final Pattern R_FPS = Pattern.compile("\"r_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
    private static final Pattern AVG_FPS = Pattern.compile("\"avg_frame_rate\"\\s*:\\s*\"(\\d+)/(\\d+)\"");
    private static final Pattern DURATION = Pattern.compile("\"format\"\\s*:\\s*\\{[^}]*\"duration\"\\s*:\\s*\"([0-9.]+)\"");
    private static final Pattern WIDTH = Pattern.compile("\"width\"\\s*:\\s*(\\d+)");
    private static final Pattern HEIGHT = Pattern.compile("\"height\"\\s*:\\s*(\\d+)");
    private static final Pattern AUDIO_STREAM = Pattern.compile("\\{[^}]*\"codec_type\"\\s*:\\s*\"audio\"[^}]*}");
    private static final Pattern SAMPLE_RATE = Pattern.compile("\"sample_rate\"\\s*:\\s*\"(\\d+)\"");
    private static final Pattern CHANNEL_COUNT = Pattern.compile("\"channels\"\\s*:\\s*(\\d+)");
    private static final Pattern START_TIME = Pattern.compile("\"start_time\"\\s*:\\s*\"([0-9.]+)\"");

    @Contract("_, _ -> new")
    public static @NotNull MediaArgs read(@NotNull String absVideoPath, @Nullable String absAudioPath) {
        String ffprobePath = AppProps.ffprobePath();

        String videoJson = Executable.runCommand(ffprobePath, "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format", absVideoPath);
        if (videoJson == null) throw new RuntimeException("Error generating probe json for " + absVideoPath + ". Using " + ffprobePath + ". Reading log for details.");

        String audioJson = absAudioPath != null ? Executable.runCommand(ffprobePath, "-v", "quiet", "-print_format", "json", "-show_streams", "-show_format", absAudioPath) : null;

        try {
            Matcher videoMatcher = VIDEO_STREAM.matcher(videoJson);
            if (!videoMatcher.find()) throw new RuntimeException("Error matching video");

            String videoBlock = videoMatcher.group();

            Matcher avgFpsMatcher = AVG_FPS.matcher(videoBlock);
            Matcher rFpsMatcher = R_FPS.matcher(videoBlock);
            Matcher widthMatcher = WIDTH.matcher(videoBlock);
            Matcher heightMatcher = HEIGHT.matcher(videoBlock);
            Matcher videoStartMatcher = START_TIME.matcher(videoBlock);
            Matcher durationMatcher = DURATION.matcher(videoJson);

            avgFpsMatcher.find(); rFpsMatcher.find(); widthMatcher.find(); heightMatcher.find(); videoStartMatcher.find(); durationMatcher.find();

            double avgFps = Double.parseDouble(avgFpsMatcher.group(1)) / Double.parseDouble(avgFpsMatcher.group(2));
            double rFps = Double.parseDouble(rFpsMatcher.group(1)) / Double.parseDouble(rFpsMatcher.group(2));

            double fps = avgFps > 0 ? avgFps : rFps;

            int width = Integer.parseInt(widthMatcher.group(1));
            int height = Integer.parseInt(heightMatcher.group(1));

            double scale = Math.min(1.0, Math.min((double) NarutoConfig.maxWidth() / width, (double) NarutoConfig.maxHeight() / height));

            width = ((int) (width * scale)) & ~1;
            height = ((int) (height * scale)) & ~1;

            double videoStartSec = Double.parseDouble(videoStartMatcher.group(1));

            double duration = Double.parseDouble(durationMatcher.group(1)) * 1000D;

            if (audioJson != null) {
                Matcher audioMatcher = AUDIO_STREAM.matcher(audioJson);
                if (!audioMatcher.find()) throw new RuntimeException("Error matching audio");

                String audioBlock = audioMatcher.group();

                Matcher channelCountMatcher = CHANNEL_COUNT.matcher(audioBlock);
                Matcher sampleRateMatcher = SAMPLE_RATE.matcher(audioBlock);
                Matcher audioStartMatcher = START_TIME.matcher(audioBlock);

                channelCountMatcher.find(); sampleRateMatcher.find(); audioStartMatcher.find();

                int channelCount = Integer.parseInt(channelCountMatcher.group(1));
                int sampleRate = Integer.parseInt(sampleRateMatcher.group(1));
                double audioStartSec = Double.parseDouble(audioStartMatcher.group(1));

                int openALFormat = channelCount == 1 ? AL11.AL_FORMAT_MONO16 : AL11.AL_FORMAT_STEREO16;
                return new MediaArgs(absVideoPath, absAudioPath, channelCount, sampleRate, openALFormat, fps, width, height, duration, videoStartSec, audioStartSec);
            } else {
                return new MediaArgs(absVideoPath, fps, width, height, duration, videoStartSec);
            }
        } catch (Throwable throwable) {
            System.err.println("————————————————————————————");
            System.err.println("Exception reading video " + absVideoPath + " using " + ffprobePath);
            System.err.println(videoJson);
            System.err.println("————————————————————————————");
            System.err.println("Exception reading audio " + absAudioPath + " using " + ffprobePath);
            System.err.println(audioJson);
            System.err.println("————————————————————————————");
            throwable.printStackTrace(System.err);
            throw new RuntimeException(throwable);
        }
    }

    @Contract("_ -> new")
    public static @NotNull CompletableFuture<String> toMonoOgg(String sourceStr) {
        return CompletableFuture.supplyAsync(() -> {
            if ("ogg".equals(Files.getFileExtension(sourceStr)) && isMono(sourceStr)) return sourceStr;

            File source = new File(sourceStr);
            String name = source.getName();

            File output = source.toPath().getParent().resolve(name.substring(0, name.lastIndexOf(".")) + ".ogg").toFile();
            String outputStr = output.getAbsolutePath();

            if (output.exists()) {
                if (isMono(outputStr)) {
                    return outputStr;
                } else {
                    output.delete();
                }
            }

            return Executable.runCommand(AppProps.ffmpegPath(), "-i", sourceStr, "-vn", "-acodec", "libvorbis", "-ac", "1", "-q:a", "4", "-y", outputStr) != null ? outputStr : null;
        }, NarutoTV.io());
    }

    private static boolean isMono(String absAudioPath) {
        return "1".equals(Executable.runCommand(AppProps.ffprobePath(), "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=channels", "-of", "default=noprint_wrappers=1:nokey=1", absAudioPath));
    }
}
