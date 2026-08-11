package me.kall.narutotv.core;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.renderer.FrameRenderer;
import me.kall.narutotv.produce.audio.AudioProducer;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractTV<T> {
    public MediaArgs mediaArgs;
    public AbstractFrameProducer<T> video;

    protected boolean isRunning;
    protected CompletableFuture<AbstractFrameProducer<T>> prefetched;

    public final FrameRenderer<T> renderer;

    public AbstractTV(FrameRenderer<T> renderer) {
        this.renderer = renderer;
    }

    public void setup(double seekTo) {
        if (this.isRunnable()) {
            AbstractFrameProducer<T> nextVideo = this.prefetched != null && this.prefetched.isDone() ? this.prefetched.join() : null;
            this.prefetched = null;
            if (this.mediaArgs == null) this.mediaArgs = nextVideo != null ? nextVideo.mediaArgs : this.newArgs();
            this.video = nextVideo != null ? nextVideo : this.renderer.initVideo(this.mediaArgs).setAudioCreation(this::initAudio).setLifeCreation(this::initLife);
            if (this.video.off.get()) this.video.setup(seekTo);

            this.renderer.setup(this.mediaArgs, seekTo);

            this.isRunning = true;
        }
    }

    public abstract boolean isRunnable();

    protected abstract @NotNull MediaArgs newArgs();

    protected @Nullable AudioProducer initAudio(MediaArgs mediaArgs, double seekTo) {
        AudioProducer audio = new AudioProducer(this.initVolume(), mediaArgs);
        audio.setup(seekTo);
        return audio;
    }

    protected abstract float initVolume();

    protected @NotNull LifetimeController initLife(@NotNull MediaArgs mediaArgs, double seekTo) {
        return LifetimeController.create(mediaArgs.fps(), mediaArgs.duration())
                .setEndRestartFunc(() -> () -> {
                    this.shutdownCurrent(true);
                    this.setup(0D);
                })
                .setSynchronizeFunc(() -> (seekToArg) -> {
                    this.shutdownEntire(true);
                    this.setup(seekToArg);
                })
                .setPauseFunc(() -> this::pauseAudio)
                .setResumeFunc(() -> this::resumeAudio)
                .seekTo(seekTo);
    }

    public void pauseAudio() {
        if (this.video == null) return;
        AudioProducer audio = this.video.audio();
        if (audio != null) audio.shutdown();
    }

    public void resumeAudio() {
        if (this.video == null) return;
        AudioProducer audio = this.video.audio();
        LifetimeController life = this.video.life();
        if (audio != null && life != null) audio.setup(life.sinceSetupSec());
    }

    protected float getVolume() {
        if (this.video == null) return 1.0F;
        AudioProducer audio = this.video.audio();
        if (audio == null) return 1.0F;
        return audio.volume();
    }

    public void setVolume(float volume) {
        if (this.video == null) return;
        AudioProducer audio = this.video.audio();
        if (audio == null) return;
        audio.volume(volume);
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void render() {
        if (this.isRunnable()) {
            if (!this.isRunning()) this.setup(0D);

            if (this.video == null) return;
            this.video.eager();

            LifetimeController life = this.video.life();
            if (life != null) {
                life.tick();

                if (this.prefetched == null && life.prefetchable()) {
                    this.prefetched = CompletableFuture.supplyAsync(() -> {
                        AbstractFrameProducer<T> video = this.renderer.initVideo(this.video.mediaArgs).setAudioCreation(this::initAudio).setLifeCreation(this::initLife);
                        video.setup(0D);
                        return video;
                    }, NarutoTV.io());
                }

                if (life.checkUpdate()) {
                    this.renderer.update(this.mediaArgs, this.video.fetch());
                }
            }

            this.renderer.render();
        }
    }

    public void shutdownCurrent(boolean keepArgs) {
        this.isRunning = false;

        if (this.video != null) {
            this.video.shutdown();
            this.video = null;
        }

        this.renderer.shutdown();

        if (!keepArgs) this.mediaArgs = null;
    }

    public void shutdownEntire(boolean keepArgs) {
        this.shutdownCurrent(keepArgs);

        if (this.prefetched != null) {
            this.prefetched.thenAccept(AbstractFrameProducer::shutdown);
            this.prefetched = null;
        }
    }
}
