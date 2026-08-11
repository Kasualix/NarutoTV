package me.kall.narutotv.app.data;

import me.kall.narutotv.app.FFmpeg;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record Downloaded(Path absVideoPath, Path absAudioPath) {
    @Contract(" -> new")
    public @NotNull MediaArgs toMediaArgs() {
        return FFmpeg.read(this.absVideoPath.toString(), this.absAudioPath.toString());
    }
}
