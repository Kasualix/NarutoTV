package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;

public class LocalSoundCtrl {
    private final BlockScreen screen;

    public LocalSoundCtrl(BlockScreen screen) {
        this.screen = screen;
    }

    public void setVolume(@NotNull BlockScreen screen) {
        this.screen.volume = screen.volume;
    }

    public Runnable off() {
        return () -> Minecraft.getInstance().getSoundManager().stop(this.screen.localSound, SoundSource.MUSIC);
    }

    public DoubleConsumer on() {
        return (seekTo) -> {
            this.off().run();
            Minecraft.getInstance().getSoundManager().play(new NarutoSound(this.screen, seekTo));
        };
    }
}
