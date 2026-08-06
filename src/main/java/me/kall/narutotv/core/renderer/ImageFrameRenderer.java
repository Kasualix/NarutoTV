package me.kall.narutotv.core.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.core.world.light.LightAccessor;
import me.kall.narutotv.core.world.light.Lighter;
import me.kall.narutotv.core.world.NarutoMath;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.fade.Fadable;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import me.kall.narutotv.produce.video.ImageFrameProducer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public abstract class ImageFrameRenderer implements FrameRenderer<NativeImage> {
    protected DynamicTexture dynamicTexture;
    protected ResourceLocation textureLocation;

    @Override
    public @NotNull AbstractFrameProducer<NativeImage> initVideo(@NotNull MediaArgs args) {
        return new ImageFrameProducer(args, 2);
    }

    @Override
    public void setup(@NotNull MediaArgs mediaArgs, double seekTo) {
        this.dynamicTexture = new DynamicTexture(mediaArgs.width(), mediaArgs.height(), false);
        this.textureLocation = this.setLocation();
        Minecraft.getInstance().getTextureManager().register(this.textureLocation, this.dynamicTexture);
        Fadable.setUnfadable(this.textureLocation, true);
    }

    protected abstract ResourceLocation setLocation();

    @Override
    public void update(@NotNull MediaArgs mediaArgs, @Nullable NativeImage frame) {
        if (frame == null || this.dynamicTexture == null) return;
        this.dynamicTexture.setPixels(frame);
        this.dynamicTexture.upload();
        frame.close();
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
        private final Lighter lighter;

        public World(Wall wall) {
            this.wall = wall;
            this.lighter = new Lighter(wall);
        }

        @Override
        @Contract(" -> new")
        protected @NotNull ResourceLocation setLocation() {
            return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_" + this.wall.id);
        }

        @Override
        public void update(@NotNull MediaArgs mediaArgs, @Nullable NativeImage frame) {
            if (frame != null) this.lighter.updateLight(Lighter.forImage(frame));
            super.update(mediaArgs, frame);
        }

        @Override
        public void render() {
            PoseStack poseStack = RenderCaptured.poseStack();
            MultiBufferSource.BufferSource bufferSource = RenderCaptured.bufferSource();
            Camera camera = RenderCaptured.camera();
            if (poseStack == null || bufferSource == null || camera == null || this.textureLocation == null || this.dynamicTexture == null) return;

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();
            RenderType renderType = RenderType.entityTranslucent(this.textureLocation);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            NarutoMath.Coords coords = NarutoMath.computeCoords(this.wall, camera);

            vertex(consumer, pose, normal, coords.bottomFromX(), coords.bottomFromY(), coords.bottomFromZ(), coords.u0(), coords.v0(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.bottomToX(), coords.bottomToY(), coords.bottomToZ(), coords.u1(), coords.v1(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.topToX(), coords.topToY(), coords.topToZ(), coords.u2(), coords.v2(), coords.normalX(), coords.normalY(), coords.normalZ());
            vertex(consumer, pose, normal, coords.topFromX(), coords.topFromY(), coords.topFromZ(), coords.u3(), coords.v3(), coords.normalX(), coords.normalY(), coords.normalZ());
        }

        private static void vertex(@NotNull VertexConsumer consumer, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, double normalX, double normalY, double normalZ) {
            consumer.vertex(pose, (float) x + (float) normalX * 0.1F, (float) y + (float) normalY * 0.1F, (float) z + (float) normalZ * 0.1F)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(normal, (float) normalX, (float) normalY, (float) normalZ)
                    .endVertex();
        }

        @Override
        public int getLight() {
            return this.lighter.getLight();
        }
    }
}