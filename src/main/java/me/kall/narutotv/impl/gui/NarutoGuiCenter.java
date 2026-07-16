package me.kall.narutotv.impl.gui;

import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.agent.NarutoProperties;

import java.util.concurrent.atomic.AtomicReference;

public class NarutoGuiCenter {
    private static final GuiImageRenderer NATIVE_IMAGE = new GuiImageRenderer();
    private static final GuiBufferRenderer BYTE_BUFFER = new GuiBufferRenderer();

    public static final AtomicReference<AbstractRenderer<?>> ACTIVE = new AtomicReference<>(BYTE_BUFFER);

    public static void init() {
        String endStr = System.getProperty(NarutoProperties.EARLY_END);
        String startStr = System.getProperty(NarutoProperties.EARLY_START);
        if (endStr != null && startStr != null) {
            double earlyEnd = (double) Long.parseLong(endStr);
            double earlyStart = (double) Long.parseLong(startStr);
            ACTIVE.get().restart((earlyEnd - earlyStart) / 1_000_000_000.0D);
        }

        System.clearProperty(NarutoProperties.EARLY_END);
        System.clearProperty(NarutoProperties.EARLY_START);
    }

    public static synchronized void swap() {
        AbstractRenderer<?> now = ACTIVE.get();
        ACTIVE.set(now == NATIVE_IMAGE ? BYTE_BUFFER : NATIVE_IMAGE);
    }
}
