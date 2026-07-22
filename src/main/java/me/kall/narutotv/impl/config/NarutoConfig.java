package me.kall.narutotv.impl.config;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class NarutoConfig {
    private static final Logger LOGGER = LogManager.getLogger(NarutoConfig.class);

    public static final class Server {
        private static final ForgeConfigSpec CONFIG;
        private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISPLAYERS;
        private static final ForgeConfigSpec.ConfigValue<? extends String> SCREEN_BUILDER;

        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("NarutoTV");
            DISPLAYERS = builder.defineList("Displayers", Lists.newArrayList("minecraft:glass"), Predicates.alwaysTrue());
            SCREEN_BUILDER = builder.define("ScreenBuilder", "minecraft:stick");
            builder.pop();
            CONFIG = builder.build();
        }

        private static final Supplier<Set<ResourceLocation>> DISPLAYERS_CACHE = Suppliers.memoize(() -> DISPLAYERS.get().stream().map(ResourceLocation::parse).collect(ObjectOpenHashSet::new, ObjectOpenHashSet::add, ObjectOpenHashSet::addAll));

        public static void register(@NotNull ModLoadingContext context) {
            context.registerConfig(ModConfig.Type.SERVER, CONFIG);
        }

        public static Set<ResourceLocation> displayers() {
            return DISPLAYERS_CACHE.get();
        }

        @Contract(" -> new")
        public static @NotNull ResourceLocation builder() {
            return ResourceLocation.parse(SCREEN_BUILDER.get());
        }
    }

    public static final class Client {
        private static final ForgeConfigSpec CONFIG;
        private static final ForgeConfigSpec.BooleanValue MUTE_MUSIC;
        private static final ForgeConfigSpec.BooleanValue FADABLE;
        private static final ForgeConfigSpec.IntValue TICKS_BEFORE_FADE;

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
}
