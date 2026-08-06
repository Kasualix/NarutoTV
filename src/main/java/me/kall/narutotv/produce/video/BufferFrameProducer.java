package me.kall.narutotv.produce.video;

import me.kall.narutotv.app.data.MediaArgs;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class BufferFrameProducer extends AbstractFrameProducer<ByteBuffer> {
    private final LinkedBlockingQueue<ByteBuffer> free;
    private final Set<ByteBuffer> all = ConcurrentHashMap.newKeySet();

    public BufferFrameProducer(@NotNull MediaArgs mediaArgs, int bufferSeconds) {
        super(mediaArgs, mediaArgs.width() * mediaArgs.height() + (mediaArgs.width() / 2) * (mediaArgs.height() / 2) * 2, bufferSeconds);
        this.free = new LinkedBlockingQueue<>(this.bufferCapacity);
    }

    @Override
    public void setup(double seekTo) {
        for (int index = 0; index < this.bufferCapacity; index++) {
            ByteBuffer buffer = MemoryUtil.memAlloc(this.frameSize);
            this.all.add(buffer);
            this.free.offer(buffer);
        }

        super.setup(seekTo);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        synchronized (this.all) {
            for (ByteBuffer buffer : this.all) MemoryUtil.memFree(buffer);
            this.all.clear();
        }
        this.free.clear();
    }

    @Override
    @Contract(pure = true)
    protected @NotNull String frameType() {
        return "yuv420p";
    }

    @Override
    protected @NotNull ByteBuffer next() {
        ByteBuffer buffer = this.free.poll();
        if (buffer == null) {
            buffer = MemoryUtil.memAlloc(this.frameSize);
            this.all.add(buffer);
        }
        return buffer;
    }

    @Override
    protected void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException {
        this.frames.put(new Frame<>(frameIndex, frame));
    }

    @Override
    protected void onDead(ByteBuffer frame) {
        this.free.offer(frame);
    }
}
