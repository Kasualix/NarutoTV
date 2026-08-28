package me.kall.narutotv;

import me.kall.narutotv.invoker.NarutoInvokers;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.Contract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(NarutoTV.MOD_ID)
public final class NarutoTV {
    public static final String MOD_ID = "narutotv";

    private static final ExecutorService FILE_TASKS = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NarutoFileTasks");
        thread.setDaemon(true);
        return thread;
    });

    public NarutoTV(FMLJavaModLoadingContext context) {
        if (FMLLoader.getDist().isClient()) {
            if (ModList.get().isLoaded("dragit")) {
                context.getModEventBus().addListener(NarutoInvokers::invokerRegistry);
            }
        }
    }

    @Contract(pure = true)
    public static ExecutorService io() {
        return FILE_TASKS;
    }
}
