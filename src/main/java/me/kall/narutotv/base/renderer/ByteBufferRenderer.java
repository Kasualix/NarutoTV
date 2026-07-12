package me.kall.narutotv.base.renderer;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppPaths;
import me.kall.narutotv.app.produce.video.AbstractFrameProducer;
import me.kall.narutotv.app.produce.video.BufferFrameProducer;
import me.kall.narutotv.base.renderer.gl.LoadingFrame;
import me.kall.narutotv.base.renderer.gl.YuvGLEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

public abstract class ByteBufferRenderer extends AbstractRenderer<ByteBuffer> {
    private final LoadingFrame loading = new LoadingFrame();

    private YuvGLEngine engine;

    @Override
    public @NotNull AbstractFrameProducer<ByteBuffer> initVideo() {
        return BufferFrameProducer.create(this.mediaArgs(), 2, AppPaths.absFFmpegPath());
    }

    @Override
    public synchronized void update(@Nullable ByteBuffer frame) {
        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;

        int width = mediaArgs.width();
        int height = mediaArgs.height();

        if (frame == null) frame = this.loading.get(width, height);

        this.engine.update(frame);
    }

    @Override
    public synchronized void onSetup(double seekTo) {
        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;

        this.engine = new YuvGLEngine(this.fragmentSource(), this.vertexSource());
        this.engine.initTexture(mediaArgs.width(), mediaArgs.height());
        this.update(null);
    }

    @Override
    public synchronized void render() {
        super.render();
        this.engine.render();
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        this.engine.shutdown();
    }

    protected abstract String fragmentSource();
    protected abstract String vertexSource();
}
