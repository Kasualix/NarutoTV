package me.kall.narutotv.app.produce.video;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.impl.NarutoProperties;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class BufferFrameProducer extends AbstractFrameProducer<ByteBuffer> {
    private final LinkedBlockingQueue<ByteBuffer> freeBuffers;
    private final Set<ByteBuffer> allBuffers = ConcurrentHashMap.newKeySet();

    private BufferFrameProducer(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() + (mediaArgs.width() / 2) * (mediaArgs.height() / 2) * 2, bufferSeconds, absFFmpegPath);
        this.freeBuffers = new LinkedBlockingQueue<>(this.bufferCapacity);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull BufferFrameProducer create(MediaArgs mediaArgs, int bufferSeconds, String absFFmpegPath) {
        return new BufferFrameProducer(mediaArgs, bufferSeconds, absFFmpegPath);
    }

    @Override
    public BufferFrameProducer setAudioCreation(@NotNull Double2ObjectFunction<AudioProducer> func) {
        super.setAudioCreation(func);
        return this;
    }

    @Override
    public BufferFrameProducer setLifeCreation(@NotNull Double2ObjectFunction<LifetimeController> func) {
        super.setLifeCreation(func);
        return this;
    }

    @Override
    protected void recycleFrame(ByteBuffer frame) {
        this.freeBuffers.offer(frame);
    }

    @Override
    protected int debugLength() {
        return 10;
    }

    @Override
    protected @NotNull ByteBuffer frameCreation() {
        ByteBuffer buffer = this.freeBuffers.poll();
        if (buffer == null) {
            buffer = MemoryUtil.memAlloc(this.frameSize);
            this.allBuffers.add(buffer);
        }
        return buffer;
    }

    @Override
    protected void onFrameCreated(ByteBuffer frame, long frameIndex) throws InterruptedException {
        this.frames.put(new Frame<>(frameIndex, frame));
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
                "-vf", "scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=yuv420p",
                "-f", "rawvideo",
                "-vcodec", "rawvideo",
                "-tune", "zerolatency",
                "-"
        ));
        return command;
    }

    @Override
    public void setup(double setupTime) {
        for (int i = 0; i < this.bufferCapacity; i++) {
            ByteBuffer buffer = MemoryUtil.memAlloc(frameSize);
            this.allBuffers.add(buffer);
            this.freeBuffers.offer(buffer);
        }

        super.setup(setupTime);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        synchronized (this.allBuffers) {
            for (ByteBuffer buffer : this.allBuffers) MemoryUtil.memFree(buffer);
            this.allBuffers.clear();
        }
        this.freeBuffers.clear();
    }
}
