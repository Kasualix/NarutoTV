package me.kall.narutotv.app.produce.video;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private final LinkedBlockingQueue<NativeImage> freeImages;
    private final Set<NativeImage> allImages = ConcurrentHashMap.newKeySet();

    private NativeImage currentImage;

    private ImageFrameProducer(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() * 4, bufferSeconds, absFFmpegPath);
        this.freeImages = new LinkedBlockingQueue<>(this.bufferCapacity);
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
        this.freeImages.offer(frame);
    }

    @Override
    protected int debugLength() {
        return 20;
    }

    @Override
    protected ByteBuffer frameCreation() {
        NativeImage image = this.freeImages.poll();
        if (image == null) {
            image = new NativeImage(this.mediaArgs.width(), this.mediaArgs.height(), false);
            this.allImages.add(image);
        }

        this.currentImage = image;
        return MemoryUtil.memByteBuffer(((NativeImageAccessor)(Object)image).getPixels(), this.frameSize);
    }

    @Override
    protected void onFrameCreated(ByteBuffer frame, long frameIndex) throws InterruptedException {
        this.frames.put(new Frame<>(frameIndex, this.currentImage));
        this.currentImage = null;
    }

    @Override
    @Contract("_ -> new")
    protected String @NotNull [] setCommand(double setupTime) {
        return new String[]{this.absFFmpegPath, "-loglevel", "quiet", "-hwaccel", "auto", "-ss", String.valueOf(setupTime), "-i", this.mediaArgs.absVideoPath(), "-map", "0:v:0", "-an", "-sn", "-dn", "-threads", "0", "-vf", "fps=" + this.mediaArgs.fps() + ",scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=rgba", "-f", "rawvideo", "-vcodec", "rawvideo", "-tune", "zerolatency", "-"};
    }

    @Override
    public void setup(double setupTime) {
        for (int i = 0; i < this.bufferCapacity; i++) {
            NativeImage image = new NativeImage(this.mediaArgs.width(), this.mediaArgs.height(), false);
            this.allImages.add(image);
            this.freeImages.offer(image);
        }

        super.setup(setupTime);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        synchronized (this.allImages) {
            for (NativeImage image : this.allImages) image.close();
            this.allImages.clear();
        }
        this.freeImages.clear();
        this.currentImage = null;
    }
}
