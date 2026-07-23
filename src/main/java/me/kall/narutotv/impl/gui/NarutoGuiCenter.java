package me.kall.narutotv.impl.gui;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.agent.NarutoProperties;
import me.kall.narutotv.impl.screen.NarutoGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicReference;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public final class NarutoGuiCenter {
    private static final GuiImageRenderer NATIVE_IMAGE = new GuiImageRenderer();
    private static final GuiBufferRenderer BYTE_BUFFER = new GuiBufferRenderer();

    private static final AtomicReference<AbstractRenderer<?>> ACTIVE = new AtomicReference<>(BYTE_BUFFER);

    private static int interval = 0;

    public static AbstractRenderer<?> getActive() {
        return ACTIVE.get();
    }

    public static boolean isImageRenderer() {
        return ACTIVE.get().equals(NATIVE_IMAGE);
    }

    @SubscribeEvent
    public static void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        AbstractRenderer<?> renderer = ACTIVE.get();

        if (!renderer.isRunning()) return;

        if (interval > 0) {
            interval--;
            return;
        }

        MediaArgs mediaArgs = renderer.mediaArgs();
        if (mediaArgs == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        long window = minecraft.getWindow().getWindow();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F12) != GLFW.GLFW_PRESS || minecraft.screen instanceof NarutoGuiScreen) return;

        minecraft.setScreen(new NarutoGuiScreen(minecraft.screen, mediaArgs));
    }

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
        boolean isImageRenderer = isImageRenderer();
        MediaArgs mediaArgs = isImageRenderer ? NATIVE_IMAGE.mediaArgs() : BYTE_BUFFER.mediaArgs();
        if (mediaArgs != null) Sources.cutInLine(mediaArgs.absVideoPath(), mediaArgs.absAudioPath());
        ACTIVE.set(isImageRenderer ? BYTE_BUFFER : NATIVE_IMAGE);
        NATIVE_IMAGE.shutdown();
        BYTE_BUFFER.shutdown();
    }
}
