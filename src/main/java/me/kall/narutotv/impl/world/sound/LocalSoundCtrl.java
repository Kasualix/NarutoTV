package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.util.function.DoubleConsumer;

public class LocalSoundCtrl {
    public final Runnable off;
    public final DoubleConsumer on;

    public LocalSoundCtrl(BlockScreen screen) {
        this.off = () -> Minecraft.getInstance().getSoundManager().stop(screen.localSound, SoundSource.MUSIC);
        this.on = (seekTo) -> {
            this.off.run();
            Minecraft.getInstance().getSoundManager().play(new NarutoSound(screen, seekTo));
        };
    }
}
