package me.kall.narutotv.world.light;

import net.minecraft.core.BlockPos;

public interface LightAccessor {
    int getLight(BlockPos pos);
    void setLightable(boolean lightable);
    void checkLight();
}
