package me.kall.narutotv.impl.world.util;

import me.kall.narutotv.impl.world.data.Wall;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class NarutoMath {
    public record Coords(
            double normalX, double normalY, double normalZ,
            double bottomFromX, double bottomFromY, double bottomFromZ, float u0, float v0,
            double bottomToX, double bottomToY, double bottomToZ, float u1, float v1,
            double topToX, double topToY, double topToZ, float u2, float v2,
            double topFromX, double topFromY, double topFromZ, float u3, float v3
    ) {}

    public static float computeU(double pointX, double pointY, double pointZ, double centerX, double centerY, double centerZ, double rightX, double rightY, double rightZ) {
        return ((pointX - centerX) * rightX + (pointY - centerY) * rightY + (pointZ - centerZ) * rightZ) >= 0 ? 1.0F : 0.0F;
    }

    public static float computeV(double pointX, double pointY, double pointZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        return ((pointX - centerX) * upX + (pointY - centerY) * upY + (pointZ - centerZ) * upZ) >= 0 ? 0.0F : 1.0F;
    }

    public static @NotNull NarutoMath.Coords computeCoords(@NotNull Wall wall, @NotNull Camera camera) {
        Vec3 camVec = camera.getPosition();
        Vector3f upVec = camera.getUpVector();

        double camX = camVec.x, camY = camVec.y, camZ = camVec.z;

        Wall.Data data = wall.getData();

        int minX = data.minX, minY = data.minY, minZ = data.minZ;
        int maxX = data.maxX, maxY = data.maxY, maxZ = data.maxZ;

        double[] corners = data.getCorners(camX, camY, camZ, upVec);

        double bottomFromX = corners[0], bottomFromY = corners[1], bottomFromZ = corners[2];
        double bottomToX = corners[3], bottomToY = corners[4], bottomToZ = corners[5];
        double topFromX = corners[6], topFromY = corners[7], topFromZ = corners[8];
        double topToX = corners[9], topToY = corners[10], topToZ = corners[11];

        double normalX = 0, normalY = 0, normalZ = 0;
        switch (data.axisThickness) {
            case X -> normalX = Math.abs(camX - minX) < Math.abs(camX - maxX) ? -1 : 1;
            case Y -> normalY = Math.abs(camY - minY) < Math.abs(camY - maxY) ? -1 : 1;
            case Z -> normalZ = Math.abs(camZ - minZ) < Math.abs(camZ - maxZ) ? -1 : 1;
        }

        double uDirX = bottomToX - bottomFromX, uDirY = bottomToY - bottomFromY, uDirZ = bottomToZ - bottomFromZ;
        double vDirX = topFromX - bottomFromX, vDirY = topFromY - bottomFromY, vDirZ = topFromZ - bottomFromZ;

        double uLength = Math.sqrt(uDirX * uDirX + uDirY * uDirY + uDirZ * uDirZ);
        double vLength = Math.sqrt(vDirX * vDirX + vDirY * vDirY + vDirZ * vDirZ);

        boolean isSquare = Math.abs(uLength - vLength) < 1e-6;

        if (uLength > 1e-9) { uDirX /= uLength; uDirY /= uLength; uDirZ /= uLength; }
        if (vLength > 1e-9) { vDirX /= vLength; vDirY /= vLength; vDirZ /= vLength; }

        double camUpX = upVec.x(), camUpY = upVec.y(), camUpZ = upVec.z();
        double dotUpN = camUpX * normalX + camUpY * normalY + camUpZ * normalZ;
        double upProjX = camUpX - normalX * dotUpN;
        double upProjY = camUpY - normalY * dotUpN;
        double upProjZ = camUpZ - normalZ * dotUpN;
        double upProjLenSq = upProjX * upProjX + upProjY * upProjY + upProjZ * upProjZ;

        if (upProjLenSq < 1e-4) {
            Vector3f lookVec = camera.getLookVector();
            double lookX = lookVec.x(), lookY = lookVec.y(), lookZ = lookVec.z();
            double dotLookN = lookX * normalX + lookY * normalY + lookZ * normalZ;
            upProjX = lookX - normalX * dotLookN;
            upProjY = lookY - normalY * dotLookN;
            upProjZ = lookZ - normalZ * dotLookN;
            upProjLenSq = upProjX * upProjX + upProjY * upProjY + upProjZ * upProjZ;

            if (upProjLenSq < 1e-4) {
                upProjX = uDirX; upProjY = uDirY; upProjZ = uDirZ;
                upProjLenSq = 1.0;
            }
        }

        double invUpLen = 1.0 / Math.sqrt(upProjLenSq);
        upProjX *= invUpLen; upProjY *= invUpLen; upProjZ *= invUpLen;

        double upDotU = upProjX * uDirX + upProjY * uDirY + upProjZ * uDirZ;
        double upDotV = upProjX * vDirX + upProjY * vDirY + upProjZ * vDirZ;

        double upX, upY, upZ, preRightX, preRightY, preRightZ;
        if (isSquare) {
            if (Math.abs(upDotU) >= Math.abs(upDotV)) {
                double sign = upDotU >= 0 ? 1.0 : -1.0;
                upX = uDirX * sign; upY = uDirY * sign; upZ = uDirZ * sign;
                preRightX = vDirX; preRightY = vDirY; preRightZ = vDirZ;
            } else {
                double sign = upDotV >= 0 ? 1.0 : -1.0;
                upX = vDirX * sign; upY = vDirY * sign; upZ = vDirZ * sign;
                preRightX = uDirX; preRightY = uDirY; preRightZ = uDirZ;
            }
        } else {
            double sign = upDotV >= 0 ? 1.0 : -1.0;
            upX = vDirX * sign; upY = vDirY * sign; upZ = vDirZ * sign;
            preRightX = uDirX; preRightY = uDirY; preRightZ = uDirZ;
        }

        double targetX = upY * normalZ - upZ * normalY;
        double targetY = upZ * normalX - upX * normalZ;
        double targetZ = upX * normalY - upY * normalX;

        double alignDot = preRightX * targetX + preRightY * targetY + preRightZ * targetZ;

        double rightX, rightY, rightZ;
        if (alignDot >= 0) {
            rightX = preRightX; rightY = preRightY; rightZ = preRightZ;
        } else {
            rightX = -preRightX; rightY = -preRightY; rightZ = -preRightZ;
        }

        double centerX = data.centerX, centerY = data.centerY, centerZ = data.centerZ;

        float u0 = computeU(bottomFromX, bottomFromY, bottomFromZ, centerX, centerY, centerZ, rightX, rightY, rightZ);
        float v0 = computeV(bottomFromX, bottomFromY, bottomFromZ, centerX, centerY, centerZ, upX, upY, upZ);

        float u1 = computeU(bottomToX, bottomToY, bottomToZ, centerX, centerY, centerZ, rightX, rightY, rightZ);
        float v1 = computeV(bottomToX, bottomToY, bottomToZ, centerX, centerY, centerZ, upX, upY, upZ);

        float u2 = computeU(topToX, topToY, topToZ, centerX, centerY, centerZ, rightX, rightY, rightZ);
        float v2 = computeV(topToX, topToY, topToZ, centerX, centerY, centerZ, upX, upY, upZ);

        float u3 = computeU(topFromX, topFromY, topFromZ, centerX, centerY, centerZ, rightX, rightY, rightZ);
        float v3 = computeV(topFromX, topFromY, topFromZ, centerX, centerY, centerZ, upX, upY, upZ);

        return new Coords(
                normalX, normalY, normalZ,
                bottomFromX, bottomFromY, bottomFromZ, u0, v0,
                bottomToX, bottomToY, bottomToZ, u1, v1,
                topToX, topToY, topToZ, u2, v2,
                topFromX, topFromY, topFromZ, u3, v3
        );
    }
}