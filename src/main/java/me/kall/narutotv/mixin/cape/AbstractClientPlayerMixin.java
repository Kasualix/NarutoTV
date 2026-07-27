package me.kall.narutotv.mixin.cape;

import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    /*@WrapMethod(method = "getCloakTextureLocation")
    private ResourceLocation customCape(Operation<ResourceLocation> original) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        UUID uuid = player.getUUID();

    }*/
}
