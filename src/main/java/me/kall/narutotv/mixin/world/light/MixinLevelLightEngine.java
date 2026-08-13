package me.kall.narutotv.mixin.world.light;

import me.kall.narutotv.world.light.PosLighter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(LevelLightEngine.class)
public class MixinLevelLightEngine implements PosLighter.LevelLightEngineAccessor {
    @Shadow @Final @Nullable private LightEngine<?, ?> blockEngine;
    @Shadow @Final @Nullable private LightEngine<?, ?> skyEngine;

    @Override
    public void naruto$checkBlock(long pos) {
        if (this.blockEngine != null) ((PosLighter.LightEngineAccessor)this.blockEngine).naruto$checkBlock(pos);
        if (this.skyEngine != null) ((PosLighter.LightEngineAccessor)this.skyEngine).naruto$checkBlock(pos);
    }
}
