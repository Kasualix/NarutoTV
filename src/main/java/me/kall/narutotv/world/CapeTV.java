package me.kall.narutotv.world;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.app.FFmpeg;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.file.GamePaths;
import me.kall.narutotv.data.file.Sources;
import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.produce.audio.AudioProducer;
import me.kall.narutotv.renderer.BufferFrameRenderer;
import me.kall.narutotv.renderer.FrameRenderer;
import me.kall.narutotv.renderer.ImageFrameRenderer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class CapeTV<T> extends AbstractTV<T> {
    public static final Consumer<CapeTV<?>> DEATH = tv -> tv.shutdownEntire(false);

    public final Cape cape;

    protected CapeTV(FrameRenderer<T> renderer, Cape cape) {
        super(renderer);
        this.cape = cape;
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
    protected @NotNull MediaArgs newArgs() {
        return this.cape.video().isBlank() ? Sources.random(true) : FFmpeg.read(GamePaths.absConfig(this.cape.video()), null);
    }

    @Override protected @Nullable AudioProducer initAudio(MediaArgs mediaArgs, double seekTo) {
        return null;
    }
    @Override protected float initVolume() {
        return 0;
    }
    @Override protected float getVolume() {
        return 0F;
    }
    @Override public void pauseAudio() {}
    @Override public void resumeAudio() {}
    @Override public void setVolume(float volume) {}

    public static final class Buffer extends CapeTV<ByteBuffer> {
        public Buffer(Cape cape) {
            super(new BufferFrameRenderer.Cape(), cape);
        }
    }

    public static final class Image extends CapeTV<NativeImage> {
        public Image(Cape cape) {
            super(new ImageFrameRenderer.Cape(cape), cape);
        }
    }
}
