package me.kall.narutotv.context;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;

public class RenderCaptured {
    private static @Nullable GuiGraphics graphics;
    private static @Nullable PoseStack poseStack;
    private static @Nullable Camera camera;
    private static @Nullable MultiBufferSource.BufferSource bufferSource;

    public static @Nullable GuiGraphics graphics() {
        return graphics;
    }

    public static void graphics(@Nullable GuiGraphics graphics) {
        RenderCaptured.graphics = graphics;
    }

    public static @Nullable PoseStack poseStack() {
        return poseStack;
    }

    public static void poseStack(@Nullable PoseStack poseStack) {
        RenderCaptured.poseStack = poseStack;
    }

    public static @Nullable Camera camera() {
        return camera;
    }

    public static void camera(@Nullable Camera camera) {
        RenderCaptured.camera = camera;
    }

    public static @Nullable MultiBufferSource.BufferSource bufferSource() {
        return bufferSource;
    }

    public static void bufferSource(@Nullable MultiBufferSource.BufferSource bufferSource) {
        RenderCaptured.bufferSource = bufferSource;
    }
}
