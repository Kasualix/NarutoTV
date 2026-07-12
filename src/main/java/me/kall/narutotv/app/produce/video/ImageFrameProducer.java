package me.kall.narutotv.app.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.mixin.NativeImageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private ByteBuffer frame;

    private ImageFrameProducer(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() * 3, bufferSeconds, absFFmpegPath);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull ImageFrameProducer create(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        return new ImageFrameProducer(mediaArgs, bufferSeconds, absFFmpegPath);
    }

    @Override
    public ImageFrameProducer setAudioCreation(@NotNull Double2ObjectFunction<AudioProducer> func) {
        this.audioCreation.set(func);
        return this;
    }

    @Override
    public ImageFrameProducer setLifeCreation(@NotNull Double2ObjectFunction<LifetimeController> func) {
        this.lifeCreation.set(func);
        return this;
    }

    @Override
    protected void recycleFrame(@NotNull NativeImage frame) {
        frame.close();
    }

    @Override
    protected int debugLength() {
        return 20;
    }

    @Override
    protected ByteBuffer frameCreation() {
        if (this.frame == null) {
            this.frame = MemoryUtil.memAlloc(this.frameSize);
        } else {
            this.frame.clear();
        }
        return this.frame;
    }

    @Override
    protected void handleFrame(ByteBuffer frame, long frameIndex) throws InterruptedException {
        long start = System.nanoTime();
        NativeImage image = this.buildImage(frame);
        this.timeCostDebugger.record(System.nanoTime() - start);
        this.frames.put(new Frame<>(frameIndex, image));
    }

    @Override
    @Contract("_ -> new")
    protected String @NotNull [] setCommand(double setupTime) {
        return new String[]{this.absFFmpegPath, "-loglevel", "quiet", "-hwaccel", "auto", "-ss", String.valueOf(setupTime), "-i", this.mediaArgs.absVideoPath(), "-map", "0:v:0", "-an", "-sn", "-dn", "-threads", "0", "-vf", "fps=" + this.mediaArgs.fps() + ",scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=rgb24", "-f", "rawvideo", "-vcodec", "rawvideo", "-tune", "zerolatency", "-"};
    }

    private @NotNull NativeImage buildImage(@NotNull ByteBuffer buffer) {
        int width = this.mediaArgs.width();
        int height = this.mediaArgs.height();

        NativeImage image = new NativeImage(width, height, false);

        long destPtr = ((NativeImageAccessor)(Object)image).getPixels();

        int totalPixels = width * height;
        int index = 0;

        for (int i = 0; i < totalPixels; i++) {
            int r = buffer.get(index++) & 0xFF;
            int g = buffer.get(index++) & 0xFF;
            int b = buffer.get(index++) & 0xFF;

            int color = 0xFF000000 | (r << 16) | (g << 8) | b;

            MemoryUtil.memPutInt(destPtr, color);

            destPtr += 4;
        }

        return image;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.frame != null) {
            MemoryUtil.memFree(this.frame);
            this.frame = null;
        }
    }
}
