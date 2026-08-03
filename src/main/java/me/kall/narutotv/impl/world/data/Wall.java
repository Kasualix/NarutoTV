package me.kall.narutotv.impl.world.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.NarutoTV;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Objects;

public class Wall {
    public static final ResourceLocation NO_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "no");

    public final BlockPos leftBottom;
    public final BlockPos leftTop;
    public final BlockPos rightBottom;
    public final BlockPos rightTop;
    public final ResourceLocation dimension;

    public final double centerX, centerY, centerZ;
    public final String id;

    public ResourceLocation localSound = NO_LOCAL_SOUND;
    public float volume = 1.0F;

    public String video = "", audio = "";

    private LongSet areaInvolved, borderInvolved;
    private Data data;

    public Wall(long @NotNull [] corners, ResourceLocation dimension, ResourceLocation localSound, String video, String audio, float volume) {
        this(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
        this.localSound = localSound;
        this.video = video;
        this.audio = audio;
        this.volume = volume;
    }

    public Wall(@NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2, @NotNull BlockPos topCorner1, @NotNull BlockPos topCorner2, ResourceLocation dimension) {
        this.dimension = dimension;

        int dx = bottomCorner2.getX() - bottomCorner1.getX();
        int dy = bottomCorner2.getY() - bottomCorner1.getY();
        int dz = bottomCorner2.getZ() - bottomCorner1.getZ();

        BlockPos topFor1, topFor2;
        if ((topCorner1.getX() - bottomCorner1.getX()) * dx + (topCorner1.getY() - bottomCorner1.getY()) * dy + (topCorner1.getZ() - bottomCorner1.getZ()) * dz == 0) {
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

        this.centerX = (bottomCorner1.getX() + bottomCorner2.getX() + topCorner1.getX() + topCorner2.getX()) / 4.0;
        this.centerY = (bottomCorner1.getY() + bottomCorner2.getY() + topCorner1.getY() + topCorner2.getY()) / 4.0;
        this.centerZ = (bottomCorner1.getZ() + bottomCorner2.getZ() + topCorner1.getZ() + topCorner2.getZ()) / 4.0;

        this.id = String.valueOf(this.leftBottom.getX()) + this.leftBottom.getY() + this.leftBottom.getZ() +
                this.rightBottom.getX() + this.rightBottom.getY() + this.rightBottom.getZ() +
                this.leftTop.getX() + this.leftTop.getY() + this.leftTop.getZ() +
                this.rightTop.getX() + this.rightTop.getY() + this.rightTop.getZ() +
                this.dimension.getNamespace() + "_" + this.dimension.getPath();
    }

    public boolean hasLocalSound() {
        return !this.localSound.equals(NO_LOCAL_SOUND);
    }

    public boolean tooFar(@NotNull Player player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double distX = this.centerX - x;
        double distY = this.centerY - y;
        double distZ = this.centerZ - z;
        return distX * distX + distY * distY + distZ * distZ >= 64 * 64;
    }

    @Contract(" -> new")
    public long @NotNull [] toLongArray() {
        return new long[]{this.leftBottom.asLong(), this.rightBottom.asLong(), this.leftTop.asLong(), this.rightTop.asLong()};
    }

    public LongSet areaInvolved() {
        if (this.areaInvolved != null) return this.areaInvolved;
        int minX = Math.min(Math.min(this.leftBottom.getX(), this.leftTop.getX()), Math.min(this.rightBottom.getX(), this.rightTop.getX()));
        int minY = Math.min(Math.min(this.leftBottom.getY(), this.leftTop.getY()), Math.min(this.rightBottom.getY(), this.rightTop.getY()));
        int minZ = Math.min(Math.min(this.leftBottom.getZ(), this.leftTop.getZ()), Math.min(this.rightBottom.getZ(), this.rightTop.getZ()));

        int maxX = Math.max(Math.max(this.leftBottom.getX(), this.leftTop.getX()), Math.max(this.rightBottom.getX(), this.rightTop.getX()));
        int maxY = Math.max(Math.max(this.leftBottom.getY(), this.leftTop.getY()), Math.max(this.rightBottom.getY(), this.rightTop.getY()));
        int maxZ = Math.max(Math.max(this.leftBottom.getZ(), this.leftTop.getZ()), Math.max(this.rightBottom.getZ(), this.rightTop.getZ()));

        if (this.areaInvolved == null) this.areaInvolved = BlockPos.betweenClosedStream(minX, minY, minZ, maxX, maxY, maxZ).mapToLong(BlockPos::asLong).collect(LongOpenHashSet::new, LongOpenHashSet::add, LongOpenHashSet::addAll);
        return this.areaInvolved;
    }

    public LongSet borderInvolved() {
        if (this.borderInvolved != null) return this.borderInvolved;
        LongList leftEdge = getLine(this.leftBottom, this.leftTop);
        LongList rightEdge = getLine(this.rightBottom, this.rightTop);
        LongList bottomEdge = getLine(this.leftBottom, this.rightBottom);
        LongList topEdge = getLine(this.leftTop, this.rightTop);

        LongSet borders = new LongOpenHashSet(leftEdge.size() + rightEdge.size() + bottomEdge.size() + topEdge.size() - 4);
        borders.addAll(leftEdge);
        borders.addAll(rightEdge);
        borders.addAll(bottomEdge);
        borders.addAll(topEdge);
        if (this.borderInvolved == null) this.borderInvolved = borders;
        return this.borderInvolved;
    }

    public @NotNull Wall.Data getData() {
        if (this.data != null) return this.data;
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

        this.data = new Data(minX, maxX + 1, minY, maxY + 1, minZ,  maxZ + 1);
        return this.data;
    }

    public static @NotNull LongList getLine(@NotNull BlockPos from, @NotNull BlockPos to) {
        int diffAxes = 0;
        if (from.getX() != to.getX()) diffAxes++;
        if (from.getY() != to.getY()) diffAxes++;
        if (from.getZ() != to.getZ()) diffAxes++;
        if (diffAxes > 1) throw new IllegalArgumentException("Line must be axis-aligned. from: [" + from.toShortString() + "], to: [" + to.toShortString() + "]");

        LongList line = new LongArrayList(Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))) + 1);

        int deltaX = Integer.compare(to.getX(), from.getX());
        int deltaY = Integer.compare(to.getY(), from.getY());
        int deltaZ = Integer.compare(to.getZ(), from.getZ());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(from.getX(), from.getY(), from.getZ());
        line.add(pos.asLong());

        while (!pos.equals(to)) {
            pos.move(deltaX, deltaY, deltaZ);
            line.add(pos.asLong());
        }

        return line;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Wall another && another.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public static final class Data {
        public final int minX, maxX, minY, maxY, minZ, maxZ;
        public final long widthX, widthY, widthZ;
        public final double centerX, centerY, centerZ;

        public final Direction.Axis axisThickness;

        public Data(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            if (minX > maxX) throw new IllegalArgumentException("minX must be <= maxX");
            if (minY > maxY) throw new IllegalArgumentException("minY must be <= maxY");
            if (minZ > maxZ) throw new IllegalArgumentException("minZ must be <= maxZ");

            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;

            this.centerX = (double) ((long) this.minX + this.maxX) / 2.0;
            this.centerY = (double) ((long) this.minY + this.maxY) / 2.0;
            this.centerZ = (double) ((long) this.minZ + this.maxZ) / 2.0;

            this.widthX = (long) this.maxX - this.minX;
            this.widthY = (long) this.maxY - this.minY;
            this.widthZ = (long) this.maxZ - this.minZ;

            this.axisThickness = this.axisThickness();
        }

        private @NotNull Direction.Axis axisThickness() {
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

        @Contract("_, _, _, _ -> new")
        public double @NotNull [] getCorners(double camX, double camY, double camZ, Vector3f upVec) {
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

            for (int i = 0; i < 4; i++) {
                double fromX = 0, fromY = 0, fromZ = 0, toX = 0, toY = 0, toZ = 0;
                double midX = 0, midY = 0, midZ = 0;
                long len = 0;

                switch (this.axisThickness) {
                    case X -> {
                        switch (i) {
                            case 0 -> { fromX = faceX; fromY = this.minY; fromZ = this.minZ; toX = faceX; toY = this.maxY; toZ = this.minZ; midX = faceX; midY = this.centerY; midZ = this.minZ; len = this.widthY; }
                            case 1 -> { fromX = faceX; fromY = this.minY; fromZ = this.maxZ; toX = faceX; toY = this.maxY; toZ = this.maxZ; midX = faceX; midY = this.centerY; midZ = this.maxZ; len = this.widthY; }
                            case 2 -> { fromX = faceX; fromY = this.minY; fromZ = this.minZ; toX = faceX; toY = this.minY; toZ = this.maxZ; midX = faceX; midY = this.minY; midZ = this.centerZ; len = this.widthZ; }
                            case 3 -> { fromX = faceX; fromY = this.maxY; fromZ = this.minZ; toX = faceX; toY = this.maxY; toZ = this.maxZ; midX = faceX; midY = this.maxY; midZ = this.centerZ; len = this.widthZ; }
                        }
                    }
                    case Y -> {
                        switch (i) {
                            case 0 -> { fromX = this.minX; fromY = faceY; fromZ = this.minZ; toX = this.maxX; toY = faceY; toZ = this.minZ; midX = this.centerX; midY=faceY; midZ = this.minZ; len = this.widthX; }
                            case 1 -> { fromX = this.minX; fromY = faceY; fromZ = this.maxZ; toX = this.maxX; toY = faceY; toZ = this.maxZ; midX = this.centerX; midY=faceY; midZ = this.maxZ; len = this.widthX; }
                            case 2 -> { fromX = this.minX; fromY = faceY; fromZ = this.minZ; toX = this.minX; toY = faceY; toZ = this.maxZ; midX = this.minX; midY=faceY; midZ = this.centerZ; len = this.widthZ; }
                            case 3 -> { fromX = this.maxX; fromY = faceY; fromZ = this.minZ; toX = this.maxX; toY = faceY; toZ = this.maxZ; midX = this.maxX; midY=faceY; midZ = this.centerZ; len = this.widthZ; }
                        }
                    }
                    case Z -> {
                        switch (i) {
                            case 0 -> { fromX = this.minX; fromY = this.minY; fromZ = faceZ; toX = this.maxX; toY = this.minY; toZ = faceZ; midX = this.centerX; midY = this.minY; midZ = faceZ; len = this.widthX; }
                            case 1 -> { fromX = this.minX; fromY = this.maxY; fromZ = faceZ; toX = this.maxX; toY = this.maxY; toZ = faceZ; midX = this.centerX; midY = this.maxY; midZ = faceZ; len = this.widthX; }
                            case 2 -> { fromX = this.minX; fromY = this.minY; fromZ = faceZ; toX = this.minX; toY = this.maxY; toZ = faceZ; midX = this.minX; midY = this.centerY; midZ = faceZ; len = this.widthY; }
                            case 3 -> { fromX = this.maxX; fromY = this.minY; fromZ = faceZ; toX = this.maxX; toY = this.maxY; toZ = faceZ; midX = this.maxX; midY = this.centerY; midZ = faceZ; len = this.widthY; }
                        }
                    }
                }

                if (maxLen > 0 && len < maxLen) continue;

                if (this.axisThickness == Direction.Axis.X && (i == 0 || i == 1) && this.widthZ >= this.widthY) continue;
                if (this.axisThickness == Direction.Axis.Y && (i == 2 || i == 3) && this.widthX >= this.widthZ) continue;
                if (this.axisThickness == Direction.Axis.Z && (i == 2 || i == 3) && this.widthX >= this.widthY) continue;

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

        @Override
        public int hashCode() {
            return Objects.hash(this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Data other && other.minX == this.minX && other.minY == this.minY && other.minZ == this.minZ && other.maxX == this.maxX && other.maxY == this.maxY && other.maxZ == this.maxZ;
        }
    }
}