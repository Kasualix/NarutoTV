package me.kall.narutotv.mixin.world;

import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.client.ClientWalls;
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
            AbstractRenderer<?> renderer = ClientWalls.get(clientLevel.dimension().location(), pos.asLong());
            if (renderer != null && renderer.isRunning()) return renderer.getLightLevel();
        }
        return ((BlockState)this).getBlock().getLightEmission(((BlockState)this), level, pos);
    }
}
