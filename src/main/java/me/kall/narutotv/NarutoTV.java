package me.kall.narutotv;

import net.minecraftforge.fml.common.Mod;
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

    @Contract(pure = true)
    public static ExecutorService io() {
        return FILE_TASKS;
    }
}
