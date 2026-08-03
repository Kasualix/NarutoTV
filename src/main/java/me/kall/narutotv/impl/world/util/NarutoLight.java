package me.kall.narutotv.impl.world.util;

import com.mojang.blaze3d.platform.NativeImage;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.mixin.context.NativeImageAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class NarutoLight {
    public static int forBuffer(ByteBuffer yuvBuffer, int width, int height) {
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
        long pixels = ((NativeImageAccessor)(Object)image).getPixels();

        int width = image.getWidth();
        int height = image.getHeight();

        long sum = 0;
        int samples = 0;
        int step = 8;

        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int pixel = MemoryUtil.memGetInt(pixels + (x + (long)y * width) * 4L);
                int r = (pixel) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;

                sum += (long) (0.2126 * r + 0.7152 * g + 0.0722 * b);
                samples++;
            }
        }

        int avgLuma = samples == 0 ? 0 : (int) (sum / samples);
        return Math.min(15, Math.max(0, (int) Math.round((avgLuma / 255.0) * 15)));
    }

    public static void checkLight(Wall wall) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        minecraft.execute(() -> wall.areaInvolved().forEach(pos -> level.getLightEngine().checkBlock(BlockPos.of(pos))));
    }
}
