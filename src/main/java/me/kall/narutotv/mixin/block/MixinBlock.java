package me.kall.narutotv.mixin.block;

import me.kall.narutotv.context.LastCoords;
import me.kall.narutotv.world.NarutoMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {
    @Inject(method = "shouldRenderFace", at = @At("RETURN"), cancellable = true)
    private static void skipForVideo(BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) return;

        ResourceLocation dimension = clientLevel.dimension().location();

        double centerX = (double) offset.getX() + 0.5D + (double) face.getStepX() * 0.5D;
        double centerY = (double) offset.getY() + 0.5D + (double) face.getStepY() * 0.5D;
        double centerZ = (double) offset.getZ() + 0.5D + (double) face.getStepZ() * 0.5D;

        synchronized (LastCoords.DATA) {
            for (NarutoMath.Coords coords : LastCoords.get(dimension)) {
                if (LastCoords.isIn(coords, centerX, centerY, centerZ)) {
                    cir.setReturnValue(false);
                    break;
                }
            }
        }
    }
}