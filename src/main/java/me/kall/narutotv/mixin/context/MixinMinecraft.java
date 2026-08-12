package me.kall.narutotv.mixin.context;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.data.world.wall.ClientWalls;
import me.kall.narutotv.override.GuiSceneControl;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.produce.video.AbstractFrameProducer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow public abstract boolean isPaused();

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", remap = false))
    private void initGuiScene(int i, boolean bl, @NotNull Operation<Void> original) {
        original.call(i, bl);
        GuiSceneControl.init();
        RenderProps.shutdown();

        AbstractFrameProducer<?> video = GuiSceneControl.active.video;
        if (video != null) video.eager();
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
