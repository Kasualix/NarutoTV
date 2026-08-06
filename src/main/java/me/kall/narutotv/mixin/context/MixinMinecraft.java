package me.kall.narutotv.mixin.context;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.override.GuiSceneControl;
import me.kall.narutotv.produce.util.LifetimeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow
    public abstract boolean isPaused();

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
    private void shutdownEarlyRenderer(Minecraft instance, Overlay loadingGui, @NotNull Operation<Void> original) {
        RenderProps.shutdown();
        RenderProps.markEnd();
        GuiSceneControl.init();
        original.call(instance, loadingGui);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void checkPause(CallbackInfo ci) {
        if (this.isPaused()) {
            ClientWalls.forEach(renderer -> {
                if (renderer.video != null) {
                    LifetimeController life = renderer.video.life();
                    if (life != null) life.pause();
                }
            });
        } else {
            ClientWalls.forEach(renderer -> {
                if (renderer.video != null) {
                    LifetimeController life = renderer.video.life();
                    if (life != null) life.resume();
                }
            });
        }
    }
}
