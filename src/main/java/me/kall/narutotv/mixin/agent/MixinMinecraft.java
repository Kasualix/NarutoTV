package me.kall.narutotv.mixin.agent;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.narutotv.impl.NarutoProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"))
    private void shutdownEarlyRenderer(Minecraft instance, Overlay loadingGui, @NotNull Operation<Void> original) {
        System.setProperty(NarutoProperties.SHUTDOWN, "true");
        String elapsed = System.getProperty(NarutoProperties.EARLY_END);
        if (elapsed == null) System.setProperty(NarutoProperties.EARLY_END, String.valueOf(System.nanoTime()));
        original.call(instance, loadingGui);
    }
}
