package me.kall.narutotv.base.data;

import net.minecraft.client.gui.GuiGraphics;

public class Graphics {
    private static final ThreadLocal<GuiGraphics> GRAPHICS = new ThreadLocal<>();

    public static void capture(GuiGraphics guiGraphics) {
        GRAPHICS.set(guiGraphics);
    }

    public static void deprecate() {
        GRAPHICS.remove();
    }

    public static GuiGraphics get() {
        return GRAPHICS.get();
    }
}