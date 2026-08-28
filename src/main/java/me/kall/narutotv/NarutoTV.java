package me.kall.narutotv;

import me.kall.narutotv.invoker.NarutoInvokers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    public NarutoTV(IEventBus modBus, @NotNull Dist dist, ModContainer container) {
        if (dist.isClient()) {
            if (ModList.get().isLoaded("dragit")) {
                modBus.addListener(NarutoInvokers::invokerRegistry);
            }
        }
    }

    @Contract(pure = true)
    public static ExecutorService io() {
        return FILE_TASKS;
    }
}
