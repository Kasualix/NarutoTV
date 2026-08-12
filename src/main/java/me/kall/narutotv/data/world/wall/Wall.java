package me.kall.narutotv.data.world.wall;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.NarutoTV;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

public final class Wall {
    public static final ResourceLocation NO_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "no");

    public final BlockPos leftBottom, leftTop, rightBottom, rightTop;
    public final ResourceLocation dimension;

    public final double centerX, centerY, centerZ;
    public final int minX, maxX, minY, maxY, minZ, maxZ;
    public final int widthX, widthY, widthZ;
    public final Direction.Axis axisThickness;
    public final String id;

    private final LongSet areaInvolved, borderInvolved;

    private final int hash;

    public ResourceLocation localSound = NO_LOCAL_SOUND;
    public float volume = 1.0F;

    public String video = "", audio = "";

    public boolean light = false;

    public Wall(long @NotNull [] corners, ResourceLocation dimension, ResourceLocation localSound, String video, String audio, float volume, boolean light) {
        this(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
        this.localSound = localSound;
        this.video = video;
        this.audio = audio;
        this.volume = volume;
        this.light = light;
    }

    public Wall(@NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2, @NotNull BlockPos topCorner1, @NotNull BlockPos topCorner2, ResourceLocation dimension) {
        this.dimension = dimension;

        BlockPos topFor1, topFor2;
        if (((long)topCorner1.getX() - bottomCorner1.getX()) * ((long)bottomCorner2.getX() - bottomCorner1.getX()) + ((long)topCorner1.getY() - bottomCorner1.getY()) * ((long)bottomCorner2.getY() - bottomCorner1.getY()) + ((long)topCorner1.getZ() - bottomCorner1.getZ()) * ((long)bottomCorner2.getZ() - bottomCorner1.getZ()) == 0) {
            topFor1 = topCorner1;
            topFor2 = topCorner2;
        } else {
            topFor1 = topCorner2;
            topFor2 = topCorner1;
        }

        this.leftBottom = bottomCorner1;
        this.leftTop = topFor1;
        this.rightBottom = bottomCorner2;
        this.rightTop = topFor2;

        this.hash = Objects.hash(this.leftBottom, this.leftTop, this.rightBottom, this.rightTop, this.dimension);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        BlockPos[] positions = {this.leftBottom, this.rightBottom, this.leftTop, this.rightTop};

        for (BlockPos pos : positions) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }

        this.minX = minX;
        this.maxX = maxX + 1;
        this.minY = minY;
        this.maxY = maxY + 1;
        this.minZ = minZ;
        this.maxZ = maxZ + 1;

        this.centerX = (double) (this.minX + this.maxX) / 2.0D;
        this.centerY = (double) (this.minY + this.maxY) / 2.0D;
        this.centerZ = (double) (this.minZ + this.maxZ) / 2.0D;

        this.widthX = this.maxX - this.minX;
        this.widthY = this.maxY - this.minY;
        this.widthZ = this.maxZ - this.minZ;

        this.axisThickness = this.testAxis();

        this.id = String.valueOf(this.leftBottom.getX()) + this.leftBottom.getY() + this.leftBottom.getZ() +
                this.rightBottom.getX() + this.rightBottom.getY() + this.rightBottom.getZ() +
                this.leftTop.getX() + this.leftTop.getY() + this.leftTop.getZ() +
                this.rightTop.getX() + this.rightTop.getY() + this.rightTop.getZ() +
                this.dimension.getNamespace() + "_" + this.dimension.getPath();

        this.areaInvolved = new LongOpenHashSet((this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ));

        for (int x = this.minX; x < this.maxX; x++) {
            for (int y = this.minY; y < this.maxY; y++) {
                for (int z = this.minZ; z < this.maxZ; z++) {
                    this.areaInvolved.add(BlockPos.asLong(x, y, z));
                }
            }
        }

        LongList leftEdge = getLine(this.leftBottom, this.leftTop);
        LongList rightEdge = getLine(this.rightBottom, this.rightTop);
        LongList bottomEdge = getLine(this.leftBottom, this.rightBottom);
        LongList topEdge = getLine(this.leftTop, this.rightTop);

        this.borderInvolved = new LongOpenHashSet(leftEdge.size() + rightEdge.size() + bottomEdge.size() + topEdge.size() - 4);

        this.borderInvolved.addAll(leftEdge);
        this.borderInvolved.addAll(rightEdge);
        this.borderInvolved.addAll(bottomEdge);
        this.borderInvolved.addAll(topEdge);
    }

    private Direction.@NotNull Axis testAxis() {
        Direction.Axis axisThickness = null;

        if (this.widthX == 1) axisThickness = Direction.Axis.X;

        if (this.widthY == 1) {
            if (axisThickness != null) throw new IllegalArgumentException("Expected exactly one axis to have thickness 1, but got widthX=" + this.widthX + ", widthY=" + this.widthY + ", widthZ=" + this.widthZ);
            axisThickness = Direction.Axis.Y;
        }

        if (this.widthZ == 1) {
            if (axisThickness != null) throw new IllegalArgumentException("Expected exactly one axis to have thickness 1, but got widthX=" + this.widthX + ", widthY=" + this.widthY + ", widthZ=" + this.widthZ);
            axisThickness = Direction.Axis.Z;
        }

        if (axisThickness == null) throw new IllegalArgumentException("Failed to find one axis to have thickness 1. WidthX: " + this.widthX + ". WidthY: " + this.widthY + ". WidthZ: " + this.widthZ);
        return axisThickness;
    }

    public boolean hasLocalSound() {
        return !this.localSound.equals(NO_LOCAL_SOUND);
    }

    @Contract(" -> new")
    public long @NotNull [] toLongArray() {
        return new long[]{this.leftBottom.asLong(), this.rightBottom.asLong(), this.leftTop.asLong(), this.rightTop.asLong()};
    }

    public LongSet areaInvolved() {
        return this.areaInvolved;
    }

    public LongSet borderInvolved() {
        return this.borderInvolved;
    }

    public static @NotNull LongList getLine(@NotNull BlockPos from, @NotNull BlockPos to) {
        int diffAxes = 0;
        if (from.getX() != to.getX()) diffAxes++;
        if (from.getY() != to.getY()) diffAxes++;
        if (from.getZ() != to.getZ()) diffAxes++;
        if (diffAxes > 1) throw new IllegalArgumentException("Line must be axis-aligned. from: [" + from.toShortString() + "], to: [" + to.toShortString() + "]");

        LongList line = new LongArrayList(Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))) + 1);

        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());
        int dz = Integer.compare(to.getZ(), from.getZ());

        int x = from.getX(), y = from.getY(), z = from.getZ();
        int toX = to.getX(), toY = to.getY(), toZ = to.getZ();

        line.add(BlockPos.asLong(x, y, z));
        while (x != toX || y != toY || z != toZ) {
            x += dx;
            y += dy;
            z += dz;
            line.add(BlockPos.asLong(x, y, z));
        }
        return line;
    }

    @Contract("_, _, _, _ -> new")
    public double @NotNull [] sortedCorners(double camX, double camY, double camZ, Vector3f upVec) {
        double faceX = 0, faceY = 0, faceZ = 0;
        long maxLen = 0;

        switch (this.axisThickness) {
            case X -> {
                faceX = Math.abs(camX - this.minX) <= Math.abs(camX - this.maxX) ? this.minX : this.maxX;
                maxLen = Math.max(this.widthY, this.widthZ);
            }
            case Y -> {
                faceY = Math.abs(camY - this.minY) <= Math.abs(camY - this.maxY) ? this.minY : this.maxY;
                maxLen = Math.max(this.widthX, this.widthZ);
            }
            case Z -> {
                faceZ = Math.abs(camZ - this.minZ) <= Math.abs(camZ - this.maxZ) ? this.minZ : this.maxZ;
                maxLen = Math.max(this.widthX, this.widthY);
            }
        }

        double bestScore = Double.POSITIVE_INFINITY;
        double bottomFromX = 0, bottomFromY = 0, bottomFromZ = 0, bottomToX = 0, bottomToY = 0, bottomToZ = 0;

        for (int index = 0; index < 4; index++) {
            double fromX = 0, fromY = 0, fromZ = 0, toX = 0, toY = 0, toZ = 0;
            double midX = 0, midY = 0, midZ = 0;
            long len = 0;

            switch (this.axisThickness) {
                case X -> {
                    switch (index) {
                        case 0 -> { fromX = faceX; fromY = this.minY; fromZ = this.minZ; toX = faceX; toY = this.maxY; toZ = this.minZ; midX = faceX; midY = this.centerY; midZ = this.minZ; len = this.widthY; }
                        case 1 -> { fromX = faceX; fromY = this.minY; fromZ = this.maxZ; toX = faceX; toY = this.maxY; toZ = this.maxZ; midX = faceX; midY = this.centerY; midZ = this.maxZ; len = this.widthY; }
                        case 2 -> { fromX = faceX; fromY = this.minY; fromZ = this.minZ; toX = faceX; toY = this.minY; toZ = this.maxZ; midX = faceX; midY = this.minY; midZ = this.centerZ; len = this.widthZ; }
                        case 3 -> { fromX = faceX; fromY = this.maxY; fromZ = this.minZ; toX = faceX; toY = this.maxY; toZ = this.maxZ; midX = faceX; midY = this.maxY; midZ = this.centerZ; len = this.widthZ; }
                    }
                }
                case Y -> {
                    switch (index) {
                        case 0 -> { fromX = this.minX; fromY = faceY; fromZ = this.minZ; toX = this.maxX; toY = faceY; toZ = this.minZ; midX = this.centerX; midY=faceY; midZ = this.minZ; len = this.widthX; }
                        case 1 -> { fromX = this.minX; fromY = faceY; fromZ = this.maxZ; toX = this.maxX; toY = faceY; toZ = this.maxZ; midX = this.centerX; midY=faceY; midZ = this.maxZ; len = this.widthX; }
                        case 2 -> { fromX = this.minX; fromY = faceY; fromZ = this.minZ; toX = this.minX; toY = faceY; toZ = this.maxZ; midX = this.minX; midY=faceY; midZ = this.centerZ; len = this.widthZ; }
                        case 3 -> { fromX = this.maxX; fromY = faceY; fromZ = this.minZ; toX = this.maxX; toY = faceY; toZ = this.maxZ; midX = this.maxX; midY=faceY; midZ = this.centerZ; len = this.widthZ; }
                    }
                }
                case Z -> {
                    switch (index) {
                        case 0 -> { fromX = this.minX; fromY = this.minY; fromZ = faceZ; toX = this.maxX; toY = this.minY; toZ = faceZ; midX = this.centerX; midY = this.minY; midZ = faceZ; len = this.widthX; }
                        case 1 -> { fromX = this.minX; fromY = this.maxY; fromZ = faceZ; toX = this.maxX; toY = this.maxY; toZ = faceZ; midX = this.centerX; midY = this.maxY; midZ = faceZ; len = this.widthX; }
                        case 2 -> { fromX = this.minX; fromY = this.minY; fromZ = faceZ; toX = this.minX; toY = this.maxY; toZ = faceZ; midX = this.minX; midY = this.centerY; midZ = faceZ; len = this.widthY; }
                        case 3 -> { fromX = this.maxX; fromY = this.minY; fromZ = faceZ; toX = this.maxX; toY = this.maxY; toZ = faceZ; midX = this.maxX; midY = this.centerY; midZ = faceZ; len = this.widthY; }
                    }
                }
            }

            if (maxLen > 0 && len < maxLen) continue;

            if (this.axisThickness == Direction.Axis.X && (index == 0 || index == 1) && this.widthZ >= this.widthY) continue;
            if (this.axisThickness == Direction.Axis.Y && (index == 2 || index == 3) && this.widthX >= this.widthZ) continue;
            if (this.axisThickness == Direction.Axis.Z && (index == 2 || index == 3) && this.widthX >= this.widthY) continue;

            double toCamX = midX - camX;
            double toCamY = midY - camY;
            double toCamZ = midZ - camZ;

            double verticality = switch (this.axisThickness) {
                case X -> Math.abs(upVec.x());
                case Y -> Math.abs(upVec.y());
                case Z -> Math.abs(upVec.z());
            };

            double score = (1.0 - verticality) * (toCamX * upVec.x() + toCamY * upVec.y() + toCamZ * upVec.z()) + verticality * ((Mth.square(toCamX) + Mth.square(toCamY) + Mth.square(toCamZ)) / (maxLen * 0.5 + 1));

            if (score < bestScore) {
                bestScore = score;
                bottomFromX = fromX; bottomFromY = fromY; bottomFromZ = fromZ;
                bottomToX = toX; bottomToY = toY; bottomToZ = toZ;
            }
        }

        double topFromX = bottomFromX, topFromY = bottomFromY, topFromZ = bottomFromZ;
        double topToX = bottomToX, topToY = bottomToY, topToZ = bottomToZ;

        switch (this.axisThickness) {
            case X -> {
                if (bottomFromZ == bottomToZ) {
                    double newZ = (bottomFromZ == this.minZ) ? this.maxZ : this.minZ;
                    topFromZ = newZ; topToZ = newZ;
                } else {
                    double newY = (bottomFromY == this.minY) ? this.maxY : this.minY;
                    topFromY = newY; topToY = newY;
                }
            }
            case Y -> {
                if (bottomFromZ == bottomToZ) {
                    double newZ = (bottomFromZ == this.minZ) ? this.maxZ : this.minZ;
                    topFromZ = newZ; topToZ = newZ;
                } else {
                    double newX = (bottomFromX == this.minX) ? this.maxX : this.minX;
                    topFromX = newX; topToX = newX;
                }
            }
            case Z -> {
                if (bottomFromY == bottomToY) {
                    double newY = (bottomFromY == this.minY) ? this.maxY : this.minY;
                    topFromY = newY; topToY = newY;
                } else {
                    double newX = (bottomFromX == this.minX) ? this.maxX : this.minX;
                    topFromX = newX; topToX = newX;
                }
            }
        }

        return new double[]{bottomFromX, bottomFromY, bottomFromZ, bottomToX, bottomToY, bottomToZ, topFromX, topFromY, topFromZ, topToX, topToY, topToZ};
    }

    public boolean isInside(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return isInside(this.leftBottom, minX, minY, minZ, maxX, maxY, maxZ)
                && isInside(this.leftTop, minX, minY, minZ, maxX, maxY, maxZ)
                && isInside(this.rightBottom, minX, minY, minZ, maxX, maxY, maxZ)
                && isInside(this.rightTop, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean isInside(@NotNull BlockPos pos, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public @NotNull CompoundTag toRelativeNBT(int originX, int originY, int originZ) {
        CompoundTag tag = new CompoundTag();
        long[] relCorners = new long[4];
        relCorners[0] = BlockPos.asLong(this.leftBottom.getX() - originX, this.leftBottom.getY() - originY, this.leftBottom.getZ() - originZ);
        relCorners[1] = BlockPos.asLong(this.rightBottom.getX() - originX, this.rightBottom.getY() - originY, this.rightBottom.getZ() - originZ);
        relCorners[2] = BlockPos.asLong(this.leftTop.getX() - originX, this.leftTop.getY() - originY, this.leftTop.getZ() - originZ);
        relCorners[3] = BlockPos.asLong(this.rightTop.getX() - originX, this.rightTop.getY() - originY, this.rightTop.getZ() - originZ);
        tag.putLongArray(SavedWalls.CORNERS_KEY, relCorners);
        tag.putString(SavedWalls.LOCAL_SOUND_KEY, this.localSound.toString());
        tag.putFloat(SavedWalls.VOLUME_KEY, this.volume);
        tag.putString(SavedWalls.VIDEO_KEY, this.video);
        tag.putString(SavedWalls.AUDIO_KEY, this.audio);
        tag.putBoolean(SavedWalls.LIGHT_KEY, this.light);
        return tag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Wall other)) return false;

        return this.leftBottom.equals(other.leftBottom) && this.rightBottom.equals(other.rightBottom) && this.leftTop.equals(other.leftTop) && this.rightTop.equals(other.rightTop) && this.dimension.equals(other.dimension);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    @Override
    public String toString() {
        return "Wall{" +
                "corners=[" + this.leftBottom.toShortString() + ", " + this.rightBottom.toShortString() + ", " + this.leftTop.toShortString() + ", " + this.rightTop.toShortString() + "]" +
                ", dim=" + this.dimension +
                ", size=(" + this.widthX + "x" + this.widthY + "x" + this.widthZ + ")" +
                ", axis=" + this.axisThickness +
                ", center=[" + String.format("%.2f", this.centerX) + ", " + String.format("%.2f", this.centerY) + ", " + String.format("%.2f", this.centerZ) + "]" +
                ", localSound=" + this.localSound +
                ", volume=" + this.volume +
                ", video='" + this.video + '\'' +
                ", audio='" + this.audio + '\'' +
                ", light=" + this.light +
                '}';
    }
}
