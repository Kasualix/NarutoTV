package me.kall.narutotv.config;

import com.google.common.collect.Lists;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.util.JsonConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class NarutoConfig {
    private static final JsonConfig CONFIG = JsonConfig.create(NarutoTV.MOD_ID, "1")
            .put("displayers", Lists.newArrayList("minecraft:glass"))
            .put("screenBuilder", "minecraft:stick")
            .put("muteMusic", true)
            .put("fadable", true)
            .put("ticksBeforeFade", 100)
            .put("audioVolume", 1.0F)
            .put("teleControlItem", "minecraft:stick")
            .put("maxVideoWidth", 1920)
            .put("maxVideoHeight", 1080)
            .initialize();

    private static final Set<String> displayers = CONFIG.getSet("displayers", String.class);
    private static final String builder = CONFIG.getString("screenBuilder");

    private static boolean muteMusic = CONFIG.getBoolean("muteMusic");
    private static boolean fadable = CONFIG.getBoolean("fadable");
    private static int ticksBeforeFade = CONFIG.getInt("ticksBeforeFade");
    private static float volume = CONFIG.getFloat("audioVolume");
    private static int maxWidth = CONFIG.getInt("maxVideoWidth");
    private static int maxHeight = CONFIG.getInt("maxVideoHeight");

    private static final String teleControl = CONFIG.getString("teleControlItem");

    public static Set<String> displayers() {
        return displayers;
    }

    @Contract(" -> new")
    public static @NotNull String builder() {
        return builder;
    }

    public static boolean fadable() {
        return fadable;
    }

    public static int ticksBeforeFade() {
        return ticksBeforeFade;
    }

    public static boolean muteMusic() {
        return muteMusic;
    }

    public static float volume() {
        return volume;
    }

    @Contract(" -> new")
    public static @NotNull String teleControl() {
        return teleControl;
    }

    public static int maxWidth() {
        return maxWidth;
    }

    public static int maxHeight() {
        return maxHeight;
    }

    public static void fadable(boolean fadable) {
        if (NarutoConfig.fadable == fadable) return;
        NarutoConfig.fadable = fadable;
        CONFIG.put("fadable", fadable).saveToFile();
    }

    public static void ticksBeforeFade(int ticksBeforeFade) {
        if (NarutoConfig.ticksBeforeFade == ticksBeforeFade) return;
        NarutoConfig.ticksBeforeFade = ticksBeforeFade;
        CONFIG.put("ticksBeforeFade", ticksBeforeFade).saveToFile();
    }

    public static void muteMusic(boolean muteMusic) {
        if (NarutoConfig.muteMusic == muteMusic) return;
        NarutoConfig.muteMusic = muteMusic;
        CONFIG.put("muteMusic", muteMusic).saveToFile();
    }

    public static boolean volume(double volume) {
        if (NarutoConfig.volume == volume) return false;

        NarutoConfig.volume = (float) volume;
        CONFIG.put("audioVolume", volume).saveToFile();

        return true;
    }

    public static boolean maxWidth(int maxWidth) {
        if (NarutoConfig.maxWidth == maxWidth) return false;

        NarutoConfig.maxWidth = maxWidth;
        CONFIG.put("maxVideoWidth", maxWidth).saveToFile();
        return true;
    }

    public static boolean maxHeight(int maxHeight) {
        if (NarutoConfig.maxHeight == maxHeight) return false;

        NarutoConfig.maxHeight = maxHeight;
        CONFIG.put("maxVideoHeight", maxHeight).saveToFile();
        return true;
    }
}
