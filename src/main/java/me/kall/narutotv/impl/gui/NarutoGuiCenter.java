package me.kall.narutotv.impl.gui;

import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.agent.NarutoProperties;

import java.util.concurrent.atomic.AtomicReference;

public class NarutoGuiCenter {
    private static final NarutoGuiCenter INSTANCE = new NarutoGuiCenter();

    public static NarutoGuiCenter getInstance() {
        return INSTANCE;
    }

    public static AbstractRenderer<?> getActive() {
        return INSTANCE.active.get();
    }

    private final GuiImageRenderer nativeImage;
    private final GuiBufferRenderer byteBuffer;

    private final AtomicReference<AbstractRenderer<?>> active;

    private NarutoGuiCenter() {
        this.nativeImage = new GuiImageRenderer();
        this.byteBuffer = new GuiBufferRenderer();
        this.active = new AtomicReference<>(this.byteBuffer);
    }

    public void init() {
        String endStr = System.getProperty(NarutoProperties.EARLY_END);
        String startStr = System.getProperty(NarutoProperties.EARLY_START);
        if (endStr != null && startStr != null) {
            double earlyEnd = (double) Long.parseLong(endStr);
            double earlyStart = (double) Long.parseLong(startStr);
            this.active.get().restart((earlyEnd - earlyStart) / 1_000_000_000.0D);
        }

        System.clearProperty(NarutoProperties.EARLY_END);
        System.clearProperty(NarutoProperties.EARLY_START);
    }

    public synchronized void swap() {
        AbstractRenderer<?> now = this.active.get();
        this.active.set(now == this.nativeImage ? this.byteBuffer : this.nativeImage);
    }
}
