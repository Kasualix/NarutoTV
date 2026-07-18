package me.kall.narutotv.impl.world.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.NarutoTV;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class BlockScreen {
    public static final ResourceLocation HAS_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "has"), NO_LOCAL_SOUND = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "no");

    public final BlockPos leftBottom;
    public final BlockPos leftTop;
    public final BlockPos rightBottom;
    public final BlockPos rightTop;
    public final ResourceLocation dimension;

    public final double centerX, centerY, centerZ;

    public final String id;

    public ResourceLocation localSound = NO_LOCAL_SOUND;

    public String video, audio;

    private LongSet areaInvolved, borderInvolved;

    public BlockScreen(long @NotNull [] corners, ResourceLocation dimension, ResourceLocation localSound) {
        this(BlockPos.of(corners[0]), BlockPos.of(corners[1]), BlockPos.of(corners[2]), BlockPos.of(corners[3]), dimension);
        this.localSound = localSound;
    }

    public BlockScreen(@NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2, @NotNull BlockPos topCorner1, @NotNull BlockPos topCorner2, ResourceLocation dimension) {
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

        if (Math.abs(dx) >= Math.abs(dz) ? bottomCorner1.getX() <= bottomCorner2.getX() : bottomCorner1.getZ() <= bottomCorner2.getZ()) {
            this.leftBottom = bottomCorner1;
            this.leftTop = topFor1;
            this.rightBottom = bottomCorner2;
            this.rightTop = topFor2;
        } else {
            this.leftBottom = bottomCorner2;
            this.leftTop = topFor2;
            this.rightBottom = bottomCorner1;
            this.rightTop = topFor1;
        }

        this.centerX = (bottomCorner1.getX() + bottomCorner2.getX() + topCorner1.getX() + topCorner2.getX()) / 4.0;
        this.centerY = (bottomCorner1.getY() + bottomCorner2.getY() + topCorner1.getY() + topCorner2.getY()) / 4.0;
        this.centerZ = (bottomCorner1.getZ() + bottomCorner2.getZ() + topCorner1.getZ() + topCorner2.getZ()) / 4.0;

        this.id = String.valueOf(this.leftBottom.getX()) + this.leftBottom.getY() + this.leftBottom.getZ() +
                this.rightBottom.getX() + this.rightBottom.getY() + this.rightBottom.getZ() +
                this.leftTop.getX() + this.leftTop.getY() + this.leftTop.getZ() +
                this.rightBottom.getX() + this.rightBottom.getY() + this.rightBottom.getZ() + this.dimension.toString();
    }

    public boolean hasLocalSound() {
        return this.localSound != NO_LOCAL_SOUND;
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
        return new long[]{this.leftBottom.asLong(), this.leftTop.asLong(), this.rightBottom.asLong(), this.rightTop.asLong()};
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

    private static @NotNull LongList getLine(@NotNull BlockPos from, @NotNull BlockPos to) {
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
        return object instanceof BlockScreen another && another.id.equals(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}