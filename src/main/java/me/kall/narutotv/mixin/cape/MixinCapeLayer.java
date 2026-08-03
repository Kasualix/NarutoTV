package me.kall.narutotv.mixin.cape;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.cape.CapeBufferRenderer;
import me.kall.narutotv.impl.world.cape.CapeImageRenderer;
import me.kall.narutotv.impl.world.data.client.ClientVideoCapes;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(CapeLayer.class)
public abstract class MixinCapeLayer {
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/PlayerModel;renderCloak(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"))
    private void drawVideoCape(PlayerModel<AbstractClientPlayer> instance, PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, Operation<Void> original, @Local(argsOnly = true) @NotNull AbstractClientPlayer player, @Local(argsOnly = true) MultiBufferSource bufferSource) {
        UUID uuid = player.getUUID();
        ClientVideoCapes.VideoCape videoCape = ClientVideoCapes.get(uuid);

        if (videoCape == null) {
            original.call(instance, poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        AbstractRenderer<?> renderer = videoCape.renderer();

        if (renderer == null) {
            renderer = ClientVideoCapes.isImageRenderer() ? new CapeImageRenderer(videoCape) : new CapeBufferRenderer(videoCape);
            videoCape.setRenderer(renderer);
            renderer.setup(0D);
        }

        if (renderer instanceof CapeBufferRenderer bufferRenderer) {
            bufferRenderer.capture(poseStack);
            bufferRenderer.render();
            bufferRenderer.deprecate();
        } else if (renderer instanceof CapeImageRenderer imageRenderer) {
            imageRenderer.capture(poseStack, bufferSource);
            imageRenderer.render();
            imageRenderer.deprecate();
        } else {
            throw new IllegalArgumentException("Unsupported renderer type: " + renderer.getClass().getSimpleName());
        }
    }
}
