package me.kall.narutotv.mixin.sound;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Options.class)
public abstract class MixinOptions {
    @WrapMethod(method = "getSoundSourceVolume")
    private float skipSound(@NotNull SoundSource category, Operation<Float> original) {
        return category.equals(SoundSource.MUSIC) && NarutoConfig.Client.musicMuted() && NarutoGuiCenter.getActive().isRunning() ? 0.0F : original.call(category);
    }
}
