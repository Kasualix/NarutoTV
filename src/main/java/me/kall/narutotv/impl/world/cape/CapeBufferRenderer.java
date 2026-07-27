package me.kall.narutotv.impl.world.cape;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.renderer.ByteBufferRenderer;
import me.kall.narutotv.base.renderer.gl.AbstractGLEngine;
import me.kall.narutotv.impl.world.data.client.VideoCapes;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapeBufferRenderer extends ByteBufferRenderer {
    private final VideoCapes.VideoCape videoCape;

    public CapeBufferRenderer(VideoCapes.VideoCape videoCape) {
        this.videoCape = videoCape;
    }

    @Override
    protected AbstractGLEngine initEngine() {
        return null;
    }

    @Override
    public @NotNull MediaArgs initMediaArgs() {
        return this.videoCape.mediaArgs();
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.shutdown();
            return false;
        }
        return true;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        this.videoCape.narutoTexture().close();
    }

    @Override
    public float initVolume() {
        return 0.0F;
    }

    @Override
    public float getVolume() {
        return 0.0F;
    }

    @Override
    public void setVolume(float volume) {}

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        return null;
    }

    @Override
    public Runnable pauseAudio() {
        return () -> {};
    }

    @Override
    public Runnable resumeAudio() {
        return () -> {};
    }
}
