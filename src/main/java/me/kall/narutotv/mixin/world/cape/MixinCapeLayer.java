package me.kall.narutotv.mixin.world.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CapeLayer.class, priority = 1500)
public abstract class MixinCapeLayer {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/PlayerModel;renderCloak(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void naruto$drawCape(PlayerModel<AbstractClientPlayer> model, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, Operation<Void> original, @Local(argsOnly = true) MultiBufferSource bufferSource, @Local(argsOnly = true) @NotNull AbstractClientPlayer player) {
        CapeTV<?> capeTV = ClientCapes.get(player.getUUID());

        if (capeTV != null) {
            RenderCaptured.poseStack(poseStack);
            RenderCaptured.bufferSource(bufferSource);
            capeTV.render();
            RenderCaptured.poseStack(null);
            RenderCaptured.bufferSource(null);
            return;
        }

        original.call(model, poseStack, buffer, packedLight, packedOverlay);
    }
}
