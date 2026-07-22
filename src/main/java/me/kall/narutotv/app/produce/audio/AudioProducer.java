package me.kall.narutotv.app.produce.audio;

import com.google.common.util.concurrent.AtomicDouble;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.AbstractProducer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class AudioProducer extends AbstractProducer {
    private final AtomicLong device = new AtomicLong(), context = new AtomicLong();
    private final AtomicInteger source = new AtomicInteger();

    private final AtomicDouble volume;
    private final AtomicBoolean volumeInit = new AtomicBoolean(true);

    private final MediaArgs mediaArgs;
    private final String absFFmpegPath;

    private AudioProducer(MediaArgs mediaArgs, float volume, String absFFmpegPath) {
        this.volume = new AtomicDouble(volume);
        this.mediaArgs = mediaArgs;
        this.absFFmpegPath = absFFmpegPath;
    }

    @Contract("_, _, _ -> new")
    public static @NotNull AudioProducer create(MediaArgs mediaArgs, float volume, String absFFmpegPath) {
        return new AudioProducer(mediaArgs, volume, absFFmpegPath);
    }

    public float getVolume() {
        return this.volume.floatValue();
    }

    @Override
    @Contract("_ -> new")
    protected String @NotNull [] setCommand(double setupTime) {
        return new String[]{this.absFFmpegPath, "-ss", String.valueOf(setupTime), "-i", this.mediaArgs.absAudioPath(), "-vn", "-f", "s16le", "-ac", String.valueOf(this.mediaArgs.channelCount()), "-ar", String.valueOf(this.mediaArgs.sampleRate()), "-loglevel", "error", "-"};
    }

    @Override
    protected void forInput(@NotNull InputStream input) throws IOException {
        try {
            long context = this.context.get();
            long device = this.device.get();

            EXTThreadLocalContext.alcSetThreadContext(context);
            AL.createCapabilities(ALC.createCapabilities(device));

            this.source.set(AL11.alGenSources());

            byte[] bufferArray = new byte[8192];
            int read;

            while (!this.isCanceled() && (read = input.read(bufferArray)) != -1) {
                ByteBuffer data = MemoryUtil.memAlloc(read);
                data.put(bufferArray, 0, read).flip();

                int buffer = AL11.alGenBuffers();
                AL11.alBufferData(buffer, this.mediaArgs.openALFormat(), data, this.mediaArgs.sampleRate());
                MemoryUtil.memFree(data);

                int source = this.source.get();
                AL11.alSourceQueueBuffers(source, buffer);

                if (this.volumeInit.compareAndSet(true, false)) {
                    AL11.alSourcef(source, AL11.AL_GAIN, this.getVolume());
                }

                if (AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) != AL11.AL_PLAYING) AL11.alSourcePlay(source);

                int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
            }
        } finally {
            EXTThreadLocalContext.alcSetThreadContext(0);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();

        int source = this.source.getAndSet(0);
        if (source != 0) {
            long context = this.context.get();
            long device = this.device.get();
            if (context != 0 && device != 0) {
                EXTThreadLocalContext.alcSetThreadContext(context);
                AL.createCapabilities(ALC.createCapabilities(device));
                AL11.alSourceStop(source);
                int queued = AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED);
                while (queued-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
                AL11.alDeleteSources(source);
                EXTThreadLocalContext.alcSetThreadContext(0);
            }
        }

        long context = this.context.getAndSet(0);
        long device = this.device.getAndSet(0);
        if (context != 0L) ALC11.alcDestroyContext(context);
        if (device != 0L) ALC11.alcCloseDevice(device);
    }

    @Override
    public void setup(double setupTime) {
        long device = ALC11.alcOpenDevice((ByteBuffer) null);
        long context = ALC11.alcCreateContext(device, (IntBuffer) null);

        this.device.set(device);
        this.context.set(context);

        super.setup(setupTime);
    }
}
