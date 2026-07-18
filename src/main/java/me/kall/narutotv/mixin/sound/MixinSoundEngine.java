package me.kall.narutotv.mixin.sound;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.narutotv.app.produce.audio.AudioProducer;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @WrapMethod(method = "reload")
    private void reloadSound(@NotNull Operation<Void> original) {
        AbstractRenderer<?> renderer = NarutoGuiCenter.getActive();
        LifetimeController life = renderer.life();
        AudioProducer audio = renderer.audio();
        long start = System.nanoTime();
        if (audio != null && life != null) audio.shutdown();
        original.call();
        if (audio != null && life != null) audio.setup((System.nanoTime() - start + life.nanoTimeFromSetup()) / 1_000_000_000.0D);
    }
}
