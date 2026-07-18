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

    private double leftBottomX, leftBottomY, leftBottomZ;
    private double leftTopX, leftTopY, leftTopZ;
    private double rightBottomX, rightBottomY, rightBottomZ;
    private double rightTopX, rightTopY, rightTopZ;

    private double normalX, normalY, normalZ;
    private double centerX, centerY, centerZ;

    private boolean horizontal;
    private double side1, side2;
    private double sideBottomX, sideBottomZ;
    private double sideLeftX, sideLeftZ;
    private double sideTopX, sideTopZ;
    private double sideRightX, sideRightZ;

    public WorldImageRenderer(BlockScreen screen) {
        this.screen = screen;
        this.cacheScreenGeometry(screen);
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

        MediaArgs mediaArgs = this.mediaArgs();
        assert mediaArgs != null;

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(nextFrame));

        double normalX = this.normalX;
        double normalY = this.normalY;
        double normalZ = this.normalZ;

        double toCameraX = camera.x - this.centerX;
        double toCameraY = camera.y - this.centerY;
        double toCameraZ = camera.z - this.centerZ;
        double dot = normalX * toCameraX + normalY * toCameraY + normalZ * toCameraZ;
        boolean isFrontFacing = dot > 0;

        if (!isFrontFacing) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        double leftBottomX = this.leftBottomX;
        double leftBottomY = this.leftBottomY;
        double leftBottomZ = this.leftBottomZ;
        double leftTopX = this.leftTopX;
        double leftTopY = this.leftTopY;
        double leftTopZ = this.leftTopZ;
        double rightBottomX = this.rightBottomX;
        double rightBottomY = this.rightBottomY;
        double rightBottomZ = this.rightBottomZ;
        double rightTopX = this.rightTopX;
        double rightTopY = this.rightTopY;
        double rightTopZ = this.rightTopZ;

        if (isFrontFacing) {
            leftBottomX += normalX; leftBottomY += normalY; leftBottomZ += normalZ;
            leftTopX += normalX; leftTopY += normalY; leftTopZ += normalZ;
            rightBottomX += normalX; rightBottomY += normalY; rightBottomZ += normalZ;
            rightTopX += normalX; rightTopY += normalY; rightTopZ += normalZ;
        }

        double againstZFighting = 0.05;
        leftBottomX += normalX * againstZFighting; leftBottomY += normalY * againstZFighting; leftBottomZ += normalZ * againstZFighting;
        leftTopX += normalX * againstZFighting; leftTopY += normalY * againstZFighting; leftTopZ += normalZ * againstZFighting;
        rightBottomX += normalX * againstZFighting; rightBottomY += normalY * againstZFighting; rightBottomZ += normalZ * againstZFighting;
        rightTopX += normalX * againstZFighting; rightTopY += normalY * againstZFighting; rightTopZ += normalZ * againstZFighting;

        int rot = this.horizontal ? this.getHorizontalRotation(mediaArgs.width(), mediaArgs.height(), toCameraX, toCameraZ) : 0;

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

        if (this.horizontal) {
            for (int r = 0; r < rot; r++) {
                for (int i = 0; i < 4; i++) {
                    double oldU = uv[i][0];
                    double oldV = uv[i][1];
                    uv[i][0] = oldV;
                    uv[i][1] = 1.0 - oldU;
                }
            }
        }

        vertexConsumer.vertex(pose, (float) leftBottomX, (float) leftBottomY, (float) leftBottomZ).color(255, 255, 255, 255).uv((float) uv[0][0], (float) uv[0][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        vertexConsumer.vertex(pose, (float) leftTopX, (float) leftTopY, (float) leftTopZ).color(255, 255, 255, 255).uv((float) uv[1][0], (float) uv[1][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        vertexConsumer.vertex(pose, (float) rightTopX, (float) rightTopY, (float) rightTopZ).color(255, 255, 255, 255).uv((float) uv[2][0], (float) uv[2][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
        vertexConsumer.vertex(pose, (float) rightBottomX, (float) rightBottomY, (float) rightBottomZ).color(255, 255, 255, 255).uv((float) uv[3][0], (float) uv[3][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normalMat, (float) normalX, (float) normalY, (float) normalZ).endVertex();
    }

    private void cacheScreenGeometry(BlockScreen blockScreen) {
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
            if (leftTopY > leftBottomY) { leftTopY += 1.0; rightTopY += 1.0; } else { leftBottomY += 1.0; rightBottomY += 1.0; }
            if (rightBottomX > leftBottomX) { rightBottomX += 1.0; rightTopX += 1.0; } else { leftBottomX += 1.0; leftTopX += 1.0; }
        } else if (widthZ && heightY) {
            if (leftTopY > leftBottomY) { leftTopY += 1.0; rightTopY += 1.0; } else { leftBottomY += 1.0; rightBottomY += 1.0; }
            if (rightBottomZ > leftBottomZ) { rightBottomZ += 1.0; rightTopZ += 1.0; } else { leftBottomZ += 1.0; leftTopZ += 1.0; }
        } else if (widthX && heightZ) {
            if (leftTopZ > leftBottomZ) { leftTopZ += 1.0; rightTopZ += 1.0; } else { leftBottomZ += 1.0; rightBottomZ += 1.0; }
            if (rightBottomX > leftBottomX) { rightBottomX += 1.0; rightTopX += 1.0; } else { leftBottomX += 1.0; leftTopX += 1.0; }
        } else if (widthY && heightX) {
            if (leftTopX > leftBottomX) { leftTopX += 1.0; rightTopX += 1.0; } else { leftBottomX += 1.0; rightBottomX += 1.0; }
            if (rightBottomY > leftBottomY) { rightBottomY += 1.0; rightTopY += 1.0; } else { leftBottomY += 1.0; leftTopY += 1.0; }
        } else if (widthY && heightZ) {
            if (leftTopZ > leftBottomZ) { leftTopZ += 1.0; rightTopZ += 1.0; } else { leftBottomZ += 1.0; rightBottomZ += 1.0; }
            if (rightBottomY > leftBottomY) { rightBottomY += 1.0; rightTopY += 1.0; } else { leftBottomY += 1.0; leftTopY += 1.0; }
        } else if (widthZ && heightX) {
            if (leftTopX > leftBottomX) { leftTopX += 1.0; rightTopX += 1.0; } else { leftBottomX += 1.0; rightBottomX += 1.0; }
            if (rightBottomZ > leftBottomZ) { rightBottomZ += 1.0; rightTopZ += 1.0; } else { leftBottomZ += 1.0; leftTopZ += 1.0; }
        }

        this.leftBottomX = leftBottomX; this.leftBottomY = leftBottomY; this.leftBottomZ = leftBottomZ;
        this.leftTopX = leftTopX; this.leftTopY = leftTopY; this.leftTopZ = leftTopZ;
        this.rightBottomX = rightBottomX; this.rightBottomY = rightBottomY; this.rightBottomZ = rightBottomZ;
        this.rightTopX = rightTopX; this.rightTopY = rightTopY; this.rightTopZ = rightTopZ;

        double leftDeltaX = leftTopX - leftBottomX;
        double leftDeltaY = leftTopY - leftBottomY;
        double leftDeltaZ = leftTopZ - leftBottomZ;
        double rightDeltaX = rightBottomX - leftBottomX;
        double rightDeltaY = rightBottomY - leftBottomY;
        double rightDeltaZ = rightBottomZ - leftBottomZ;

        this.side1 = Math.sqrt(leftDeltaX * leftDeltaX + leftDeltaY * leftDeltaY + leftDeltaZ * leftDeltaZ);
        this.side2 = Math.sqrt(rightDeltaX * rightDeltaX + rightDeltaY * rightDeltaY + rightDeltaZ * rightDeltaZ);

        double normalX = leftDeltaY * rightDeltaZ - leftDeltaZ * rightDeltaY;
        double normalY = leftDeltaZ * rightDeltaX - leftDeltaX * rightDeltaZ;
        double normalZ = leftDeltaX * rightDeltaY - leftDeltaY * rightDeltaX;
        double length = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        this.normalX = normalX / length;
        this.normalY = normalY / length;
        this.normalZ = normalZ / length;

        this.centerX = (leftBottomX + rightTopX) / 2.0;
        this.centerY = (leftBottomY + rightTopY) / 2.0;
        this.centerZ = (leftBottomZ + rightTopZ) / 2.0;
        this.horizontal = Math.abs(this.normalY) > 0.99;
        if (this.horizontal) this.cacheHorizontalSideVectors();
    }

    private void cacheHorizontalSideVectors() {
        this.sideBottomX = normalizedX((this.leftBottomX + this.rightBottomX) / 2.0 - this.centerX, (this.leftBottomZ + this.rightBottomZ) / 2.0 - this.centerZ);
        this.sideBottomZ = normalizedZ((this.leftBottomX + this.rightBottomX) / 2.0 - this.centerX, (this.leftBottomZ + this.rightBottomZ) / 2.0 - this.centerZ);
        this.sideLeftX = normalizedX((this.leftBottomX + this.leftTopX) / 2.0 - this.centerX, (this.leftBottomZ + this.leftTopZ) / 2.0 - this.centerZ);
        this.sideLeftZ = normalizedZ((this.leftBottomX + this.leftTopX) / 2.0 - this.centerX, (this.leftBottomZ + this.leftTopZ) / 2.0 - this.centerZ);
        this.sideTopX = normalizedX((this.leftTopX + this.rightTopX) / 2.0 - this.centerX, (this.leftTopZ + this.rightTopZ) / 2.0 - this.centerZ);
        this.sideTopZ = normalizedZ((this.leftTopX + this.rightTopX) / 2.0 - this.centerX, (this.leftTopZ + this.rightTopZ) / 2.0 - this.centerZ);
        this.sideRightX = normalizedX((this.rightBottomX + this.rightTopX) / 2.0 - this.centerX, (this.rightBottomZ + this.rightTopZ) / 2.0 - this.centerZ);
        this.sideRightZ = normalizedZ((this.rightBottomX + this.rightTopX) / 2.0 - this.centerX, (this.rightBottomZ + this.rightTopZ) / 2.0 - this.centerZ);
    }

    private int getHorizontalRotation(int width, int height, double toCameraX, double toCameraZ) {
        double dirLen = Math.sqrt(toCameraX * toCameraX + toCameraZ * toCameraZ);
        double dirX = dirLen > 0.001 ? toCameraX / dirLen : 0;
        double dirZ = dirLen > 0.001 ? toCameraZ / dirLen : 0;
        double[] dotToSide = new double[]{
                dotOrMinusOne(dirX, dirZ, this.sideBottomX, this.sideBottomZ),
                dotOrMinusOne(dirX, dirZ, this.sideLeftX, this.sideLeftZ),
                dotOrMinusOne(dirX, dirZ, this.sideTopX, this.sideTopZ),
                dotOrMinusOne(dirX, dirZ, this.sideRightX, this.sideRightZ)
        };
        int[] allowed = Math.abs(this.side1 - this.side2) < 0.1 ? new int[]{0, 1, 2, 3} : ((this.side1 < this.side2) == (width < height) ? new int[]{1, 3} : new int[]{0, 2});
        int bestEdge = allowed[0];
        double maxD = dotToSide[bestEdge];
        for (int edge : allowed) {
            if (dotToSide[edge] > maxD) {
                maxD = dotToSide[edge];
                bestEdge = edge;
            }
        }
        return (4 - bestEdge) % 4;
    }

    private static double normalizedX(double x, double z) {
        double length = Math.sqrt(x * x + z * z);
        return length > 0.001 ? x / length : 0;
    }

    private static double normalizedZ(double x, double z) {
        double length = Math.sqrt(x * x + z * z);
        return length > 0.001 ? z / length : 0;
    }

    private static double dotOrMinusOne(double dirX, double dirZ, double sideX, double sideZ) {
        return sideX == 0 && sideZ == 0 ? -1 : dirX * sideX + dirZ * sideZ;
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
