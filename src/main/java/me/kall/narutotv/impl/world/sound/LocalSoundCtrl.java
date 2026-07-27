package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.Wall;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;

public class LocalSoundCtrl {
    private final Wall wall;

    public LocalSoundCtrl(Wall wall) {
        this.wall = wall;
    }

    public void setVolume(@NotNull Wall wall) {
        this.wall.volume = wall.volume;
    }

    public Runnable off() {
        return () -> Minecraft.getInstance().getSoundManager().stop(this.wall.localSound, SoundSource.MUSIC);
    }

    public DoubleConsumer on() {
        return (seekTo) -> {
            this.off().run();
            Minecraft.getInstance().getSoundManager().play(new NarutoSound(this.wall, seekTo));
        };
    }
}
