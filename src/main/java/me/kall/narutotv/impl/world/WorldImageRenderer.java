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
import me.kall.narutotv.impl.world.sound.LocalSound;
import me.kall.narutotv.impl.world.util.WorldMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WorldImageRenderer extends NativeImageRenderer implements InWorld {
    private final BlockScreen screen;

    private @Nullable LocalSound localSound;

    public WorldImageRenderer(@NotNull BlockScreen screen) {
        this.screen = screen;
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

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        if (this.textureLocation == null || this.dynamicTexture == null) return;

        Vec3[] corners = this.getCorners();

        WorldMath.Bounds bounds = WorldMath.computeBounds(corners);
        Vec3 normal = WorldMath.computeNormal(corners);
        Vec3 center = WorldMath.computeCenter(corners);

        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;

        WorldMath.QuadData quad = WorldMath.computeQuad(corners, bounds, normal, center, camera, mediaArgs.width(), mediaArgs.height());

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(this.textureLocation));

        vertex(consumer, pose, normalMat, quad.x1(), quad.y1(), quad.z1(), quad.u1(), quad.v1(), quad.normalX(), quad.normalY(), quad.normalZ());
        vertex(consumer, pose, normalMat, quad.x2(), quad.y2(), quad.z2(), quad.u2(), quad.v2(), quad.normalX(), quad.normalY(), quad.normalZ());
        vertex(consumer, pose, normalMat, quad.x3(), quad.y3(), quad.z3(), quad.u3(), quad.v3(), quad.normalX(), quad.normalY(), quad.normalZ());
        vertex(consumer, pose, normalMat, quad.x4(), quad.y4(), quad.z4(), quad.u4(), quad.v4(), quad.normalX(), quad.normalY(), quad.normalZ());
    }

    private Vec3 @NotNull [] getCorners() {
        BlockPos leftBottom = this.screen.leftBottom;
        BlockPos rightBottom = this.screen.rightBottom;
        BlockPos leftTop = this.screen.leftTop;
        BlockPos rightTop = this.screen.rightTop;

        return new Vec3[]{
                new Vec3(leftBottom.getX(), leftBottom.getY(), leftBottom.getZ()),
                new Vec3(rightBottom.getX(), rightBottom.getY(), rightBottom.getZ()),
                new Vec3(leftTop.getX(), leftTop.getY(), leftTop.getZ()),
                new Vec3(rightTop.getX(), rightTop.getY(), rightTop.getZ())
        };
    }

    private static void vertex(@NotNull VertexConsumer vertexConsumer, Matrix4f pose, Matrix3f normalMat, double x, double y, double z, float u, float v, double normalX, double normalY, double normalZ) {
        vertexConsumer.vertex(pose, (float) x, (float) y, (float) z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
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

        if (this.localSound != null) {
            this.localSound.off.run();
            this.localSound = null;
        }
    }

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        if (this.screen.hasLocalSound()) {
            this.localSound = new LocalSound(this.screen);
            this.localSound.on.accept(0D);
            return null;
        } else {
            return super.initAudio(seekTo);
        }
    }

    @Override
    public Runnable pauseAudio() {
        if (this.localSound != null) {
            return this.localSound.off;
        } else {
            return super.pauseAudio();
        }
    }

    @Override
    public Runnable resumeAudio() {
        LocalSound localSound = this.localSound;
        if (localSound != null) {
            return () -> {
                var life = this.life();
                if (life != null) localSound.on.accept((double) life.sinceSetup() / 1_000_000_000D);
            };
        } else {
            return super.resumeAudio();
        }
    }
}