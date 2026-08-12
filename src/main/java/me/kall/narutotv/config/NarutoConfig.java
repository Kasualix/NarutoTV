package me.kall.narutotv.config;

import com.google.common.collect.Lists;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.util.JsonConfig;
import org.jetbrains.annotations.Contract;

import java.util.Set;

public class NarutoConfig {
    private static final JsonConfig CONFIG = JsonConfig.create(NarutoTV.MOD_ID, "1")
            .put("enableGuiScreen", true)
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

    private static boolean enableGuiScreen = CONFIG.getBoolean("enableGuiScreen");
    private static boolean muteMusic = CONFIG.getBoolean("muteMusic");
    private static boolean fadable = CONFIG.getBoolean("fadable");
    private static int ticksBeforeFade = CONFIG.getInt("ticksBeforeFade");
    private static float volume = CONFIG.getFloat("audioVolume");
    private static int maxWidth = CONFIG.getInt("maxVideoWidth");
    private static int maxHeight = CONFIG.getInt("maxVideoHeight");

    private static final String teleControl = CONFIG.getString("teleControlItem");


    @Contract(pure = true)
    public static boolean enableGuiScreen() {
        return enableGuiScreen;
    }

    @Contract(pure = true)
    public static Set<String> displayers() {
        return displayers;
    }

    @Contract(pure = true)
    public static String builder() {
        return builder;
    }

    @Contract(pure = true)
    public static boolean fadable() {
        return fadable;
    }

    @Contract(pure = true)
    public static int ticksBeforeFade() {
        return ticksBeforeFade;
    }

    @Contract(pure = true)
    public static boolean muteMusic() {
        return muteMusic;
    }

    @Contract(pure = true)
    public static float volume() {
        return volume;
    }

    @Contract(pure = true)
    public static String teleControl() {
        return teleControl;
    }

    @Contract(pure = true)
    public static int maxWidth() {
        return maxWidth;
    }

    @Contract(pure = true)
    public static int maxHeight() {
        return maxHeight;
    }

    public static void enableGuiScreen(boolean enableGuiScreen) {
        if (NarutoConfig.enableGuiScreen == enableGuiScreen) return;
        NarutoConfig.enableGuiScreen = enableGuiScreen;
        CONFIG.put("enableGuiScreen", enableGuiScreen).saveToFile();
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
