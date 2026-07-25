package me.kall.narutotv.app.produce.audio;

import com.google.common.util.concurrent.AtomicDouble;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.AbstractProducer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioProducer extends AbstractProducer {
    private final AtomicDouble volume;
    private final AtomicBoolean volumeChanged = new AtomicBoolean(true);

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

    public void setVolume(float volume) {
        if (this.getVolume() != volume) {
            this.volume.set(volume);
            this.volumeChanged.set(true);
        }
    }

    @Override
    protected @NotNull @Unmodifiable List<String> setCommand(double setupTime) {
        return List.of(this.absFFmpegPath, "-ss", String.valueOf(setupTime), "-i", this.mediaArgs.absAudioPath(), "-vn", "-f", "s16le", "-ac", String.valueOf(this.mediaArgs.channelCount()), "-ar", String.valueOf(this.mediaArgs.sampleRate()), "-loglevel", "error", "-");
    }

    @Override
    protected void forInput(@NotNull InputStream input) throws IOException {
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
            int read;

            while (!this.isCanceled() && (read = input.read(bufferArray)) != -1) {
                ByteBuffer data = MemoryUtil.memAlloc(read);
                data.put(bufferArray, 0, read).flip();

                int buffer = AL11.alGenBuffers();
                AL11.alBufferData(buffer, mediaArgs.openALFormat(), data, mediaArgs.sampleRate());
                MemoryUtil.memFree(data);

                AL11.alSourceQueueBuffers(source, buffer);

                if (this.volumeChanged.compareAndSet(true, false)) AL11.alSourcef(source, AL11.AL_GAIN, this.getVolume());

                if (AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) != AL11.AL_PLAYING) AL11.alSourcePlay(source);

                int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
            }

            if (!this.isCanceled()) {
                if (AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) != AL11.AL_PLAYING) AL11.alSourcePlay(source);
                while (AL11.alGetSourcei(source, AL11.AL_SOURCE_STATE) == AL11.AL_PLAYING) {
                    if (this.isCanceled()) break;
                    if (this.volumeChanged.compareAndSet(true, false)) AL11.alSourcef(source, AL11.AL_GAIN, this.getVolume());
                    int processed = AL11.alGetSourcei(source, AL11.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0) AL11.alDeleteBuffers(AL11.alSourceUnqueueBuffers(source));
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
