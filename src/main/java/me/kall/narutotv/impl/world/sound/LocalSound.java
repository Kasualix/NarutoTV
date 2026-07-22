package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.util.function.DoubleConsumer;

public class LocalSound {
    public final Runnable off;
    public final DoubleConsumer on;

    public LocalSound(BlockScreen screen) {
        this.on = (seekTo) -> Minecraft.getInstance().getSoundManager().play(new NarutoSound(screen, seekTo));
        this.off = () -> Minecraft.getInstance().getSoundManager().stop(screen.localSound, SoundSource.MUSIC);
    }
}
