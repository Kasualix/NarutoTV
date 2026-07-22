package me.kall.narutotv.impl.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.NativeImageRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.ext.BindScreen;
import me.kall.narutotv.impl.world.sound.LocalSound;
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

public class WorldImageRenderer extends NativeImageRenderer implements BindScreen {
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
        if (Sources.isEmpty()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            this.shutdown();
            return false;
        }
        return true;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Vec3 camera) {
        if (this.textureLocation == null || this.dynamicTexture == null) return;

        BlockPos leftBottom = screen.leftBottom;
        BlockPos rightBottom = screen.rightBottom;
        BlockPos leftTop = screen.leftTop;
        BlockPos rightTop = screen.rightTop;

        int minX = Math.min(Math.min(leftBottom.getX(), rightBottom.getX()), Math.min(leftTop.getX(), rightTop.getX()));
        int maxX = Math.max(Math.max(leftBottom.getX(), rightBottom.getX()), Math.max(leftTop.getX(), rightTop.getX())) + 1;
        int minY = Math.min(Math.min(leftBottom.getY(), rightBottom.getY()), Math.min(leftTop.getY(), rightTop.getY()));
        int maxY = Math.max(Math.max(leftBottom.getY(), rightBottom.getY()), Math.max(leftTop.getY(), rightTop.getY())) + 1;
        int minZ = Math.min(Math.min(leftBottom.getZ(), rightBottom.getZ()), Math.min(leftTop.getZ(), rightTop.getZ()));
        int maxZ = Math.max(Math.max(leftBottom.getZ(), rightBottom.getZ()), Math.max(leftTop.getZ(), rightTop.getZ())) + 1;

        Vec3 bottomEdge = new Vec3(rightBottom.getX() - leftBottom.getX(), rightBottom.getY() - leftBottom.getY(), rightBottom.getZ() - leftBottom.getZ());
        Vec3 leftEdge = new Vec3(leftTop.getX() - leftBottom.getX(), leftTop.getY() - leftBottom.getY(), leftTop.getZ() - leftBottom.getZ());
        Vec3 normal = bottomEdge.cross(leftEdge).normalize();

        Vec3 center = new Vec3(
                (leftBottom.getX() + rightBottom.getX() + leftTop.getX() + rightTop.getX()) / 4.0,
                (leftBottom.getY() + rightBottom.getY() + leftTop.getY() + rightTop.getY()) / 4.0,
                (leftBottom.getZ() + rightBottom.getZ() + leftTop.getZ() + rightTop.getZ()) / 4.0
        );

        Vec3 toCamera = camera.subtract(center);
        boolean isBack = toCamera.dot(normal) < 0;

        float normalX = (float) normal.x;
        float normalY = (float) normal.y;
        float normalZ = (float) normal.z;

        if (isBack) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }
        double x1, y1, z1; float u1, v1;
        double x2, y2, z2; float u2, v2;
        double x3, y3, z3; float u3, v3;
        double x4, y4, z4; float u4, v4;

        if (Math.abs(normalX) > 0.5) {
            double surfaceX = (normalX > 0 ? maxX : minX) + normalX * 0.01;
            x1 = x2 = x3 = x4 = surfaceX;
            y1 = y2 = minY;
            y3 = y4 = maxY;
            z1 = z4 = minZ;
            z2 = z3 = maxZ;

            float uBase1 = 0.0F, uBase2 = 1.0F, uBase3 = 1.0F, uBase4 = 0.0F;

            if (isBack) {
                u1 = 1.0F - uBase1; u2 = 1.0F - uBase2; u3 = 1.0F - uBase3; u4 = 1.0F - uBase4;
            } else {
                u1 = uBase1; u2 = uBase2; u3 = uBase3; u4 = uBase4;
            }

            v1 = 1.0F; v2 = 1.0F; v3 = 0.0F; v4 = 0.0F;
        } else if (Math.abs(normalY) > 0.5) {
            double surfaceY = (normalY > 0 ? maxY : minY) + normalY * 0.01;
            y1 = y2 = y3 = y4 = surfaceY;

            if (Math.abs(bottomEdge.x) >= Math.abs(bottomEdge.z)) {
                x1 = x4 = minX;  x2 = x3 = maxX;
                z1 = z2 = minZ;  z3 = z4 = maxZ;

                if (leftBottom.getZ() == minZ) {
                    u1 = 0f; v1 = 1f;
                    u2 = 1f; v2 = 1f;
                    u3 = 1f; v3 = 0f;
                    u4 = 0f; v4 = 0f;
                } else {
                    u1 = 0f; v1 = 0f;
                    u2 = 1f; v2 = 0f;
                    u3 = 1f; v3 = 1f;
                    u4 = 0f; v4 = 1f;
                }
            } else {
                x1 = minX; z1 = minZ;
                x2 = maxX; z2 = minZ;
                x3 = maxX; z3 = maxZ;
                x4 = minX; z4 = maxZ;

                if (leftBottom.getX() == minX) {
                    u1 = 0f; v1 = 1f;
                    u2 = 0f; v2 = 0f;
                    u3 = 1f; v3 = 0f;
                    u4 = 1f; v4 = 1f;
                } else {
                    u1 = 0f; v1 = 0f;
                    u2 = 0f; v2 = 1f;
                    u3 = 1f; v3 = 1f;
                    u4 = 1f; v4 = 0f;
                }
            }

            Vec3 toPlayer = new Vec3(camera.x - center.x, 0, camera.z - center.z);
            if (toPlayer.lengthSqr() > 1e-6) {
                toPlayer = toPlayer.normalize();
                Vec3 upDir = new Vec3(leftEdge.x, 0, leftEdge.z).normalize();

                double angle = Math.atan2(upDir.cross(toPlayer).y, upDir.dot(toPlayer));

                double deg = Math.toDegrees(angle);

                int rotationSteps = getSteps(this.mediaArgs(), deg);

                float[] us = {u1, u2, u3, u4};
                float[] vs = {v1, v2, v3, v4};
                for (int i = 0; i < 4; i++) {
                    float u = us[i];
                    float v = vs[i];
                    float du = u - 0.5f;
                    float dv = v - 0.5f;
                    switch (rotationSteps) {
                        case 0:
                            break;
                        case 1:
                            us[i] = 0.5f + dv;
                            vs[i] = 0.5f - du;
                            break;
                        case 2:
                            us[i] = 0.5f - du;
                            vs[i] = 0.5f - dv;
                            break;
                        case 3:
                            us[i] = 0.5f - dv;
                            vs[i] = 0.5f + du;
                            break;
                    }
                }
                u1 = us[0]; v1 = vs[0];
                u2 = us[1]; v2 = vs[1];
                u3 = us[2]; v3 = vs[2];
                u4 = us[3]; v4 = vs[3];
            }

            if (isBack) {
                u1 = 1.0F - u1;
                u2 = 1.0F - u2;
                u3 = 1.0F - u3;
                u4 = 1.0F - u4;
            }
        } else {
            double surfaceZ = (normalZ > 0 ? maxZ : minZ) + normalZ * 0.01;
            x1 = x4 = minX;
            x2 = x3 = maxX;
            y1 = y2 = minY;
            y3 = y4 = maxY;
            z1 = z2 = z3 = z4 = surfaceZ;

            float uBase1 = 0.0F, uBase2 = 1.0F, uBase3 = 1.0F, uBase4 = 0.0F;
            if (isBack) {
                u1 = 1.0F - uBase1; u2 = 1.0F - uBase2; u3 = 1.0F - uBase3; u4 = 1.0F - uBase4;
            } else {
                u1 = uBase1; u2 = uBase2; u3 = uBase3; u4 = uBase4;
            }
            v1 = 1.0F; v2 = 1.0F; v3 = 0.0F; v4 = 0.0F;
        }

        if (isBack && Math.abs(normalY) > 0.5) {
            u1 = 1.0F - u1;  v1 = 1.0F - v1;
            u2 = 1.0F - u2;  v2 = 1.0F - v2;
            u3 = 1.0F - u3;  v3 = 1.0F - v3;
            u4 = 1.0F - u4;  v4 = 1.0F - v4;
        }

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(this.textureLocation));

        vertex(consumer, pose, normalMat, x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        vertex(consumer, pose, normalMat, x2, y2, z2, u2, v2, normalX, normalY, normalZ);
        vertex(consumer, pose, normalMat, x3, y3, z3, u3, v3, normalX, normalY, normalZ);
        vertex(consumer, pose, normalMat, x4, y4, z4, u4, v4, normalX, normalY, normalZ);
    }

    public static int getSteps(@Nullable MediaArgs mediaArgs, double deg) {
        assert mediaArgs != null;

        boolean square = Math.abs(mediaArgs.width() - mediaArgs.height()) < 1;
        int rotationSteps;
        if (square) {
            int steps = (int) Math.round(deg / 90.0);
            steps = ((steps % 4) + 4) % 4;
            rotationSteps = steps;
        } else {
            if (deg > 90 || deg < -90) {
                rotationSteps = 2;
            } else {
                rotationSteps = 0;
            }
        }
        return rotationSteps;
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
