package me.kall.narutotv.base.renderer;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.data.Sources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractRenderer<T> {
    private final AtomicReference<MediaArgs> mediaArgs = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean();

    public final AtomicReference<AbstractFrameProducer<T>> video = new AtomicReference<>();

    public abstract boolean isRunnable();

    public abstract @NotNull AbstractFrameProducer<T> initVideo();
    public abstract @NotNull MediaArgs initMediaArgs();

    public abstract void update(T frame);

    public abstract void onSetup(double seekTo);

    public @Nullable MediaArgs mediaArgs() {
        return this.mediaArgs.get();
    }

    public @Nullable AbstractFrameProducer<T> video() {
        return this.video.get();
    }

    public @Nullable AudioProducer audio() {
        var video = this.video();
        return video == null ? null : video.audio();
    }

    public @Nullable LifetimeController life() {
        var video = this.video();
        return video == null ? null : video.life();
    }

    public boolean isRunning() {
        return this.running.get();
    }

    //----我----是----华----丽----的----分----割----线----

    public synchronized void setup(double seekTo) {
        if (this.isRunnable()) {
            this.mediaArgs.set(this.initMediaArgs());

            var video = this.initVideo().setAudioCreation(this::initAudio).setLifeCreation(this::initLife);
            this.video.set(video);
            video.setup(seekTo);

            this.onSetup(seekTo);

            this.running.set(true);
        }
    }

    public synchronized void render() {
        if (!this.isRunnable()) return;
        if (!this.isRunning()) this.setup(0D);

        var life = this.life();
        if (life != null) life.tick();

        var video = this.video();
        if (video != null && life != null && life.checkUpdate()) this.update(video.fetch());
    }

    public synchronized void pause() {
        LifetimeController life = this.life();
        if (life != null) life.pause();
        this.pauseAudio();
    }

    public synchronized void resume() {
        LifetimeController life = this.life();
        if (life != null) life.resume();
        this.resumeAudio();
    }

    public synchronized void shutdown() {
        this.running.set(false);

        var video = this.video.getAndSet(null);
        if (video != null) video.shutdown();
    }

    //----我----是----华----丽----的----分----割----线----

    public @Nullable AudioProducer initAudio(double seekTo) {
        var mediaArgs = this.mediaArgs();
        if (mediaArgs == null) return null;
        var audio = AudioProducer.create(mediaArgs, 1.0F, AppPaths.absFFmpegPath());
        audio.setup(seekTo);
        return audio;
    }

    public @Nullable LifetimeController initLife(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs();
        if (mediaArgs == null) return null;
        return LifetimeController.create(System.nanoTime(), mediaArgs.fps(), mediaArgs.duration())
                .setEndRestartFunc(this::restart)
                .setSynchronizeFunc(this::restart)
                .setPauseFunc(this.pauseAudio())
                .setResumeFunc(this.resumeAudio())
                .seekTo(seekTo);
    }

    public Runnable pauseAudio() {
        return () -> {
            var audio = this.audio();
            if (audio != null) audio.shutdown();
        };
    }

    public Runnable resumeAudio() {
        return () -> {
            var audio = this.audio();
            var life = this.life();
            if (audio != null && life != null) {
                audio.setup((double) life.sinceSetup() / 1_000_000_000D);
            }
        };
    }

    public synchronized void restart() {
        this.restart(0D);
    }

    public synchronized void restart(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs();
        if (mediaArgs != null) Sources.cutInLine(mediaArgs.absVideoPath(), mediaArgs.absAudioPath());
        this.shutdown();
        this.setup(seekTo);
    }
}
