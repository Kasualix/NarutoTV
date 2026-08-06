package me.kall.narutotv.produce.audio;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.produce.AbstractProducer;
import me.kall.narutotv.data.system.AppProps;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class AudioProducer extends AbstractProducer {
    private final AtomicInteger volume;
    private final AtomicBoolean volumeChanged = new AtomicBoolean(true);
    private final MediaArgs mediaArgs;

    private final AtomicBoolean tuneInit = new AtomicBoolean();
    private final AtomicReference<Runnable> onInitTune = new AtomicReference<>();

    public AudioProducer(float volume, MediaArgs mediaArgs) {
        this.volume = new AtomicInteger(Float.floatToIntBits(volume));
        this.mediaArgs = mediaArgs;
    }

    public float volume() {
        return Float.intBitsToFloat(this.volume.get());
    }

    public void volume(float volume) {
        if (this.volume() != volume) {
            this.volume.set(Float.floatToIntBits(volume));
            this.volumeChanged.set(true);
        }
    }

    public void setOnInitTune(@Nullable Runnable onInitTune) {
        this.onInitTune.set(onInitTune);
    }

    @Override
    protected @Nullable @Unmodifiable List<String> setCommand(double seekTo) {
        return this.mediaArgs.hasAudio() ? List.of(AppProps.ffmpegPath(), "-ss", String.valueOf(seekTo + this.mediaArgs.audioStartSec()), "-i", this.mediaArgs.absAudioPath(), "-vn", "-f", "s16le", "-ac", String.valueOf(this.mediaArgs.channelCount()), "-ar", String.valueOf(this.mediaArgs.sampleRate()), "-loglevel", "error", "-") : null;
    }

    @Override
    protected void forInput(InputStream input) throws IOException {
        long device = 0;
        long context = 0;
        int source = 0;

        try {
            device = ALC11.alcOpenDevice((ByteBuffer) null);
            context = ALC11.alcCreateContext(device, (IntBuffer) null);
            EXTThreadLocalContext.alcSetThreadContext(context);
            AL.createCapabilities(ALC.createCapabilities(device));

            source = AL11.alGenSources();
            byte[] bufferArray = new byte[8192];

            while (!this.off.get()) {
                int read = input.read(bufferArray);
                if (read != -1) {
                    ByteBuffer data = MemoryUtil.memAlloc(read);
                    data.put(bufferArray, 0, read).flip();

                    int buffer = AL11.alGenBuffers();
                    AL11.alBufferData(buffer, mediaArgs.openALFormat(), data, mediaArgs.sampleRate());
                    MemoryUtil.memFree(data);
                    AL11.alSourceQueueBuffers(source, buffer);
                }

                if (this.volumeChanged.compareAndSet(true, false)) AL11.alSourcef(source, AL11.AL_GAIN, this.volume());

                int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));

                int queued = AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED);
                if (queued > 0 && AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) != AL11.AL_PLAYING) {
                    if (this.tuneInit.compareAndSet(false, true)) {
                        Runnable onInitTune = this.onInitTune.getAndSet(null);
                        if (onInitTune != null) onInitTune.run();
                    }
                    AL11.alSourcePlay(source);
                }
            }
        } finally {
            if (source != 0) {
                AL11.alSourceStop(source);
                int queued = AL11.alGetSourcei(source, AL11.AL_BUFFERS_QUEUED);
                while (queued-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
                AL11.alDeleteSources(source);
            }
            if (context != 0) {
                EXTThreadLocalContext.alcSetThreadContext(0);
                ALC11.alcDestroyContext(context);
            }
            if (device != 0) {
                ALC11.alcCloseDevice(device);
            }
        }
    }
}