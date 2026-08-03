package me.kall.narutotv.impl.world.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Objects;

public class CapeImageRenderer extends NativeImageRenderer {
    private final ClientVideoCapes.VideoCape videoCape;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();
    private final ThreadLocal<MultiBufferSource> bufferSource = new ThreadLocal<>();

    public CapeImageRenderer(ClientVideoCapes.VideoCape videoCape) {
        this.videoCape = videoCape;
    }

    public void capture(PoseStack poseStack, MultiBufferSource bufferSource) {
        this.poseStack.set(poseStack);
        this.bufferSource.set(bufferSource);
    }

    public void deprecate() {
        this.poseStack.remove();
        this.bufferSource.remove();
    }

    @Override
    protected @NotNull ResourceLocation setLocation() {
        return Objects.requireNonNull(this.videoCape.narutoTexture().textureLocation);
    }

    @Override
    public @NotNull MediaArgs initMediaArgs() {
        return this.videoCape.mediaArgs();
    }

    @Override
    public boolean isRunnable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.shutdown();
            return false;
        }
        return true;
    }

    @Override
    public void render() {
        super.render();

        ResourceLocation textureLocation = this.videoCape.narutoTexture.textureLocation;
        PoseStack poseStack = this.poseStack.get();
        MultiBufferSource bufferSource = this.bufferSource.get();

        if (textureLocation == null || poseStack == null || bufferSource == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(textureLocation));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        consumer.vertex(pose, -0.3125F, 0.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
        consumer.vertex(pose, -0.3125F, 1.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
        consumer.vertex(pose, +0.3125F, 1.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
        consumer.vertex(pose, +0.3125F, 0.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(normal, 0F, 0F, -1F).endVertex();
    }

    @Override
    public float initVolume() {
        return 0.0F;
    }

    @Override
    public float getVolume() {
        return 0.0F;
    }

    @Override
    public void setVolume(float volume) {}

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        return null;
    }

    @Override
    public Runnable pauseAudio() {
        return () -> {};
    }

    @Override
    public Runnable resumeAudio() {
        return () -> {};
    }
}
