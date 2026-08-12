package me.kall.narutotv.app.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public record MediaArgs(String absVideoPath, String absAudioPath, int channelCount, int sampleRate, int openALFormat, double fps, int width, int height, double duration, double videoStartSec, double audioStartSec) {
    public MediaArgs(String absVideoPath, double fps, int width, int height, double duration, double videoStartSec) {
        this(absVideoPath, "", 0, 0, 0, fps, width, height, duration, videoStartSec, 0D);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaArgs that)) return false;
        return this.channelCount == that.channelCount && this.sampleRate == that.sampleRate && this.openALFormat == that.openALFormat && Double.compare(that.fps, this.fps) == 0 && this.width == that.width && this.height == that.height && Double.compare(that.duration, this.duration) == 0 && Objects.equals(this.absVideoPath, that.absVideoPath) && Objects.equals(this.absAudioPath, that.absAudioPath) && Double.compare(this.videoStartSec, that.videoStartSec) == 0 && Double.compare(this.audioStartSec, that.audioStartSec) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.absVideoPath, this.absAudioPath, this.channelCount, this.sampleRate, this.openALFormat, this.fps, this.width, this.height, this.duration);
    }

    @Override
    public @NotNull String toString() {
        return
                "absVideoPath:" + Base64.getEncoder().encodeToString(this.absVideoPath.getBytes(StandardCharsets.UTF_8)) +
                        ",absAudioPath:" + Base64.getEncoder().encodeToString(this.absAudioPath.getBytes(StandardCharsets.UTF_8)) +
                        ",channelCount:" + this.channelCount +
                        ",sampleRate:" + this.sampleRate +
                        ",openALFormat:" + this.openALFormat +
                        ",fps:" + this.fps +
                        ",width:" + this.width +
                        ",height:" + this.height +
                        ",duration:" + this.duration +
                        ",videoStartSec:" + this.videoStartSec +
                        ",audioStartSec:" + this.audioStartSec;
    }

    public @NotNull String toReadableString() {
        return
                "absVideoPath:" + this.absVideoPath() +
                        ",absAudioPath:" + this.absAudioPath() +
                        ",channelCount:" + this.channelCount() +
                        ",sampleRate:" + this.sampleRate() +
                        ",openALFormat:" + this.openALFormat() +
                        ",fps:" + this.fps() +
                        ",width:" + this.width() +
                        ",height:" + this.height() +
                        ",duration:" + this.duration() +
                        ",videoStartSec:" + this.videoStartSec() +
                        ",audioStartSec:" + this.audioStartSec();
    }

    @Contract("_ -> new")
    public static @NotNull MediaArgs fromString(@NotNull String mediaArgsString) {
        String[] args = mediaArgsString.split(",");
        String absVideoPath = new String(Base64.getDecoder().decode(args[0].split(":")[1]), StandardCharsets.UTF_8);
        String absAudioPath = new String(Base64.getDecoder().decode(args[1].split(":")[1]), StandardCharsets.UTF_8);
        int channelCount = Integer.parseInt(args[2].split(":")[1]);
        int sampleRate = Integer.parseInt(args[3].split(":")[1]);
        int openALFormat = Integer.parseInt(args[4].split(":")[1]);
        double fps = Double.parseDouble(args[5].split(":")[1]);
        int width = Integer.parseInt(args[6].split(":")[1]);
        int height = Integer.parseInt(args[7].split(":")[1]);
        double duration = Double.parseDouble(args[8].split(":")[1]);
        double videoStartSec = Double.parseDouble(args[9].split(":")[1]);
        double audioStartSec = Double.parseDouble(args[10].split(":")[1]);
        return new MediaArgs(absVideoPath, absAudioPath, channelCount, sampleRate, openALFormat, fps, width, height, duration, videoStartSec, audioStartSec);
    }
}