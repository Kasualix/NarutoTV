package me.kall.narutotv.base.renderer;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.util.NarutoLight;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractRenderer<T> {
    public MediaArgs mediaArgs;
    public boolean isRunning;
    public int lightLevel;
    public AbstractFrameProducer<T> video;

    private CompletableFuture<MediaArgs> prefetched;

    public @Nullable AudioProducer audio() {
        return this.video == null ? null : this.video.audio();
    }

    public @Nullable LifetimeController life() {
        return this.video == null ? null : this.video.life();
    }

    //----我----是----华----丽----的----分----割----线----

    public abstract @NotNull AbstractFrameProducer<T> initVideo();
    public abstract @NotNull MediaArgs initMediaArgs();

    public abstract void onSetup(double seekTo);

    public abstract boolean isRunnable();

    public abstract void update(T frame);

    public void setup(double seekTo) {
        if (this.isRunnable()) {
            CompletableFuture<MediaArgs> ready = this.prefetched;
            this.prefetched = null;
            this.mediaArgs = ready != null ? ready.join() : this.initMediaArgs();

            var video = this.initVideo().setAudioCreation(this::initAudio).setLifeCreation(this::initLife);
            this.video = video;
            video.setup(seekTo);

            this.onSetup(seekTo);

            this.isRunning = true;
        }
    }

    public void render() {
        if (!this.isRunnable()) return;
        if (!this.isRunning) this.setup(0D);

        var life = this.life();
        if (life != null) life.tick();

        life = this.life();
        if (life != null) this.prefetch(life);

        var video = this.video;
        if (video != null && life != null && life.checkUpdate()) this.update(video.fetch());
    }

    private void prefetch(@NotNull LifetimeController life) {
        if (this.prefetched != null) return;
        if (life.remainingSec() <= 1.0D) {
            MediaArgs mediaArgs = this.mediaArgs;
            if (mediaArgs != null) Sources.cutInLine(mediaArgs.absVideoPath(), mediaArgs.absAudioPath());
            this.prefetched = CompletableFuture.supplyAsync(this::initMediaArgs, NarutoTV.io());
        }
    }

    public void pause() {
        LifetimeController life = this.life();
        if (life != null) life.pause();
        this.pauseAudio();
    }

    public void resume() {
        LifetimeController life = this.life();
        if (life != null) life.resume();
        this.resumeAudio();
    }

    public void shutdown() {
        this.isRunning = false;

        var video = this.video;
        this.video = null;
        if (video != null) video.shutdown();
    }

    public void restart() {
        this.restart(0D);
    }

    public void restart(double seekTo) {
        if (this.prefetched == null) {
            MediaArgs mediaArgs = this.mediaArgs;
            if (mediaArgs != null) Sources.cutInLine(mediaArgs.absVideoPath(), mediaArgs.absAudioPath());
        }
        this.shutdown();
        this.setup(seekTo);
    }

    //----我----是----华----丽----的----分----割----线----

    public abstract float initVolume();

    public @Nullable LifetimeController initLife(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs;
        if (mediaArgs == null) return null;
        return LifetimeController.create(System.nanoTime(), mediaArgs.fps(), mediaArgs.duration())
                .setEndRestartFunc(() -> this::restart)
                .setSynchronizeFunc(() -> this::restart)
                .setPauseFunc(this::pauseAudio)
                .setResumeFunc(this::resumeAudio)
                .seekTo(seekTo);
    }

    public @Nullable AudioProducer initAudio(double seekTo) {
        var mediaArgs = this.mediaArgs;
        if (mediaArgs == null) return null;
        var audio = AudioProducer.create(mediaArgs, this.initVolume(), AppPaths.absFFmpegPath());
        audio.setup(seekTo);
        return audio;
    }

    public float getVolume() {
        AudioProducer audio = this.audio();
        if (audio == null) return 1.0F;
        return audio.getVolume();
    }

    public void setVolume(float volume) {
        AudioProducer audio = this.audio();
        if (audio == null) return;
        audio.setVolume(volume);
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
            if (audio != null && life != null) audio.setup(life.sinceSetupSec());
        };
    }

    public int getLightLevel() {
        return this.lightLevel;
    }

    public void updateLightLevel(int newLevel) {
        if (this.lightLevel != newLevel) {
            this.lightLevel = newLevel;
            if (this instanceof InWorld inWorld) {
                NarutoLight.checkLight(inWorld.wall());
            }
        }
    }
}