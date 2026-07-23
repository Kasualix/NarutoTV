package me.kall.narutotv.impl.world.util;


import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class WorldMath {
    public static final double SURFACE_OFFSET = 0.01D;

    public record Bounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {}

    @Contract("_ -> new")
    public static @NotNull Bounds computeBounds(Vec3 @NotNull [] corners) {
        double minX = Math.min(Math.min(corners[0].x, corners[1].x), Math.min(corners[2].x, corners[3].x));
        double maxX = Math.max(Math.max(corners[0].x, corners[1].x), Math.max(corners[2].x, corners[3].x));
        double minY = Math.min(Math.min(corners[0].y, corners[1].y), Math.min(corners[2].y, corners[3].y));
        double maxY = Math.max(Math.max(corners[0].y, corners[1].y), Math.max(corners[2].y, corners[3].y));
        double minZ = Math.min(Math.min(corners[0].z, corners[1].z), Math.min(corners[2].z, corners[3].z));
        double maxZ = Math.max(Math.max(corners[0].z, corners[1].z), Math.max(corners[2].z, corners[3].z));

        return new Bounds((int) minX, (int) maxX + 1, (int) minY, (int) maxY + 1, (int) minZ, (int) maxZ + 1);
    }

    @Contract("_ -> new")
    public static @NotNull Vec3 computeNormal(Vec3 @NotNull [] corners) {
        Vec3 bottomEdge = corners[1].subtract(corners[0]);
        Vec3 leftEdge = corners[2].subtract(corners[0]);
        Vec3 raw = bottomEdge.cross(leftEdge);
        double length = raw.length();
        return length > 1.0E-6D ? raw.scale(1.0D / length) : new Vec3(0D, 1D, 0D);
    }

    @Contract("_ -> new")
    public static @NotNull Vec3 computeCenter(Vec3 @NotNull [] corners) {
        return new Vec3(
                (corners[0].x + corners[1].x + corners[2].x + corners[3].x) / 4.0,
                (corners[0].y + corners[1].y + corners[2].y + corners[3].y) / 4.0,
                (corners[0].z + corners[1].z + corners[2].z + corners[3].z) / 4.0
        );
    }

    public static boolean isBack(@NotNull Vec3 camera, @NotNull Vec3 center, @NotNull Vec3 normal) {
        return camera.subtract(center).dot(normal) < 0;
    }

    public static int getRotationSteps(int width, int height, double deg) {
        boolean square = Math.abs(width - height) < 1;
        if (square) {
            int steps = (int) Math.round(deg / 90.0);
            return ((steps % 4) + 4) % 4;
        }
        return (deg > 90 || deg < -90) ? 2 : 0;
    }

    public record QuadData(
            double x1, double y1, double z1, float u1, float v1,
            double x2, double y2, double z2, float u2, float v2,
            double x3, double y3, double z3, float u3, float v3,
            double x4, double y4, double z4, float u4, float v4,
            float normalX, float normalY, float normalZ,
            boolean isBack
    ) {
        @Contract(" -> new")
        public float @NotNull [] toVertexArray() {
            return new float[]{
                    (float) x1, (float) y1, (float) z1, u1, v1,
                    (float) x2, (float) y2, (float) z2, u2, v2,
                    (float) x3, (float) y3, (float) z3, u3, v3,
                    (float) x4, (float) y4, (float) z4, u4, v4
            };
        }
    }

    @Contract("_, _, _, _, _, _, _ -> new")
    public static @NotNull QuadData computeQuad(Vec3 @NotNull [] corners, @NotNull Bounds bounds, @NotNull Vec3 normal, @NotNull Vec3 center, @NotNull Vec3 camera, int width, int height) {
        boolean isBack = isBack(camera, center, normal);

        float normalX = (float) normal.x;
        float normalY = (float) normal.y;
        float normalZ = (float) normal.z;
        if (isBack) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }

        int minX = bounds.minX(), maxX = bounds.maxX();
        int minY = bounds.minY(), maxY = bounds.maxY();
        int minZ = bounds.minZ(), maxZ = bounds.maxZ();

        double x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;
        float u1, v1, u2, v2, u3, v3, u4, v4;

        if (Math.abs(normalX) > 0.5) {
            double surfaceX = (normalX > 0 ? maxX : minX) + normalX * SURFACE_OFFSET;
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
            double surfaceY = (normalY > 0 ? maxY : minY) + normalY * SURFACE_OFFSET;
            y1 = y2 = y3 = y4 = surfaceY;

            Vec3 bottomEdge = corners[1].subtract(corners[0]);
            Vec3 leftEdge = corners[2].subtract(corners[0]);

            if (Math.abs(bottomEdge.x) >= Math.abs(bottomEdge.z)) {
                x1 = x4 = minX;  x2 = x3 = maxX;
                z1 = z2 = minZ;  z3 = z4 = maxZ;

                if (corners[0].z == minZ) {
                    u1 = 0f; v1 = 1f;  u2 = 1f; v2 = 1f;
                    u3 = 1f; v3 = 0f;  u4 = 0f; v4 = 0f;
                } else {
                    u1 = 0f; v1 = 0f;  u2 = 1f; v2 = 0f;
                    u3 = 1f; v3 = 1f;  u4 = 0f; v4 = 1f;
                }
            } else {
                x1 = minX; z1 = minZ;
                x2 = maxX; z2 = minZ;
                x3 = maxX; z3 = maxZ;
                x4 = minX; z4 = maxZ;

                if (corners[0].x == minX) {
                    u1 = 0f; v1 = 1f;  u2 = 0f; v2 = 0f;
                    u3 = 1f; v3 = 0f;  u4 = 1f; v4 = 1f;
                } else {
                    u1 = 0f; v1 = 0f;  u2 = 0f; v2 = 1f;
                    u3 = 1f; v3 = 1f;  u4 = 1f; v4 = 0f;
                }
            }

            Vec3 toPlayer = new Vec3(camera.x - center.x, 0, camera.z - center.z);
            if (toPlayer.lengthSqr() > 1e-6) {
                toPlayer = toPlayer.normalize();
                Vec3 upDir = new Vec3(leftEdge.x, 0, leftEdge.z).normalize();
                double angle = Math.atan2(upDir.cross(toPlayer).y, upDir.dot(toPlayer));
                double deg = Math.toDegrees(angle);
                int rotationSteps = getRotationSteps(width, height, deg);

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
            double surfaceZ = (normalZ > 0 ? maxZ : minZ) + normalZ * SURFACE_OFFSET;
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

        return new QuadData(
                x1, y1, z1, u1, v1,
                x2, y2, z2, u2, v2,
                x3, y3, z3, u3, v3,
                x4, y4, z4, u4, v4,
                normalX, normalY, normalZ,
                isBack
        );
    }
}
