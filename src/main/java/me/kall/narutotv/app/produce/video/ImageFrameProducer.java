package me.kall.narutotv.app.produce.video;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.impl.NarutoProperties;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

public final class ImageFrameProducer extends AbstractFrameProducer<NativeImage> {
    private ByteBuffer frame;

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
        int width = this.mediaArgs.width();
        int height = this.mediaArgs.height();

        NativeImage image = new NativeImage(width, height, false);

        MemoryUtil.memCopy(MemoryUtil.memAddress(frame), ((NativeImageAccessor)(Object)image).getPixels(), (long) width * height * 4L);

        this.frames.put(new Frame<>(frameIndex, image));
    }

    @Override
    protected @NotNull List<String> setCommand(double setupTime) {
        List<String> command = Lists.newArrayList(this.absFFmpegPath, "-loglevel", "quiet");

        if (System.getProperty(NarutoProperties.GPU_ACCEL) != null) {
            command.add("-hwaccel");
            command.add("auto");
        }

        command.addAll(List.of(
                "-probesize", "32",
                "-analyzeduration", "0",
                "-fflags", "nobuffer",
                "-flags", "low_delay",
                "-ss", String.valueOf(setupTime),
                "-i", this.mediaArgs.absVideoPath(),
                "-map", "0:v:0",
                "-an", "-sn", "-dn",
                "-threads", "0",
                "-vf", "scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=rgba",
                "-f", "rawvideo",
                "-vcodec", "rawvideo",
                "-"
        ));
        return command;
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
