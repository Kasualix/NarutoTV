package me.kall.narutotv.mixin.world.light;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.kall.narutotv.world.light.PosLighter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightEngine.class)
public class MixinLightEngine implements PosLighter.LightEngineAccessor {
    @Shadow @Final private LongOpenHashSet blockNodesToCheck;

    @Override
    public void naruto$checkBlock(long pos) {
        this.blockNodesToCheck.add(pos);
    }
}