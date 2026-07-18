package me.kall.narutotv.mixin.world;

import me.kall.narutotv.impl.world.ext.ScreenLevel;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public class MixinServerLevel implements ScreenLevel {
    @Unique private boolean narutotv$cleaning;

    @Override
    public boolean narutotv$isCleaning() {
        return this.narutotv$cleaning;
    }

    @Override
    public void narutotv$setCleaning(boolean cleaning) {
        this.narutotv$cleaning = cleaning;
    }
}
