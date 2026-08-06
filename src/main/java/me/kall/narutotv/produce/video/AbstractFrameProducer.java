package me.kall.narutotv.produce.video;

import com.google.common.collect.Lists;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.produce.AbstractProducer;
import me.kall.narutotv.produce.audio.AudioProducer;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.produce.util.TimeCostDebugger;
import me.kall.narutotv.data.system.AppProps;
import me.kall.narutotv.data.system.RenderProps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public abstract class AbstractFrameProducer<T> extends AbstractProducer {
    public final MediaArgs mediaArgs;

    protected final int frameSize, bufferCapacity;

    protected final LinkedBlockingQueue<Frame<T>> frames;

    private final AtomicLong frameIndex = new AtomicLong(), seekTo = new AtomicLong();

    private final AtomicReference<T> lastFrame = new AtomicReference<>();

    private final TimeCostDebugger frameReading;

    private final AtomicReference<LifetimeController> life = new AtomicReference<>();
    private final AtomicReference<BiFunction<MediaArgs, Double, LifetimeController>> lifeCreation = new AtomicReference<>();

    private final AtomicReference<AudioProducer> audio = new AtomicReference<>();
    private final AtomicReference<BiFunction<MediaArgs, Double, AudioProducer>> audioCreation = new AtomicReference<>();

    private final AtomicBoolean ready = new AtomicBoolean(), eager = new AtomicBoolean();

    private final AtomicReference<ExecutorService> waitExecutor = new AtomicReference<>();
    private final Object waiter = new Object();

    protected AbstractFrameProducer(@NotNull MediaArgs mediaArgs, int frameSize, int bufferSeconds) {
        this.mediaArgs = mediaArgs;
        this.frameSize = frameSize;
        this.bufferCapacity = (int) (this.mediaArgs.fps() * bufferSeconds);
        this.frames = new LinkedBlockingQueue<>(this.bufferCapacity);
        this.frameReading = new TimeCostDebugger(20, "Frame Reading Cost For " + mediaArgs.absVideoPath());
    }

    public AbstractFrameProducer<T> setLifeCreation(BiFunction<MediaArgs, Double, LifetimeController> lifeCreation) {
        this.lifeCreation.set(lifeCreation);
        return this;
    }

    public AbstractFrameProducer<T> setAudioCreation(BiFunction<MediaArgs, Double, AudioProducer> audioCreation) {
        this.audioCreation.set(audioCreation);
        return this;
    }

    public @Nullable AudioProducer audio() {
        return this.audio.get();
    }

    public @Nullable LifetimeController life() {
        return this.life.get();
    }

    @Override
    public void setup(double seekTo) {
        this.frameIndex.set((long) (seekTo * this.mediaArgs.fps()));
        this.seekTo.set(Double.doubleToRawLongBits(seekTo));

        ExecutorService waitExecutor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "FetchWaiter_" + System.nanoTime());
            thread.setDaemon(true);
            return thread;
        });

        this.waitExecutor.set(waitExecutor);

        super.setup(seekTo);
    }

    @Override
    protected @NotNull List<String> setCommand(double seekTo) {
        List<String> command = Lists.newArrayList(AppProps.ffmpegPath(), "-loglevel", "quiet");

        if (RenderProps.gpuAccel()) {
            command.add("-hwaccel");
            command.add("auto");
        }

        command.add("-probesize");
        command.add("32");

        command.add("-analyzeduration");
        command.add("0");

        command.add("-fflags");
        command.add("nobuffer");

        command.add("-flags");
        command.add("low_delay");

        command.add("-ss");
        command.add(String.valueOf(seekTo + this.mediaArgs.videoStartSec()));

        command.add("-i");
        command.add(this.mediaArgs.absVideoPath());

        command.add("-map");
        command.add("0:v:0");

        command.add("-an");
        command.add("-sn");
        command.add("-dn");

        command.add("-threads");
        command.add("0");

        command.add("-vf");
        command.add("scale=" + this.mediaArgs.width() + ":" + this.mediaArgs.height() + ":flags=fast_bilinear,format=" + this.frameType());

        command.add("-f");
        command.add("rawvideo");

        command.add("-vcodec");
        command.add("rawvideo");

        command.add("-tune");
        command.add("zerolatency");

        command.add("-");

        return command;
    }

    @Override
    protected void forInput(@NotNull InputStream input) throws IOException, InterruptedException{
        byte[] bufferArray = new byte[Math.min(this.frameSize, 256 * 1024)];

        while (!this.off.get()) {
            ByteBuffer frame = this.next();
            frame.clear();

            long start = System.nanoTime();
            while (frame.hasRemaining()) {
                int read = input.read(bufferArray, 0, Math.min(bufferArray.length, frame.remaining()));
                if (read == -1) return;
                frame.put(bufferArray, 0, read);
            }
            this.frameReading.record(System.nanoTime() - start);

            this.onBuilt(frame.flip(), this.frameIndex.incrementAndGet());
            this.checkWarmed();
        }
    }

    private void checkWarmed() {
        if (this.frames.remainingCapacity() == 0 && !this.ready.get()) {
            this.waitExecutor.get().submit(() -> {
                synchronized (this.waiter) {
                    while (!this.ready.get()) {
                        if (this.eager.get()) {
                            BiFunction<MediaArgs, Double, LifetimeController> lifeCreation = this.lifeCreation.getAndSet(null);
                            BiFunction<MediaArgs, Double, AudioProducer> audioCreation = this.audioCreation.getAndSet(null);
                            if (lifeCreation != null && audioCreation != null) {
                                double seekTo = Double.longBitsToDouble(this.seekTo.get());

                                AudioProducer audio = audioCreation.apply(this.mediaArgs, seekTo);
                                LifetimeController life = lifeCreation.apply(this.mediaArgs, seekTo);

                                this.life.set(life);

                                if (audio != null) {
                                    audio.setOnInitTune(() -> {
                                        this.life.get().seekTo(seekTo);
                                        this.ready.set(true);
                                    });
                                    this.audio.set(audio);
                                } else {
                                    life.seekTo(seekTo);
                                    this.ready.set(true);
                                }

                                break;
                            }
                        }

                        try {
                            this.waiter.wait();
                        } catch (InterruptedException exception) {
                            if (this.off.get()) break;
                            exception.printStackTrace(System.err);
                            throw new RuntimeException(exception);
                        }
                    }
                }
            });
        }
    }

    public void eager() {
        synchronized (this.waiter) {
            this.eager.set(true);
            this.waiter.notify();
        }
    }

    public @Nullable T fetch() {
        if (this.ready.get()) {
            T lastFrame = this.lastFrame.getAndSet(null);
            if (lastFrame != null) this.onDead(lastFrame);

            LifetimeController life = this.life();
            if (life != null) {
                Frame<T> frame = this.frames.poll();
                if (frame != null) {
                    long expected = (long) (life.sinceSetupSec() * this.mediaArgs.fps());

                    while (frame != null && frame.index() < expected) {
                        this.onDead(frame.data());
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
            }
        }

        return null;
    }

    @Override
    public void shutdown() {
        super.shutdown();

        ExecutorService waitExecutor = this.waitExecutor.getAndSet(null);
        if (waitExecutor != null) waitExecutor.shutdownNow();

        for (Frame<T> frame : this.frames) this.onDead(frame.data());
        this.frames.clear();

        T last = this.lastFrame.getAndSet(null);
        if (last != null) this.onDead(last);

        this.ready.set(false);
        this.eager.set(false);

        this.life.set(null);

        this.lifeCreation.set(null);
        this.audioCreation.set(null);

        this.frameReading.reset();

        AudioProducer audio = this.audio.getAndSet(null);
        if (audio != null) audio.shutdown();
    }

    protected abstract String frameType();

    protected abstract ByteBuffer next();

    protected abstract void onBuilt(ByteBuffer frame, long frameIndex) throws InterruptedException;
    protected abstract void onDead(T frame);

    protected record Frame<T>(long index, T data) {}
}
