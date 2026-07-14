package me.kall.narutotv.app.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.app.util.TimeCostDebugger;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private ByteBuffer frame;

    private final TimeCostDebugger frameBuilding = new TimeCostDebugger(20, "Frame Building");

    private ImageFrameProducer(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() * 4, bufferSeconds, absFFmpegPath);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull ImageFrameProducer create(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        return new ImageFrameProducer(mediaArgs, bufferSeconds, absFFmpegPath);
    }

    @Override
    public ImageFrameProducer setAudioCreation(@NotNull Double2ObjectFunction<AudioProducer> func) {
        super.setAudioCreation(func);
        return this;
    }

    @Override
    public ImageFrameProducer setLifeCreation(@NotNull Double2ObjectFunction<LifetimeController> func) {
        super.setLifeCreation(func);
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
    protected void onFrameCreated(ByteBuffer frame, long frameIndex) throws InterruptedException {
        long start = System.nanoTime();

        int width = this.mediaArgs.width();
        int height = this.mediaArgs.height();

        NativeImage image = new NativeImage(width, height, false);

        MemoryUtil.memCopy(MemoryUtil.memAddress(frame), ((NativeImageAccessor)(Object)image).getPixels(), (long) width * height * 4L);

        this.frameBuilding.record(System.nanoTime() - start);
        this.frames.put(new Frame<>(frameIndex, image));
    }

    @Override
    protected void onLagSpike() {
        this.frameBuilding.printDebug();
    }

    @Override
    @Contract("_ -> new")
    protected String @NotNull [] setCommand(double setupTime) {
        return new String[]{this.absFFmpegPath, "-loglevel", "quiet", "-hwaccel", "auto", "-ss", String.valueOf(setupTime), "-i", this.mediaArgs.absVideoPath(), "-map", "0:v:0", "-an", "-sn", "-dn", "-threads", "0", "-vf", "fps=" + this.mediaArgs.fps() + ",scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=rgba", "-f", "rawvideo", "-vcodec", "rawvideo", "-tune", "zerolatency", "-"};
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
