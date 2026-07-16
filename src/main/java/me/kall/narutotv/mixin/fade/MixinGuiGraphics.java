package me.kall.narutotv.mixin.fade;


import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.fade.FadeCenter;
import me.kall.narutotv.fade.CustomFade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = GuiGraphics.class, priority = 500)
public abstract class MixinGuiGraphics {
    @WrapMethod(method = "setColor")
    private void fade$setColor(float red, float green, float blue, float alpha, @NotNull Operation<Void> original) {
        original.call(red, green, blue, alpha * FadeCenter.fadeAlpha());
    }

    @WrapMethod(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V")
    private void fade$fill(RenderType renderType, int minX, int minY, int maxX, int maxY, int z, int color, @NotNull Operation<Void> original) {
        original.call(renderType, minX, minY, maxX, maxY, z, FadeCenter.modifyAlpha(color));
    }

    @WrapMethod(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V")
    private void fade$fillGradient(VertexConsumer consumer, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, @NotNull Operation<Void> original) {
        original.call(consumer, x1, y1, x2, y2, z, FadeCenter.modifyAlpha(colorFrom), FadeCenter.modifyAlpha(colorTo));
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V")
    private void fade$innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha, @NotNull Operation<Void> original) {
        if (FadeCenter.isHidden() && !CustomFade.getInstance().isUnfadable(atlasLocation)) return;
        original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV, red, green, blue, CustomFade.getInstance().isUnfadable(atlasLocation) ? alpha : alpha * FadeCenter.fadeAlpha());
    }

    @WrapMethod(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V")
    private void fade$innerBlit(ResourceLocation atlasLocation, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, Operation<Void> original) {
        if (FadeCenter.isHidden() && !CustomFade.getInstance().isUnfadable(atlasLocation)) return;
        original.call(atlasLocation, x1, x2, y1, y2, blitOffset, minU, maxU, minV, maxV);
    }
}