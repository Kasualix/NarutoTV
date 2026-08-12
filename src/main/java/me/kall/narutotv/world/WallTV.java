package me.kall.narutotv.world;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.renderer.BufferFrameRenderer;
import me.kall.narutotv.renderer.FrameRenderer;
import me.kall.narutotv.renderer.ImageFrameRenderer;
import me.kall.narutotv.world.light.LightAccessor;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.produce.audio.AudioProducer;
import me.kall.narutotv.produce.util.LifetimeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public abstract class WallTV<T> extends AbstractTV<T> {
    public static final Consumer<WallTV<?>> DEATH = tv -> tv.shutdownEntire(false);

    public final Wall wall;

    protected @Nullable Runnable soundOff;
    protected @Nullable DoubleConsumer soundOn;

    protected WallTV(FrameRenderer<T> renderer, Wall wall) {
        super(renderer);
        this.wall = wall;
    }

    public int getLight(BlockPos pos) {
        return ((LightAccessor)this.renderer).getLight(pos);
    }

    public void setLight(boolean light) {
        ((LightAccessor)this.renderer).setLightable(light);
    }

    public void checkLight() {
        ((LightAccessor)this.renderer).checkLight();
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.shutdownEntire(false);
            return false;
        }
        return true;
    }

    @Override
    public @NotNull MediaArgs newArgs() {
        return this.wall.video.isBlank() ? Sources.random(true) : Sources.get(GamePaths.absConfig(this.wall.video));
    }

    @Override
    public void shutdownCurrent(boolean keepArgs) {
        super.shutdownCurrent(keepArgs);

        if (this.soundOff != null) {
            this.soundOff.run();
            this.soundOff = null;
        }
    }

    @Override
    public float initVolume() {
        return this.wall.volume;
    }

    @Override
    public float getVolume() {
        return this.wall.volume;
    }

    @Override
    public void setVolume(float volume) {
        if (this.wall.hasLocalSound()) {
            if (this.video == null) return;
            LifetimeController life = this.video.life();
            if (life == null) return;
            this.wall.volume = volume;
            if (this.soundOff != null) this.soundOff.run();
            if (this.soundOn != null) this.soundOn.accept(life.sinceSetupSec());
        } else {
            super.setVolume(volume);
        }
    }

    @Override
    public @Nullable AudioProducer initAudio(MediaArgs mediaArgs, double seekTo) {
        if (this.wall.hasLocalSound()) {
            this.soundOff = () -> Minecraft.getInstance().getSoundManager().stop(this.wall.localSound, SoundSource.MUSIC);
            this.soundOn = (seekToArg) -> {
                if (this.soundOff != null) this.soundOff.run();
                Minecraft.getInstance().getSoundManager().play(new NarutoSound(this.wall, seekToArg));
            };
            this.soundOn.accept(seekTo);
            return null;
        } else {
            return super.initAudio(mediaArgs, seekTo);
        }
    }

    @Override
    public void pauseAudio() {
        if (this.soundOff != null) {
            this.soundOff.run();
        } else {
            super.pauseAudio();
        }
    }

    @Override
    public void resumeAudio() {
        if (this.soundOn != null) {
            if (this.video == null) return;
            LifetimeController life = this.video.life();
            if (life == null) return;
            this.soundOn.accept(life.sinceSetupSec());
        } else {
            super.resumeAudio();
        }
    }

    public static final class NarutoSound extends SimpleSoundInstance {
        public final double seekTo;

        public NarutoSound(@NotNull Wall wall, double seekTo) {
            super(wall.localSound, SoundSource.MUSIC, wall.volume, 1.0F, RandomSource.create(), false, 0, Attenuation.LINEAR, wall.centerX, wall.centerY, wall.centerZ, false);
            this.seekTo = seekTo;
        }
    }

    public static final class Buffer extends WallTV<ByteBuffer> {
        public Buffer(Wall wall) {
            super(new BufferFrameRenderer.World(wall), wall);
        }
    }

    public static final class Image extends WallTV<NativeImage> {
        public Image(Wall wall) {
            super(new ImageFrameRenderer.World(wall), wall);
        }
    }
}
