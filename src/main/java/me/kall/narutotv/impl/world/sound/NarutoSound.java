package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.Wall;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class NarutoSound extends SimpleSoundInstance {
    public final double seekTo;

    public NarutoSound(@NotNull Wall wall, double seekTo) {
        super(wall.localSound, SoundSource.MUSIC, wall.volume, 1.0F, RandomSource.create(), false, 0, Attenuation.LINEAR, wall.centerX, wall.centerY, wall.centerZ, false);
        this.seekTo = seekTo;
    }
}
