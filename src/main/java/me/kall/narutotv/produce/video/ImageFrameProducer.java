package me.kall.narutotv.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private ByteBuffer frame;

    public ImageFrameProducer(@NotNull MediaArgs mediaArgs, int bufferSeconds) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() * 4, bufferSeconds);
    }

    @Override
    @Contract(pure = true)
    protected @NotNull String frameType() {
        return "rgba";
    }

    @Override
    protected ByteBuffer next() {
        if (this.frame == null) {
            this.frame = MemoryUtil.memAlloc(this.frameSize);
        } else {
            this.frame.clear();
        }
        return this.frame;
    }

    @Override
    protected void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException {
        int width = this.mediaArgs.width();
        int height = this.mediaArgs.height();

        NativeImage image = new NativeImage(width, height, false);

        long pixels = ((NativeImageAccessor)(Object)image).getPixels();

        MemoryUtil.memCopy(MemoryUtil.memAddress(frame), pixels, (long) width * (long) height * 4L);

        byte[] lightMap = new byte[width * height];
        for (int i = 0; i < width * height; i++) {
            int pixel = MemoryUtil.memGetInt(pixels + i * 4L);

            int r = (pixel) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = (pixel >> 16) & 0xFF;

            int luma = (54 * r + 183 * g + 19 * b) >> 8;

            lightMap[i] = (byte) ((luma * 15 + 127) / 255);
        }

        this.frames.put(new Frame<>(frameIndex, image, lightMap));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.frame != null) {
            MemoryUtil.memFree(this.frame);
            this.frame = null;
        }
    }

    @Override
    protected void onDead(@NotNull NativeImage frame) {
        frame.close();
    }
}
