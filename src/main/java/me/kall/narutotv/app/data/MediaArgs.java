package me.kall.narutotv.app.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record MediaArgs(String absVideoPath, String absAudioPath, int channelCount, int sampleRate, int openALFormat, double fps, int width, int height, double duration) {
    @Override
    public int width() {
        int width = this.width;
        int widthCap = 7680;
        if (width > widthCap) width = widthCap;
        return width;
    }

    @Override
    public int height() {
        int height = this.height;
        int heightCap = 4320;
        if (height > heightCap) height = heightCap;
        return height;
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
                ",duration:" + this.duration;
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
        return new MediaArgs(absVideoPath, absAudioPath, channelCount, sampleRate, openALFormat, fps, width, height, duration);
    }
}