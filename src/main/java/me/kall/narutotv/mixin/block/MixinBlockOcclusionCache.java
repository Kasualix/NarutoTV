package me.kall.narutotv.mixin.block;

import me.kall.narutotv.context.LastCoords;
import me.kall.narutotv.world.NarutoMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.embeddium.impl.modern.render.chunk.compile.pipeline.BlockOcclusionCache;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public abstract class MixinBlockOcclusionCache {
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private void skipForVideo(BlockState selfState, BlockGetter view, BlockPos pos, Direction facing, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        double x = (double) pos.getX() + 0.5D + 0.5D * (double) facing.getStepX();
        double y = (double) pos.getY() + 0.5D + 0.5D * (double) facing.getStepY();
        double z = (double) pos.getZ() + 0.5D + 0.5D * (double) facing.getStepZ();

        synchronized (LastCoords.DATA) {
            for (NarutoMath.Coords coords : LastCoords.get(level.dimension().location())) {
                if (LastCoords.isIn(coords, x, y, z)) {
                    cir.setReturnValue(false);
                    break;
                }
            }
        }
    }
}
