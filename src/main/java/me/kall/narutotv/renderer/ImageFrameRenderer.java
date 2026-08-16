package me.kall.narutotv.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.dragit.DragIt;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.fade.Fadable;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import me.kall.narutotv.produce.video.ImageFrameProducer;
import me.kall.narutotv.util.NarutoMath;
import me.kall.narutotv.world.api.RenderCoordsEvent;
import me.kall.narutotv.world.light.LightAccessor;
import me.kall.narutotv.world.light.PosLighter;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageFrameRenderer implements FrameRenderer<NativeImage> {
    protected DynamicTexture dynamicTexture;
    protected ResourceLocation textureLocation;

    @Override
    public @NotNull AbstractFrameProducer<NativeImage> initVideo(@NotNull MediaArgs args) {
        return new ImageFrameProducer(args, 2);
    }

    @Override
    public void setup(@NotNull MediaArgs mediaArgs, double seekTo, boolean ready) {
        this.dynamicTexture = new DynamicTexture(mediaArgs.width(), mediaArgs.height(), false);
        this.textureLocation = this.setLocation();
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this.dynamicTexture);
        Fadable.setUnfadable(this.textureLocation, true);

        if (ready) return;
        try (NativeImage loading = initLoading(mediaArgs.width(), mediaArgs.height())) {
            this.dynamicTexture.setPixels(loading);
            this.dynamicTexture.upload();
        }
    }

    private static @NotNull NativeImage initLoading(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.WHITE);

        int fontSize = Math.max(12, height / 6);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics();
        String text = "Loading...";
        int textWidth = metrics.stringWidth(text);
        while (textWidth > width * 0.9 && fontSize > 10) {
            fontSize--;
            font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics();
            textWidth = metrics.stringWidth(text);
        }
        int textHeight = metrics.getHeight();
        int x = (width - textWidth) / 2;
        int y = (height - textHeight) / 2 + metrics.getAscent();
        graphics.drawString(text, x, y);
        graphics.dispose();

        NativeImage nativeImage = new NativeImage(width, height, false);
        long pixels = ((NativeImageAccessor)(Object)nativeImage).getPixels();

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int argb = image.getRGB(px, py);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g_ = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g_ << 8) | r;
                MemoryUtil.memPutInt(pixels + ((px + (long)py * width) * 4L), abgr);
            }
        }
        return nativeImage;
    }

    protected abstract ResourceLocation setLocation();

    @Override
    public void update(@NotNull MediaArgs mediaArgs, @Nullable AbstractFrameProducer.Frame<NativeImage> frame) {
        if (frame == null || this.dynamicTexture == null) return;
        this.dynamicTexture.setPixels(frame.data());
        this.dynamicTexture.upload();
        frame.data().close();
    }

    @Override
    public void shutdown() {
        if (this.textureLocation != null) Minecraft.getInstance().getTextureManager().release(this.textureLocation);
        if (this.dynamicTexture != null) this.dynamicTexture.close();
        this.dynamicTexture = null;
        this.textureLocation = null;
    }

    public static final class Gui extends ImageFrameRenderer {
        @Override
        @Contract(" -> new")
        protected @NotNull ResourceLocation setLocation() {
            return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "general_client_gui");
        }

        @Override
        public void render() {
            GuiGraphics graphics = RenderCaptured.graphics();
            if (graphics == null || this.textureLocation == null || this.dynamicTexture == null) return;

            int guiWidth = graphics.guiWidth();
            int guiHeight = graphics.guiHeight();

            graphics.blit(this.textureLocation, 0, 0, 0, 0, guiWidth, guiHeight, guiWidth, guiHeight);
        }
    }

    public static final class World extends ImageFrameRenderer implements LightAccessor {
        private final Wall wall;
        private final PosLighter posLighter;

        public World(Wall wall) {
            this.wall = wall;
            this.posLighter = new PosLighter(wall);
        }

        @Override
        @Contract(" -> new")
        protected @NotNull ResourceLocation setLocation() {
            return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_" + this.wall.id);
        }

        @Override
        public void update(@NotNull MediaArgs mediaArgs, @Nullable AbstractFrameProducer.Frame<NativeImage> frame) {
            if (frame != null && this.wall.light) this.posLighter.updateLight(frame.lightMap(), mediaArgs.width(), mediaArgs.height());
            super.update(mediaArgs, frame);
        }

        @Override
        public void render() {
            PoseStack poseStack = RenderCaptured.poseStack();
            MultiBufferSource bufferSource = RenderCaptured.bufferSource();
            Camera camera = RenderCaptured.camera();
            if (poseStack == null || bufferSource == null || camera == null || this.textureLocation == null || this.dynamicTexture == null) return;

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();
            RenderType renderType = RenderType.entityTranslucent(this.textureLocation);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            NarutoMath.Coords coords = NarutoMath.computeCoords(this.wall, camera);

            MinecraftForge.EVENT_BUS.post(new RenderCoordsEvent(coords, this.wall.dimension));

            vertex(consumer, pose, normal, coords.bottomFromX(), coords.bottomFromY(), coords.bottomFromZ(), coords.u0(), coords.v0(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.bottomToX(), coords.bottomToY(), coords.bottomToZ(), coords.u1(), coords.v1(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.topToX(), coords.topToY(), coords.topToZ(), coords.u2(), coords.v2(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.topFromX(), coords.topFromY(), coords.topFromZ(), coords.u3(), coords.v3(), coords.normalX(), coords.normalY(), coords.normalZ());
        }

        private static void vertex(@NotNull VertexConsumer consumer, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, double normalX, double normalY, double normalZ) {
            consumer.vertex(pose, (float) x, (float) y, (float) z)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, (float) normalX, (float) normalY, (float) normalZ)
                    .endVertex();
        }

        @Override
        public int getLight(BlockPos pos) {
            return this.posLighter.getLight(pos);
        }

        @Override
        public void setLightable(boolean lightable) {
            this.posLighter.setLightable(lightable);
        }

        @Override
        public void checkLight() {
            this.posLighter.checkLight();
        }
    }

    public static final class Cape extends ImageFrameRenderer {
        private final me.kall.narutotv.data.world.cape.Cape cape;
        public Cape(me.kall.narutotv.data.world.cape.Cape cape) {

            this.cape = cape;
        }

        @Override
        @Contract(" -> new")
        protected @NotNull ResourceLocation setLocation() {
            return ResourceLocation.fromNamespaceAndPath(DragIt.MOD_ID, "cape_" + this.cape.player().hashCode());
        }

        @Override
        public void render() {
            PoseStack poseStack = RenderCaptured.poseStack();
            MultiBufferSource bufferSource = RenderCaptured.bufferSource();

            if (poseStack == null || bufferSource == null || this.textureLocation == null || this.dynamicTexture == null) return;

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(this.textureLocation));
            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();

            consumer.vertex(pose, -0.3125F, 0.01F, 0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
            consumer.vertex(pose, -0.3125F, 1.01F, 0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
            consumer.vertex(pose, +0.3125F, 1.01F, 0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
            consumer.vertex(pose, +0.3125F, 0.01F, 0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
        }
    }
}