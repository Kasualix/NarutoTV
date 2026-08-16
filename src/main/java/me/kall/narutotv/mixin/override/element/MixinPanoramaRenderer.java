package me.kall.narutotv.mixin.override.element;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.override.OverrideCenter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @WrapMethod(method = "render")
    private void skipPanoramaRenderer(GuiGraphics guiGraphics, int width, int height, float fade, float partialTick, Operation<Void> original) {
        if (OverrideCenter.getInstance().overridable()) {
            OverrideCenter.getInstance().override();
        } else {
            original.call(guiGraphics, width, height, fade, partialTick);
        }
    }
}