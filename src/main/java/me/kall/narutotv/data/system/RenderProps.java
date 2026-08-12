package me.kall.narutotv.data.system;

import me.kall.narutotv.app.data.MediaArgs;
import org.jetbrains.annotations.Nullable;

public class RenderProps {
    private static final String SHUTDOWN = "narutotv.shutdown";
    private static final String EARLY_COST = "narutotv.cost";
    private static final String INITIAL_MEDIA = "narutotv.initial";
    private static final String GPU_ACCEL = "narutotv.accel";

    private static final double NANO_TO_SEC = 1_000_000_000.0D;

    public static void shutdown() {
        System.setProperty(SHUTDOWN, "");
    }

    public static boolean isEnd() {
        return System.getProperty(SHUTDOWN) != null;
    }

    public static void markCost(double cost) {
        System.setProperty(EARLY_COST, Double.toString(cost));
    }

    public static double earlyCost() {
        String earlyCost = System.getProperty(EARLY_COST);
        return earlyCost == null ? 0D : Double.parseDouble(earlyCost);
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
