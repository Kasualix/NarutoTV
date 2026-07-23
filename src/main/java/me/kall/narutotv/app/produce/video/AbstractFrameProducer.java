package me.kall.narutotv.app.produce.video;

import it.unimi.dsi.fastutil.doubles.Double2ObjectFunction;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.AbstractProducer;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.app.util.TimeCostDebugger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractFrameProducer<T> extends AbstractProducer {
    public final MediaArgs mediaArgs;

    protected final int frameSize, bufferCapacity;

    protected final String absFFmpegPath;

    protected final LinkedBlockingQueue<Frame<T>> frames;

    private final AtomicLong frameIndex = new AtomicLong(), setupTime = new AtomicLong();
    private final AtomicReference<T> lastFrame = new AtomicReference<>();

    private final AtomicBoolean fetchable = new AtomicBoolean();

    private final TimeCostDebugger frameReading;

    private final AtomicReference<LifetimeController> life = new AtomicReference<>();
    private final AtomicReference<Double2ObjectFunction<LifetimeController>> lifeCreation = new AtomicReference<>();

    private final AtomicReference<AudioProducer> audio = new AtomicReference<>();
    private final AtomicReference<Double2ObjectFunction<AudioProducer>> audioCreation = new AtomicReference<>();

    protected AbstractFrameProducer(@NotNull MediaArgs mediaArgs, int frameSize, int bufferSeconds, String absFFmpegPath) {
        this.mediaArgs = mediaArgs;
        this.frameSize = frameSize;
        this.bufferCapacity = (int) (this.mediaArgs.fps() * bufferSeconds);
        this.absFFmpegPath = absFFmpegPath;
        this.frames = new LinkedBlockingQueue<>(this.bufferCapacity);
        this.frameReading = new TimeCostDebugger(this.debugLength(), "Frame Reading For " + mediaArgs.absVideoPath());
    }

    public AbstractFrameProducer<T> setLifeCreation(Double2ObjectFunction<LifetimeController> lifeCreation) {
        this.lifeCreation.set(lifeCreation);
        return this;
    }

    public AbstractFrameProducer<T> setAudioCreation(Double2ObjectFunction<AudioProducer> audioCreation) {
        this.audioCreation.set(audioCreation);
        return this;
    }

    public @Nullable AudioProducer audio() {
        return this.audio.get();
    }

    public @Nullable LifetimeController life() {
        return this.life.get();
    }

    public @Nullable T fetch() {
        if (!this.fetchable.get()) return null;

        T lastFrame = this.lastFrame.getAndSet(null);
        if (lastFrame != null) this.recycleFrame(lastFrame);

        LifetimeController life = this.life();
        if (life == null) return null;

        Frame<T> frame = this.frames.poll();
        if (frame == null) return null;

        long expected = (long) ((double) life.sinceSetup() / 1_000_000_000L * this.mediaArgs.fps());

        while (frame != null && frame.index() < expected) {
            this.recycleFrame(frame.data());
            frame = this.frames.poll();
        }

        if (frame == null) {
            life.detectLagSpike();
            this.frameReading.printDebug();
            return null;
        }

        this.lastFrame.set(frame.data());
        return frame.data();
    }

    @Override
    protected void forInput(@NotNull InputStream input) throws IOException, InterruptedException {
        ReadableByteChannel channel = Channels.newChannel(input);

        while (!this.isCanceled()) {
            ByteBuffer frame = this.frameCreation();
            frame.clear();

            long start = System.nanoTime();
            while (frame.hasRemaining()) {
                if (channel.read(frame) == -1) return;
            }

            this.frameReading.record(System.nanoTime() - start);

            this.onFrameCreated(frame.flip(), this.frameIndex.incrementAndGet());

            if (this.frames.remainingCapacity() == 0 && !this.fetchable.get()) {
                Double2ObjectFunction<LifetimeController> lifeCreation = this.lifeCreation.getAndSet(null);
                Double2ObjectFunction<AudioProducer> audioCreation = this.audioCreation.getAndSet(null);
                if (lifeCreation != null && audioCreation != null) {
                    double setupTime = Double.longBitsToDouble(this.setupTime.get());
                    this.audio.set(audioCreation.apply(setupTime));
                    this.life.set(lifeCreation.apply(setupTime));
                    this.fetchable.set(true);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        for (Frame<T> frame : this.frames) this.recycleFrame(frame.data());
        this.frames.clear();

        T lastFetched = this.lastFrame.getAndSet(null);
        if (lastFetched != null) this.recycleFrame(lastFetched);

        this.fetchable.set(false);
        this.life.set(null);

        this.frameReading.reset();

        AudioProducer audio = this.audio.getAndSet(null);
        if (audio != null) audio.shutdown();
    }

    @Override
    public void setup(double setupTime) {
        this.frameIndex.set((long) (setupTime * this.mediaArgs.fps()));
        this.setupTime.set(Double.doubleToRawLongBits(setupTime));

        super.setup(setupTime);
    }

    protected abstract void recycleFrame(T frame);
    protected abstract int debugLength();

    protected abstract ByteBuffer frameCreation();
    protected abstract void onFrameCreated(ByteBuffer frame, long frameIndex) throws InterruptedException;

    public record Frame<T>(long index, T data) {}
}
