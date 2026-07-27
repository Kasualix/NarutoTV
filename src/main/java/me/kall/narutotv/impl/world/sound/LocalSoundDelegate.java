package me.kall.narutotv.impl.world.sound;

import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.impl.world.data.Wall;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class LocalSoundDelegate {
    private final Wall wall;
    private final Supplier<LifetimeController> lifeSupplier;
    private final DoubleSupplier superGetVolume;
    private final FloatConsumer superSetVolume;
    private final Double2ObjectFunction<AudioProducer> superInitAudio;
    private final Supplier<Runnable> superPauseAudio;
    private final Supplier<Runnable> superResumeAudio;

    private @Nullable LocalSoundCtrl localSoundCtrl;

    public LocalSoundDelegate(Wall wall, Supplier<LifetimeController> lifeSupplier, DoubleSupplier superGetVolume, FloatConsumer superSetVolume, Double2ObjectFunction<AudioProducer> superInitAudio, Supplier<Runnable> superPauseAudio, Supplier<Runnable> superResumeAudio) {
        this.wall = wall;
        this.lifeSupplier = lifeSupplier;
        this.superGetVolume = superGetVolume;
        this.superSetVolume = superSetVolume;
        this.superInitAudio = superInitAudio;
        this.superPauseAudio = superPauseAudio;
        this.superResumeAudio = superResumeAudio;
    }

    public float getVolume() {
        return this.localSoundCtrl != null ? this.wall.volume : (float) this.superGetVolume.getAsDouble();
    }

    public void setVolume(float volume) {
        var life = this.lifeSupplier.get();
        if (this.localSoundCtrl != null && life != null) {
            this.wall.volume = volume;
            this.localSoundCtrl.setVolume(this.wall);
            this.localSoundCtrl.on().accept(life.sinceSetupSec());
        } else {
            this.superSetVolume.accept(volume);
        }
    }

    public @Nullable AudioProducer initAudio(double seekTo) {
        if (this.wall.hasLocalSound()) {
            this.localSoundCtrl = new LocalSoundCtrl(this.wall);
            this.localSoundCtrl.on().accept(0D);
            return null;
        } else {
            return this.superInitAudio.apply(seekTo);
        }
    }

    public Runnable pauseAudio() {
        return this.localSoundCtrl != null ? this.localSoundCtrl.off() : this.superPauseAudio.get();
    }

    public Runnable resumeAudio() {
        LocalSoundCtrl ctrl = this.localSoundCtrl;
        if (ctrl != null) {
            return () -> {
                var life = this.lifeSupplier.get();
                if (life != null) ctrl.on().accept(life.sinceSetupSec());
            };
        } else {
            return this.superResumeAudio.get();
        }
    }

    public void shutdown() {
        if (this.localSoundCtrl != null) {
            this.localSoundCtrl.off().run();
            this.localSoundCtrl = null;
        }
    }
}