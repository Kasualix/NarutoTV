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
import me.kall.narutotv.impl.world.sound.LocalSoundEngine;
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

    private @Nullable LocalSoundEngine localSoundEngine;

    public WorldImageRenderer(BlockScreen screen) {
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
        ResourceLocation nextFrame = this.textureLocation;
        if (nextFrame == null) return;
        BlockScreen blockScreen = this.screen;

        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;
        int width = mediaArgs.width();
        int height = mediaArgs.height();

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(nextFrame));

        BlockPos leftBottomCorner = blockScreen.leftBottom;
        BlockPos leftTopCorner = blockScreen.leftTop;
        BlockPos rightBottomCorner = blockScreen.rightBottom;
        BlockPos rightTopCorner = blockScreen.rightTop;

        double leftBottomX = leftBottomCorner.getX();
        double leftBottomY = leftBottomCorner.getY();
        double leftBottomZ = leftBottomCorner.getZ();

        double leftTopX = leftTopCorner.getX();
        double leftTopY = leftTopCorner.getY();
        double leftTopZ = leftTopCorner.getZ();

        double rightBottomX = rightBottomCorner.getX();
        double rightBottomY = rightBottomCorner.getY();
        double rightBottomZ = rightBottomCorner.getZ();

        double rightTopX = rightTopCorner.getX();
        double rightTopY = rightTopCorner.getY();
        double rightTopZ = rightTopCorner.getZ();

        boolean widthX = leftBottomX != rightBottomX;
        boolean widthY = leftBottomY != rightBottomY;
        boolean widthZ = leftBottomZ != rightBottomZ;

        boolean heightX = leftBottomX != leftTopX;
        boolean heightY = leftBottomY != leftTopY;
        boolean heightZ = leftBottomZ != leftTopZ;

        if (widthX && heightY) {
            if (leftTopY > leftBottomY) {
                leftTopY += 1.0;
                rightTopY += 1.0;
            } else {
                leftBottomY += 1.0;
                rightBottomY += 1.0;
            }

            if (rightBottomX > leftBottomX) {
                rightBottomX += 1.0;
                rightTopX += 1.0;
            } else {
                leftBottomX += 1.0;
                leftTopX += 1.0;
            }
        } else if (widthZ && heightY) {
            if (leftTopY > leftBottomY) {
                leftTopY += 1.0;
                rightTopY += 1.0;
            } else {
                leftBottomY += 1.0;
                rightBottomY += 1.0;
            }

            if (rightBottomZ > leftBottomZ) {
                rightBottomZ += 1.0;
                rightTopZ += 1.0;
            } else {
                leftBottomZ += 1.0;
                leftTopZ += 1.0;
            }
        } else if (widthX && heightZ) {
            if (leftTopZ > leftBottomZ) {
                leftTopZ += 1.0;
                rightTopZ += 1.0;
            } else {
                leftBottomZ += 1.0;
                rightBottomZ += 1.0;
            }

            if (rightBottomX > leftBottomX) {
                rightBottomX += 1.0;
                rightTopX += 1.0;
            } else {
                leftBottomX += 1.0;
                leftTopX += 1.0;
            }
        } else if (widthY && heightX) {
            if (leftTopX > leftBottomX) {
                leftTopX += 1.0;
                rightTopX += 1.0;
            } else {
                leftBottomX += 1.0;
                rightBottomX += 1.0;
            }

            if (rightBottomY > leftBottomY) {
                rightBottomY += 1.0;
                rightTopY += 1.0;
            } else {
                leftBottomY += 1.0;
                leftTopY += 1.0;
            }
        } else if (widthY && heightZ) {
            if (leftTopZ > leftBottomZ) {
                leftTopZ += 1.0;
                rightTopZ += 1.0;
            } else {
                leftBottomZ += 1.0;
                rightBottomZ += 1.0;
            }

            if (rightBottomY > leftBottomY) {
                rightBottomY += 1.0;
                rightTopY += 1.0;
            } else {
                leftBottomY += 1.0;
                leftTopY += 1.0;
            }
        } else if (widthZ && heightX) {
            if (leftTopX > leftBottomX) {
                leftTopX += 1.0;
                rightTopX += 1.0;
            } else {
                leftBottomX += 1.0;
                rightBottomX += 1.0;
            }

            if (rightBottomZ > leftBottomZ) {
                rightBottomZ += 1.0;
                rightTopZ += 1.0;
            } else {
                leftBottomZ += 1.0;
                leftTopZ += 1.0;
            }
        }

        double leftCornerDistX = leftTopX - leftBottomX;
        double leftCornerDistY = leftTopY - leftBottomY;
        double leftCornerDistZ = leftTopZ - leftBottomZ;

        double rightCornerDistX = rightBottomX - leftBottomX;
        double rightCornerDistY = rightBottomY - leftBottomY;
        double rightCornerDistZ = rightBottomZ - leftBottomZ;

        double normalX = leftCornerDistY * rightCornerDistZ - leftCornerDistZ * rightCornerDistY;
        double normalY = leftCornerDistZ * rightCornerDistX - leftCornerDistX * rightCornerDistZ;
        double normalZ = leftCornerDistX * rightCornerDistY - leftCornerDistY * rightCornerDistX;

        double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);

        normalX /= length;
        normalY /= length;
        normalZ /= length;

        double centerX = (leftBottomX + rightTopX) / 2.0;
        double centerY = (leftBottomY + rightTopY) / 2.0;
        double centerZ = (leftBottomZ + rightTopZ) / 2.0;

        double toCameraX = camera.x - centerX;
        double toCameraY = camera.y - centerY;
        double toCameraZ = camera.z - centerZ;

        double dot = normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ;

        boolean isFrontFacing = dot > 0;

        if (dot < 0) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        if (isFrontFacing) {
            leftBottomX += normalX;
            leftBottomY += normalY;
            leftBottomZ += normalZ;

            leftTopX += normalX;
            leftTopY += normalY;
            leftTopZ += normalZ;

            rightBottomX += normalX;
            rightBottomY += normalY;
            rightBottomZ += normalZ;

            rightTopX += normalX;
            rightTopY += normalY;
            rightTopZ += normalZ;
        }

        double againstZFighting = 0.05;

        leftBottomX += normalX * againstZFighting;
        leftBottomY += normalY * againstZFighting;
        leftBottomZ += normalZ * againstZFighting;

        leftTopX += normalX * againstZFighting;
        leftTopY += normalY * againstZFighting;
        leftTopZ += normalZ * againstZFighting;

        rightBottomX += normalX * againstZFighting;
        rightBottomY += normalY * againstZFighting;
        rightBottomZ += normalZ * againstZFighting;

        rightTopX += normalX * againstZFighting;
        rightTopY += normalY * againstZFighting;
        rightTopZ += normalZ * againstZFighting;

        boolean isHorizontal = Math.abs(normalY) > 0.99;
        int rot = 0;

        if (isHorizontal) {
            double side1 = Math.sqrt(leftCornerDistX * leftCornerDistX + leftCornerDistY * leftCornerDistY + leftCornerDistZ * leftCornerDistZ);
            double side2 = Math.sqrt(rightCornerDistX * rightCornerDistX + rightCornerDistY * rightCornerDistY + rightCornerDistZ * rightCornerDistZ);
            boolean isSquare = Math.abs(side1 - side2) < 0.1;

            boolean videoPortrait = width < height;

            double midBottomX = (leftBottomX + rightBottomX) / 2.0;
            double midBottomZ = (leftBottomZ + rightBottomZ) / 2.0;
            double midLeftX = (leftBottomX + leftTopX) / 2.0;
            double midLeftZ = (leftBottomZ + leftTopZ) / 2.0;
            double midTopX = (leftTopX + rightTopX) / 2.0;
            double midTopZ = (leftTopZ + rightTopZ) / 2.0;
            double midRightX = (rightBottomX + rightTopX) / 2.0;
            double midRightZ = (rightBottomZ + rightTopZ) / 2.0;

            double dirX = toCameraX;
            double dirZ = toCameraZ;
            double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLen > 0.001) {
                dirX /= dirLen;
                dirZ /= dirLen;
            } else {
                dirX = 0; dirZ = 0;
            }

            double[] dotToSide = new double[4];

            double vx = midBottomX - centerX;
            double vz = midBottomZ - centerZ;
            double vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[0] = dirX * vx + dirZ * vz;
            } else dotToSide[0] = -1;

            vx = midLeftX - centerX;
            vz = midLeftZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[1] = dirX * vx + dirZ * vz;
            } else dotToSide[1] = -1;

            vx = midTopX - centerX;
            vz = midTopZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[2] = dirX * vx + dirZ * vz;
            } else dotToSide[2] = -1;

            vx = midRightX - centerX;
            vz = midRightZ - centerZ;
            vlen = Math.sqrt(vx * vx + vz * vz);
            if (vlen > 0.001) {
                vx /= vlen; vz /= vlen;
                dotToSide[3] = dirX * vx + dirZ * vz;
            } else dotToSide[3] = -1;

            int[] allowed;
            if (isSquare) {
                allowed = new int[]{0, 1, 2, 3};
            } else {
                allowed = ((side1 < side2) == videoPortrait) ? new int[]{1, 3} : new int[]{0, 2};
            }

            int bestEdge = allowed[0];
            double maxD = dotToSide[allowed[0]];
            for (int e : allowed) {
                if (dotToSide[e] > maxD) {
                    maxD = dotToSide[e];
                    bestEdge = e;
                }
            }
            rot = (4 - bestEdge) % 4;
        }

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        double[][] uv = new double[4][2];
        if (isFrontFacing) {
            uv[0][0] = 1; uv[0][1] = 1;
            uv[1][0] = 1; uv[1][1] = 0;
            uv[2][0] = 0; uv[2][1] = 0;
            uv[3][0] = 0;
        } else {
            uv[0][0] = 0; uv[0][1] = 1;
            uv[1][0] = 0; uv[1][1] = 0;
            uv[2][0] = 1; uv[2][1] = 0;
            uv[3][0] = 1;
        }
        uv[3][1] = 1;

        if (isHorizontal) {
            for (int r = 0; r < rot; r++) {
                for (int i = 0; i < 4; i++) {
                    double oldU = uv[i][0];
                    double oldV = uv[i][1];
                    uv[i][0] = oldV;
                    uv[i][1] = 1.0 - oldU;
                }
            }
        }

        if (isFrontFacing) {
            vertexConsumer.vertex(pose, (float) leftBottomX, (float) leftBottomY, (float) leftBottomZ).color(255, 255, 255, 255).uv((float) uv[0][0], (float) uv[0][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) leftTopX, (float) leftTopY, (float) leftTopZ).color(255, 255, 255, 255).uv((float) uv[1][0], (float) uv[1][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightTopX, (float) rightTopY, (float) rightTopZ).color(255, 255, 255, 255).uv((float) uv[2][0], (float) uv[2][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightBottomX, (float) rightBottomY, (float) rightBottomZ).color(255, 255, 255, 255).uv((float) uv[3][0], (float) uv[3][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        } else {
            vertexConsumer.vertex(pose, (float) leftBottomX, (float) leftBottomY, (float) leftBottomZ).color(255, 255, 255, 255).uv((float) uv[0][0], (float) uv[0][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) leftTopX, (float) leftTopY, (float) leftTopZ).color(255, 255, 255, 255).uv((float) uv[1][0], (float) uv[1][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightTopX, (float) rightTopY, (float) rightTopZ).color(255, 255, 255, 255).uv((float) uv[2][0], (float) uv[2][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
            vertexConsumer.vertex(pose, (float) rightBottomX, (float) rightBottomY, (float) rightBottomZ).color(255, 255, 255, 255).uv((float) uv[3][0], (float) uv[3][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        }
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

        if (this.localSoundEngine != null) {
            this.localSoundEngine.off.run();
            this.localSoundEngine = null;
        }
    }

    @Override
    public @Nullable AudioProducer initAudio(double seekTo) {
        if (this.screen.hasLocalSound()) {
            this.localSoundEngine = new LocalSoundEngine(this.screen);
            this.localSoundEngine.on.accept(0D);
            return null;
        } else {
            return super.initAudio(seekTo);
        }
    }

    @Override
    public Runnable pauseAudio() {
        if (this.localSoundEngine != null) {
            return this.localSoundEngine.off;
        } else {
            return super.pauseAudio();
        }
    }

    @Override
    public Runnable resumeAudio() {
        LocalSoundEngine localSoundEngine = this.localSoundEngine;
        if (localSoundEngine != null) {
            return () -> {
                var life = this.life();
                if (life != null) localSoundEngine.on.accept((double) life.nanoTimeFromSetup() / 1_000_000_000D);
            };
        } else {
            return super.resumeAudio();
        }
    }
}
