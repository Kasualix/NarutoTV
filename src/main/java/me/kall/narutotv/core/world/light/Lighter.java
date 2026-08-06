package me.kall.narutotv.core.world.light;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class Lighter implements LightAccessor {
    private int light;

    private final Wall wall;

    public Lighter(Wall wall) {
        this.wall = wall;
    }

    @Override
    public int getLight() {
        return this.light;
    }

    public void updateLight(int newLight) {
        if (this.light == newLight) return;
        this.light = newLight;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        minecraft.execute(() -> this.wall.areaInvolved().forEach(pos -> level.getLightEngine().checkBlock(BlockPos.of(pos))));
    }

    public static int forBuffer(@NotNull ByteBuffer yuvBuffer, int width, int height) {
        int ySize = width * height;
        long sum = 0;
        int step = 16;
        int samples = 0;

        for (int i = 0; i < ySize; i += step) {
            sum += (yuvBuffer.get(i) & 0xFF);
            samples++;
        }

        int avgLuma = samples == 0 ? 0 : (int) (sum / samples);
        return Math.min(15, Math.max(0, (int) Math.round((avgLuma / 255.0) * 15)));
    }

    public static int forImage(@NotNull NativeImage image) {
        long pixels = ((NativeImageAccessor) (Object) image).getPixels();

        int width = image.getWidth();
        int height = image.getHeight();

        long sum = 0;
        int samples = 0;

        int step = Math.max(8, width / 120);

        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int pixel = MemoryUtil.memGetInt(pixels + (x + (long) y * width) * 4L);
                int r = (pixel) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;

                sum += (54L * r + 183L * g + 19L * b) >> 8;
                samples++;
            }
        }

        int avgLuma = samples == 0 ? 0 : (int) (sum / samples);
        return Math.min(15, Math.max(0, (int) Math.round((avgLuma / 255.0) * 15)));
    }
}
