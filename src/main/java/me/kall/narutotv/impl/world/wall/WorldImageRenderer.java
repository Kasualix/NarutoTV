package me.kall.narutotv.impl.world.wall;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.sound.LocalSoundDelegate;
import me.kall.narutotv.impl.world.util.NarutoLight;
import me.kall.narutotv.impl.world.util.NarutoMath;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WorldImageRenderer extends NativeImageRenderer implements InWorld {
    private final Wall wall;
    private final LocalSoundDelegate soundDelegate;

    private final ThreadLocal<PoseStack> poseStack = new ThreadLocal<>();
    private final ThreadLocal<MultiBufferSource.BufferSource> bufferSource = new ThreadLocal<>();
    private final ThreadLocal<Camera> camera = new ThreadLocal<>();

    public WorldImageRenderer(@NotNull Wall wall) {
        this.wall = wall;
        this.soundDelegate = new LocalSoundDelegate(wall, this::life, super::getVolume, super::setVolume, super::initAudio, super::pauseAudio, super::resumeAudio);
    }

    public void capture(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera) {
        this.poseStack.set(poseStack);
        this.bufferSource.set(bufferSource);
        this.camera.set(camera);
    }

    public void deprecate() {
        this.poseStack.remove();
        this.bufferSource.remove();
        this.camera.remove();
    }

    @Override
    protected @NotNull ResourceLocation setLocation() {
        return ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "screen_" + this.wall.id);
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
    public void update(@Nullable NativeImage frame) {
        if (frame != null) this.updateLightLevel(NarutoLight.forImage(frame));
        super.update(frame);
    }

    @Override
    public void render() {
        PoseStack poseStack = this.poseStack.get();
        MultiBufferSource.BufferSource bufferSource = this.bufferSource.get();
        Camera camera = this.camera.get();
        if (poseStack == null || bufferSource == null || camera == null) return;

        DynamicTexture dynamicTexture = this.narutoTexture.dynamicTexture;
        ResourceLocation textureLocation = this.narutoTexture.textureLocation;

        if (textureLocation == null || dynamicTexture == null) return;
        super.render();

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        RenderType renderType = RenderType.entityTranslucent(textureLocation);
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
    public @NotNull MediaArgs initMediaArgs() {
        return Sources.get();
    }

    @Override
    public Wall wall() {
        return this.wall;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.soundDelegate.shutdown();
    }

    @Override
    public float initVolume() {
        return this.wall().volume;
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