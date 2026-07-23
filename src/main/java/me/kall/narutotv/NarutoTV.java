package me.kall.narutotv;

import me.kall.narutotv.compat.ICompat;
import me.kall.narutotv.compat.OculusCompat;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.qol.ShaderDetection;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.override.OverrideApi;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(NarutoTV.MOD_ID)
public final class NarutoTV {
    public static final String MOD_ID = "narutotv";

    private static final ExecutorService IO_WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoIOWorker");
        thread.setDaemon(true);
        return thread;
    });

    private static final boolean HAS_SHADER_MOD = isLoaded("oculus") || isLoaded("iris");
    private static final ICompat COMPAT = HAS_SHADER_MOD ? new OculusCompat() : () -> false;

    public NarutoTV(@NotNull FMLJavaModLoadingContext context) {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        if (FMLLoader.getDist().isClient()) {
            if (HAS_SHADER_MOD) ShaderDetection.register(forgeBus);
            NarutoConfig.Client.register(context);

            OverrideApi.setTask(() -> NarutoGuiCenter.getActive().isRunnable(), () -> NarutoGuiCenter.getActive().render());
        }

        NarutoConfig.Server.register(context);
        NarutoPackets.register();
    }

    public static boolean shaderUsing() {
        return COMPAT.shaderUsing();
    }

    public static boolean isLoaded(String modID) {
        return FMLLoader.getLoadingModList().getModFileById(modID) != null;
    }

    public static ExecutorService io() {
        return IO_WORKER;
    }
}
