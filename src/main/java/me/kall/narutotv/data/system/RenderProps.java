package me.kall.narutotv.data.system;

import me.kall.narutotv.app.data.MediaArgs;
import org.jetbrains.annotations.Nullable;

public class RenderProps {
    private static final String SHUTDOWN = "narutotv.shutdown";
    private static final String EARLY_START = "narutotv.start";
    private static final String EARLY_END = "narutotv.end";
    private static final String INITIAL_MEDIA = "narutotv.initial";
    private static final String GPU_ACCEL = "narutotv.accel";

    private static final double NANO_TO_SEC = 1_000_000_000.0D;

    public static void shutdown() {
        System.setProperty(SHUTDOWN, "");
    }

    public static boolean isEnd() {
        return System.getProperty(SHUTDOWN) != null;
    }

    public static void markStart() {
        if (System.getProperty(EARLY_START) == null) System.setProperty(EARLY_START, String.valueOf(System.nanoTime()));
    }

    public static void markEnd() {
        System.setProperty(EARLY_END, String.valueOf(System.nanoTime()));
    }

    public static double earlyCost() {
        String startStr = System.getProperty(EARLY_START);
        String endStr = System.getProperty(EARLY_END);

        if (endStr != null && startStr != null) {
            System.clearProperty(EARLY_END);
            System.clearProperty(EARLY_START);

            return ((double) Long.parseLong(endStr) - (double) Long.parseLong(startStr)) / nano2Sec();
        }

        return 0D;
    }

    public static void saveInit(MediaArgs mediaArgs) {
        if (System.getProperty(INITIAL_MEDIA) == null) System.setProperty(INITIAL_MEDIA, mediaArgs.toString());
    }

    public static @Nullable MediaArgs syncInit() {
        String initial = System.getProperty(INITIAL_MEDIA);
        if (initial == null) {
            return null;
        } else {
            System.clearProperty(INITIAL_MEDIA);
            return MediaArgs.fromString(initial);
        }
    }

    public static void turnAccel(boolean on) {
        if (on) {
            System.setProperty(GPU_ACCEL, "");
        } else {
            System.clearProperty(GPU_ACCEL);
        }
    }

    public static boolean gpuAccel() {
        return System.getProperty(GPU_ACCEL) != null;
    }

    public static double nano2Sec() {
        return NANO_TO_SEC;
    }
}
