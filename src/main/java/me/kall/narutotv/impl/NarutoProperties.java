package me.kall.narutotv.impl;

import me.kall.narutotv.app.data.MediaArgs;
import org.jetbrains.annotations.Nullable;

public class NarutoProperties {
    public static final String SHUTDOWN = "narutotv.shutdown";

    public static final String EARLY_START = "narutotv.start";
    public static final String EARLY_END = "narutotv.end";

    public static final String INITIAL_MEDIA = "narutotv.initial";

    public static final String GPU_ACCEL = "narutotv.accel";

    public static @Nullable MediaArgs sync() {
        String initial = System.getProperty(NarutoProperties.INITIAL_MEDIA);

        if (initial == null) {
            return null;
        } else {
            System.clearProperty(NarutoProperties.INITIAL_MEDIA);
            return MediaArgs.fromString(initial);
        }
    }
}
