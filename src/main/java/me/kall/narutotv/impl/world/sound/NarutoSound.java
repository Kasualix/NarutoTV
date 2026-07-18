package me.kall.narutotv.impl.world.sound;

import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class NarutoSound extends SimpleSoundInstance {
    private static final RandomSource RANDOM = RandomSource.create();

    public final double seekTo;

    public NarutoSound(@NotNull BlockScreen screen, double seekTo) {
        super(screen.localSound, SoundSource.MUSIC, 1.0F, 1.0F, RANDOM, false, 0, Attenuation.LINEAR, screen.centerX, screen.centerY, screen.centerZ, false);
        this.seekTo = seekTo;
    }
}
