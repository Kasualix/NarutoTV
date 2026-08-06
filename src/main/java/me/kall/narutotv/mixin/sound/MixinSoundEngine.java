package me.kall.narutotv.mixin.sound;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.Channel;
import me.kall.narutotv.core.world.WallTV;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL11;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @WrapMethod(method = "reload")
    private void reloadGuiSound(@NotNull Operation<Void> original) {
        Minecraft.getInstance().execute(() -> {
            GuiSceneControl.active.pauseAudio();
            ClientWalls.forEach(WallTV::pauseAudio);
        });
        original.call();
        Minecraft.getInstance().execute(() -> {
            GuiSceneControl.active.resumeAudio();
            ClientWalls.forEach(WallTV::resumeAudio);
        });
    }

    @Dynamic
    @WrapOperation(method = {"lambda$play$6", "method_19752"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/audio/Channel;play()V"))
    private void playSound(@NotNull Channel channel, Operation<Void> original, @Local(argsOnly = true) SoundInstance sound) {
        if (sound instanceof WallTV.NarutoSound narutoSound) AL11.alSourcef(((ChannelAccessor)channel).narutotv$source(), AL11.AL_SEC_OFFSET, (float) narutoSound.seekTo);
        channel.play();
    }
}
