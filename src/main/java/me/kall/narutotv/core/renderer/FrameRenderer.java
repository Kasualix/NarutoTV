package me.kall.narutotv.core.renderer;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FrameRenderer<T> {
    @NotNull AbstractFrameProducer<T> initVideo(@NotNull MediaArgs mediaArgs);

    void setup(@NotNull MediaArgs mediaArgs, double seekTo);

    void update(@NotNull MediaArgs mediaArgs, @Nullable T frame);

    void render();

    void shutdown();
}
