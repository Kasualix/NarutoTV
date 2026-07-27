package me.kall.narutotv;

import net.minecraftforge.fml.common.Mod;

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

    public static ExecutorService io() {
        return IO_WORKER;
    }
}
