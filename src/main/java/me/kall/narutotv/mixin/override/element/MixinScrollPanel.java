package me.kall.narutotv.mixin.override.element;

import me.kall.narutotv.override.CustomOverride;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScrollPanel.class, remap = false)
public abstract class MixinScrollPanel {
    @Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"), cancellable = true)
    private void skipRenderDarkDirtBackground(@NotNull CallbackInfo ci) {
        if (CustomOverride.getInstance().overridable()) ci.cancel();
    }
}