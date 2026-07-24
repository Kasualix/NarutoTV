package me.kall.narutotv.mixin.context;

import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResourceLoadStateTracker.class)
public abstract class MixinResourceLoadStateTracker {
    @Inject(method = "finishReload", at = @At("TAIL"))
    private void onFinishReload(CallbackInfo ci) {
        ClientRenderers.forEach(AbstractRenderer::shutdown);
    }
}
