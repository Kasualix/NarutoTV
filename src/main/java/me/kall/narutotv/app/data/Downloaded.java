package me.kall.narutotv.app.data;

import java.nio.file.Path;

public record Downloaded(Path absVideoPath, Path absAudioPath) {
}
