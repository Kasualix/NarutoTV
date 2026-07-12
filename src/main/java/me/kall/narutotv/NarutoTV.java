package me.kall.narutotv;

import me.kall.narutotv.fade.FadeCenter;
import me.kall.narutotv.override.CustomOverride;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

@Mod(NarutoTV.MOD_ID)
public final class NarutoTV {
    public static final String MOD_ID = "narutotv";

    public NarutoTV(@NotNull FMLJavaModLoadingContext context) {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        IEventBus modBus = context.getModEventBus();

        if (FMLLoader.getDist().isClient()) {
            FadeCenter.register(forgeBus);
            CustomOverride.register(forgeBus);
        }
    }
}
