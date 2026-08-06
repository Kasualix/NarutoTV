package me.kall.narutotv.mixin.override.element;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.override.OverrideCenter;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PanoramaRenderer.class)
public abstract class MixinPanoramaRenderer {
    @WrapMethod(method = "render")
    private void skipPanoramaRenderer(float deltaT, float alpha, Operation<Void> original) {
        if (OverrideCenter.getInstance().overridable()) {
            OverrideCenter.getInstance().override();
        } else {
            original.call(deltaT, alpha);
        }
    }
}