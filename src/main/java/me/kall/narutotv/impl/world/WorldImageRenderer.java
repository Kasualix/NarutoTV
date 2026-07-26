package me.kall.narutotv.impl.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.sound.LocalSoundDelegate;
import me.kall.narutotv.impl.world.util.NarutoMath;
import net.minecraft.client.Camera;
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

public class WorldImageRenderer extends NativeImageRenderer implements InWorld {
    private final BlockScreen screen;
    private final LocalSoundDelegate soundDelegate;

    public WorldImageRenderer(@NotNull BlockScreen screen) {
        this.screen = screen;
        this.soundDelegate = new LocalSoundDelegate(screen, this::life, super::getVolume, super::setVolume, super::initAudio, super::pauseAudio, super::resumeAudio);
    }

    @Override
    protected @NotNull ResourceLocation setLocation() {
        return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "screen_" + this.screen.id);
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

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera) {
        if (this.textureLocation == null || this.dynamicTexture == null) return;

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        RenderType renderType = RenderType.entityTranslucent(this.textureLocation);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        NarutoMath.Coords coords = NarutoMath.computeCoords(this.screen, camera);

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
    public @NotNull MediaArgs initMediaArgs() {
        return Sources.get();
    }

    @Override
    public BlockScreen screen() {
        return this.screen;
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        this.soundDelegate.shutdown();
    }

    @Override
    public float initVolume() {
        return this.screen().volume;
    }

    @Override
    public float getVolume() {
        return this.soundDelegate.getVolume();
    }

    @Override
    public void setVolume(float volume) {
        this.soundDelegate.setVolume(volume);
    }

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        return this.soundDelegate.initAudio(seekTo);
    }

    @Override
    public Runnable pauseAudio() {
        return this.soundDelegate.pauseAudio();
    }

    @Override
    public Runnable resumeAudio() {
        return this.soundDelegate.resumeAudio();
    }
}