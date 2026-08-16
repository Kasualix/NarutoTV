package me.kall.narutotv.mixin.override.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.override.OverrideCenter;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @WrapMethod(method = "renderBlurredBackground")
    private void override(float partialTick, Operation<Void> original) {
        if (OverrideCenter.getInstance().overridable()) {
            OverrideCenter.getInstance().override();
        } else {
            original.call(partialTick);
        }
    }
}