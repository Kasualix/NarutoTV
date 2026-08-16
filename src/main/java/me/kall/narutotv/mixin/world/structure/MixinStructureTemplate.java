package me.kall.narutotv.mixin.world.structure;

import me.kall.narutotv.data.world.wall.SavedWalls;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.packet.wall.WallLifePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(StructureTemplate.class)
public abstract class MixinStructureTemplate {
    @Unique private final List<CompoundTag> narutoTV$walls = new ArrayList<>();

    @Inject(method = "fillFromWorld", at = @At("RETURN"))
    private void onFillFromWorld(Level level, BlockPos pos, Vec3i size, boolean withEntities, Block toIgnore, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int minX = Math.min(pos.getX(), pos.getX() + size.getX() - 1);
        int minY = Math.min(pos.getY(), pos.getY() + size.getY() - 1);
        int minZ = Math.min(pos.getZ(), pos.getZ() + size.getZ() - 1);

        int maxX = minX + size.getX() - 1;
        int maxY = minY + size.getY() - 1;
        int maxZ = minZ + size.getZ() - 1;

        this.narutoTV$walls.clear();

        for (Wall wall : SavedWalls.get(serverLevel).getIn(serverLevel.dimension().location())) {
            if (wall.isInside(minX, minY, minZ, maxX, maxY, maxZ)) this.narutoTV$walls.add(wall.toRelativeNBT(minX, minY, minZ));
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void onSave(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!this.narutoTV$walls.isEmpty()) {
            ListTag list = new ListTag();
            list.addAll(this.narutoTV$walls);
            tag.put(SavedWalls.DATA_NAME, list);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void onLoad(HolderGetter<Block> blockGetter, @NotNull CompoundTag tag, CallbackInfo ci) {
        this.narutoTV$walls.clear();
        if (tag.contains(SavedWalls.DATA_NAME, Tag.TAG_LIST)) {
            ListTag list = tag.getList(SavedWalls.DATA_NAME, Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                this.narutoTV$walls.add(list.getCompound(index));
            }
        }
    }

    @Inject(method = "placeInWorld", at = @At("RETURN"))
    private void onPlaceInWorld(net.minecraft.world.level.ServerLevelAccessor serverLevel, BlockPos offset, BlockPos pos, StructurePlaceSettings settings, RandomSource random, int flags, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || this.narutoTV$walls.isEmpty()) return;
        if (!(serverLevel instanceof ServerLevel level)) return;

        ResourceLocation dimension = level.dimension().location();

        for (CompoundTag wallTag : this.narutoTV$walls) {
            long[] relCorners = wallTag.getLongArray(SavedWalls.CORNERS_KEY);
            long[] absCorners = new long[4];

            for (int i = 0; i < 4; i++) {
                BlockPos transformed = StructureTemplate.calculateRelativePosition(settings, BlockPos.of(relCorners[i]));
                absCorners[i] = BlockPos.asLong(transformed.getX() + pos.getX(), transformed.getY() + pos.getY(), transformed.getZ() + pos.getZ());
            }

            Wall wall = new Wall(absCorners, dimension, ResourceLocation.parse(wallTag.getString(SavedWalls.LOCAL_SOUND_KEY)), wallTag.getString(SavedWalls.VIDEO_KEY), wallTag.getString(SavedWalls.AUDIO_KEY), wallTag.getFloat(SavedWalls.VOLUME_KEY), wallTag.getBoolean(SavedWalls.LIGHT_KEY));

            SavedWalls.get(level).update(wall);
            PacketDistributor.sendToAllPlayers(new WallLifePacket(wall));
        }
    }
}