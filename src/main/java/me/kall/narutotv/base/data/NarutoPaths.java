package me.kall.narutotv.base.data;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class NarutoPaths {
    public static final Path MODS, GAME, CONFIG, RESOURCEPACKS, SOURCES, COOKIES;

    static {
        GAME = Path.of(System.getProperty("user.dir"));
        CONFIG = GAME.resolve("config");
        SOURCES = CONFIG.resolve("narutotv-sources");
        COOKIES = CONFIG.resolve("narutotv-cookies");
        MODS = GAME.resolve("mods");
        RESOURCEPACKS = GAME.resolve("resourcepacks");
    }

    public static @NotNull String absolute(@NotNull String relativePath) {
        if (relativePath.isBlank()) return "";
        return CONFIG.resolve(relativePath.replace('\\', '/')).normalize().toString();
    }

    public static @NotNull String relative(@NotNull String absolutePath) {
        if (absolutePath.isBlank()) return "";

        Path target = Path.of(absolutePath).normalize();
        if (!target.startsWith(CONFIG)) return "";

        return CONFIG.relativize(target).toString().replace('\\', '/');
    }
}