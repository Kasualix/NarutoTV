package me.kall.narutotv.data.file;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class GamePaths {
    public static final Path MODS, GAME, CONFIG, RESOURCEPACKS, SOURCES, COOKIES, CAPES;

    static {
        GAME = Path.of(System.getProperty("user.dir"));
        CONFIG = GAME.resolve("config");
        SOURCES = CONFIG.resolve("narutotv-sources");
        COOKIES = CONFIG.resolve("narutotv-cookies");
        MODS = GAME.resolve("mods");
        RESOURCEPACKS = GAME.resolve("resourcepacks");
        CAPES = CONFIG.resolve("narutotv-capes");
    }

    public static @NotNull String absConfig(@NotNull String relativePath) {
        if (relativePath.isBlank()) throw new IllegalArgumentException("Path string is blank.");
        return CONFIG.resolve(relativePath.replace('\\', '/')).normalize().toString();
    }

    public static @NotNull String relConfig(@NotNull String absolutePath) {
        if (absolutePath.isBlank()) throw new IllegalArgumentException("Path string is blank.");

        Path target = Path.of(absolutePath).normalize();
        if (!target.startsWith(CONFIG)) throw new IllegalArgumentException("Already relative: " + absolutePath);

        return CONFIG.relativize(target).toString().replace('\\', '/');
    }
}
