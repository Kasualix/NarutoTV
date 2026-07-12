package me.kall.narutotv.mixin.override.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.override.CustomOverride;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CreateWorldScreen.class)
public abstract class MixinCreateWorldScreen {
    @WrapMethod(method = "renderDirtBackground")
    private void skipRenderDirtBackground(GuiGraphics guiGraphics, Operation<Void> original) {
        if (CustomOverride.getInstance().overridable()) {
            CustomOverride.getInstance().override();
        } else {
            original.call(guiGraphics);
        }
    }
}