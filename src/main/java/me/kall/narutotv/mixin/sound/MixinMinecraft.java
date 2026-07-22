package me.kall.narutotv.mixin.sound;

import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    public abstract boolean isPaused();

    @Inject(method = "runTick", at = @At("TAIL"))
    private void checkPause(CallbackInfo ci) {
        if (this.isPaused()) {
            ClientRenderers.getInstance().forEach(renderer -> {
                LifetimeController life = renderer.life();
                if (life != null) life.pause();
            });
        } else {
            ClientRenderers.getInstance().forEach(renderer -> {
                LifetimeController life = renderer.life();
                if (life != null) life.resume();
            });
        }
    }
}
