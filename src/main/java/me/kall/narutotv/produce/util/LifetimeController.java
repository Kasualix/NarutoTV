package me.kall.narutotv.produce.util;

import me.kall.narutotv.data.system.RenderProps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public final class LifetimeController {
    private final AtomicLong setupTime;
    private final AtomicLong lastFetch = new AtomicLong(-1L);

    private final AtomicLong lastLagSpike = new AtomicLong(-1L);

    private final AtomicLong pausedAt = new AtomicLong();
    private final AtomicBoolean paused = new AtomicBoolean();

    private final AtomicBoolean lagSpike = new AtomicBoolean();

    private @Nullable Supplier<Runnable> endRestartFunc, pauseFunc, resumeFunc;

    private @Nullable Supplier<DoubleConsumer> synchronizeFunc;

    private final double fps;
    private final long duration;

    private LifetimeController(double fps, double duration) {
        this.setupTime = new AtomicLong(System.nanoTime());
        this.fps = fps;
        this.duration = (long) (duration * 1000000D);
    }

    public static @NotNull LifetimeController create(double fps, double duration) {
        return new LifetimeController(fps, duration);
    }

    public LifetimeController setEndRestartFunc(@NotNull Supplier<Runnable> endRestartFunc) {
        this.endRestartFunc = endRestartFunc;
        return this;
    }

    public LifetimeController setSynchronizeFunc(@NotNull Supplier<DoubleConsumer> synchronizeFunc) {
        this.synchronizeFunc = synchronizeFunc;
        return this;
    }

    public LifetimeController setPauseFunc(@NotNull Supplier<Runnable> pauseFunc) {
        this.pauseFunc = pauseFunc;
        return this;
    }

    public LifetimeController setResumeFunc(@NotNull Supplier<Runnable> resumeFunc) {
        this.resumeFunc = resumeFunc;
        return this;
    }

    public LifetimeController seekTo(double at) {
        if (!Double.isNaN(at)) {
            this.setupTime.set(System.nanoTime() - (long) (at * RenderProps.nano2Sec()));
            this.lastFetch.set(-1L);
        }
        return this;
    }

    public boolean paused() {
        return this.paused.get();
    }

    public void pause() {
        if (!this.paused()) {
            this.paused.set(true);
            this.pausedAt.set(System.nanoTime());
            if (this.pauseFunc != null) this.pauseFunc.get().run();
        }
    }

    public void resume() {
        if (this.paused()) {
            this.paused.set(false);
            this.setupTime.addAndGet(System.nanoTime() - this.pausedAt.get());
            if (this.resumeFunc != null) this.resumeFunc.get().run();
        }
    }

    public void detectLagSpike() {
        this.lagSpike.set(true);
    }

    public long sinceSetup() {
        return System.nanoTime() - this.setupTime.get();
    }

    public double sinceSetupSec() {
        return (double) this.sinceSetup() / RenderProps.nano2Sec();
    }

    public boolean checkUpdate() {
        if (this.paused()) return false;
        long now = System.nanoTime();
        long last = this.lastFetch.get();

        if (last == -1L) {
            this.lastFetch.set(now);
            return true;
        }

        if (((double) (now - last) * this.fps) >= RenderProps.nano2Sec()) {
            this.lastFetch.set(now);
            return true;
        }

        return false;
    }

    public boolean prefetchable() {
        return ((double) (this.duration - this.sinceSetup()) / RenderProps.nano2Sec()) <= 2.0D;
    }

    public void tick() {
        if (this.paused()) return;

        if (this.endRestartFunc != null && this.sinceSetup() >= this.duration) this.endRestartFunc.get().run();

        if (this.synchronizeFunc != null && this.lagSpike.compareAndSet(true, false)) {
            long last = this.lastLagSpike.get();
            if (last == -1L) {
                this.lastLagSpike.set(System.nanoTime());
                return;
            }

            long now = System.nanoTime();
            if (now - last > RenderProps.nano2Sec() * 2D) {
                this.synchronizeFunc.get().accept(this.sinceSetupSec());
                System.err.println("Lag spike is detected. Restarting.");
                this.lastLagSpike.set(now);
            }
        }
    }
}