package me.kall.narutotv.impl.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class NarutoConfig {
    private static final ForgeConfigSpec CONFIG;

    private static final ForgeConfigSpec.BooleanValue MUTE_MUSIC, FADABLE;
    private static final ForgeConfigSpec.IntValue TICKS_BEFORE_FADE;

    private static final Logger LOGGER = LogManager.getLogger(NarutoConfig.class);

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("NarutoTV");
        MUTE_MUSIC = builder.comment("Whether to mute the vanilla game music (SoundSource.MUSIC) while NarutoTV video playback is active.").define("muteMusic", true);
        FADABLE = builder.comment("Whether to gradually fade all the gui elements except the playing video").define("fadable", true);
        TICKS_BEFORE_FADE = builder.comment("Tick count before all the gui elements get faded. 20 ticks = 1 second").defineInRange("ticksBeforeFade", 200, 0, Integer.MAX_VALUE);
        builder.pop();
        CONFIG = builder.build();
    }

    public static void register(@NotNull ModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, CONFIG);
    }

    public static boolean fadable() {
        return FADABLE.get();
    }

    public static int ticksBeforeFade() {
        return TICKS_BEFORE_FADE.get();
    }

    public static boolean musicMuted() {
        return MUTE_MUSIC.get();
    }

    public static void toggleFadable() {
        FADABLE.set(!FADABLE.get());
        LOGGER.info("[NarutoTV] fadable config option is set to {}", FADABLE.get().toString());
        CONFIG.save();
    }

    public static void toggleMuteMusic() {
        MUTE_MUSIC.set(!MUTE_MUSIC.get());
        LOGGER.info("[NarutoTV] muteMusic config option is set to {}", MUTE_MUSIC.get().toString());
        CONFIG.save();
    }
}
