package me.kall.narutotv.produce;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractProducer {
    public final AtomicBoolean off = new AtomicBoolean(true);

    private final AtomicReference<ExecutorService> executor = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<InputStream> input = new AtomicReference<>();

    public void setup(double seekTo) {
        List<String> command = this.setCommand(seekTo);
        if (command == null) return;

        this.off.set(false);

        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "FrameProducer_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.executor.set(executor);

        executor.submit(() -> {
            try {
                Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                this.process.set(process);

                InputStream input = process.getInputStream();
                this.input.set(input);

                this.forInput(input);
            } catch (IOException | InterruptedException exception) {
                if (this.off.get()) return;
                System.err.println("Exception executing " + command);
                exception.printStackTrace(System.err);
                throw new RuntimeException(exception);
            }
        });
    }

    protected abstract @Nullable List<String> setCommand(double seekTo);
    protected abstract void forInput(InputStream input) throws IOException, InterruptedException;

    public void shutdown() {
        this.off.set(true);

        Process process = this.process.getAndSet(null);
        if (process != null) process.destroyForcibly();

        ExecutorService executor = this.executor.getAndSet(null);
        if (executor != null) executor.shutdownNow();

        InputStream input = this.input.getAndSet(null);
        if (input != null) {
            try {
                input.close();
            } catch (IOException exception) {
                System.err.println("Exception closing input stream.");
                exception.printStackTrace(System.err);
                throw new RuntimeException(exception);
            }
        }
    }
}
