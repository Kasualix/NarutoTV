package me.kall.narutotv.impl.level;

import me.kall.narutotv.NarutoTV;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
    public float volume = 1.0F;

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
                this.rightBottom.getX() + this.rightBottom.getY() + this.rightBottom.getZ();
    }

    public boolean hasLocalSound() {
        return this.localSound != NO_LOCAL_SOUND;
    }
}