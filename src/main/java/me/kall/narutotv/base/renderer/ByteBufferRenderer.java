package me.kall.narutotv.base.renderer;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.produce.video.BufferFrameProducer;
import me.kall.narutotv.base.renderer.gl.AbstractGLEngine;
import me.kall.narutotv.base.renderer.gl.LoadingFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class ByteBufferRenderer extends AbstractRenderer<ByteBuffer> {
    private final LoadingFrame loading = new LoadingFrame();

    protected @Nullable AbstractGLEngine engine;

    public @Nullable AbstractGLEngine engine() {
        return this.engine;
    }

    @Override
    public @NotNull AbstractFrameProducer<ByteBuffer> initVideo() {
        return BufferFrameProducer.create(this.mediaArgs, 2, AppPaths.absFFmpegPath());
    }

    @Override
    public void update(@Nullable ByteBuffer frame) {
        MediaArgs mediaArgs = this.mediaArgs;
        if (mediaArgs == null || this.engine == null) return;

        int width = mediaArgs.width();
        int height = mediaArgs.height();

        if (frame == null) frame = this.loading.get(width, height);

        this.engine.update(frame);
    }

    @Override
    public void onSetup(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs;

        assert mediaArgs != null;

        this.engine = this.initEngine();
        this.update(null);
    }

    @Override
    public void render() {
        super.render();
        if (this.engine != null) {
            this.engine.render();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.engine != null) {
            this.engine.shutdown();
            this.engine = null;
        }
    }

    protected abstract AbstractGLEngine initEngine();
}