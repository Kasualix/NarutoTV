package me.kall.narutotv.mixin.world.light;

import me.kall.narutotv.world.WallTV;
import me.kall.narutotv.data.world.wall.ClientWalls;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IForgeBlockState.class)
public interface MixinIForgeBlockState {
    /**
     * @author Kall
     * @reason Light walls
     */
    @Overwrite(remap = false)
    default int getLightEmission(BlockGetter level, BlockPos pos) {
        if (level instanceof ClientLevel clientLevel) {
            WallTV<?> tv = ClientWalls.get(clientLevel.dimension().location(), pos.asLong());
            if (tv != null && tv.isRunning() && tv.wall.light) return tv.getLight(pos);
        }
        return ((BlockState)this).getBlock().getLightEmission(((BlockState)this), level, pos);
    }
}
