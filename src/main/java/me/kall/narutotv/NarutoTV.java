package me.kall.narutotv;

import me.kall.narutotv.compat.ICompat;
import me.kall.narutotv.compat.OculusCompat;
import me.kall.narutotv.fade.FadeCenter;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.qol.KeybindCenter;
import me.kall.narutotv.impl.qol.SourceDragCenter;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.event.ScreenConstruction;
import me.kall.narutotv.override.CustomOverride;
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

    public static final ICompat COMPAT = FMLLoader.getLoadingModList().getModFileById("oculus") != null ? new OculusCompat() : () -> false;

    public NarutoTV(@NotNull FMLJavaModLoadingContext context) {
        if (FMLLoader.getDist().isClient()) {
            IEventBus forgeBus = MinecraftForge.EVENT_BUS;
            IEventBus modBus = context.getModEventBus();

            FadeCenter.register(forgeBus);
            KeybindCenter.register(forgeBus);
            CustomOverride.register(forgeBus);
            ClientRenderers.register(forgeBus);
            ScreenConstruction.register(forgeBus);

            SourceDragCenter.register(modBus);

            NarutoConfig.Client.register(context);

            OverrideApi.getInstance().set(NarutoGuiCenter.getActive()::isRunnable, NarutoGuiCenter.getActive()::render);
        }

        NarutoConfig.Server.register(context);
    }

    public static ExecutorService io() {
        return IO_WORKER;
    }
}
