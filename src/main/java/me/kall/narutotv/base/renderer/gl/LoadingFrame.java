package me.kall.narutotv.base.renderer.gl;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class LoadingFrame {
    private static final String TEXT = "Video Loading...";

    private CacheEntry cache = null;

    public synchronized @NotNull ByteBuffer get(int width, int height) {
        if (this.cache != null && this.cache.width == width && this.cache.height == height) return this.cache.buffer.duplicate();

        ByteBuffer generated = this.toYuv420(this.genImage(width, height));

        this.cache = new CacheEntry();
        this.cache.buffer = generated;
        this.cache.width = width;
        this.cache.height = height;

        return generated.duplicate();
    }

    private @NotNull BufferedImage genImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, width, height);

            int fontSize = Math.max(18, Math.min(width, height) / 10);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            graphics.setColor(Color.WHITE);

            FontMetrics metrics = graphics.getFontMetrics();
            int x = Math.max(0, (width - metrics.stringWidth(TEXT)) / 2);
            int y = Math.max(metrics.getAscent(), (height - metrics.getHeight()) / 2 + metrics.getAscent());

            graphics.drawString(TEXT, x, y);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private @NotNull ByteBuffer toYuv420(@NotNull BufferedImage image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int uvWidth = Math.max(1, (imageWidth + 1) / 2);
        int uvHeight = Math.max(1, (imageHeight + 1) / 2);

        int ySize = imageWidth * imageHeight;
        int uvSize = uvWidth * uvHeight;
        int needed = ySize + 2 * uvSize;

        byte[] y = new byte[ySize];
        byte[] u = new byte[uvSize];
        byte[] v = new byte[uvSize];

        for (int py = 0; py < imageHeight; py += 2) {
            for (int px = 0; px < imageWidth; px += 2) {
                int uAcc = 0, vAcc = 0, samples = 0;
                for (int dy = 0; dy < 2; dy++) {
                    if (py + dy >= imageHeight) continue;
                    for (int dx = 0; dx < 2; dx++) {
                        if (px + dx >= imageWidth) continue;

                        int xPos = Math.min(px + dx, imageWidth - 1);
                        int yPos = Math.min(py + dy, imageHeight - 1);

                        int rgb = image.getRGB(xPos, yPos);

                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        int yVal = clamp((int) Math.round(0.257D * r + 0.504D * g + 0.098D * b + 16.0D));
                        int uVal = clamp((int) Math.round(-0.148D * r - 0.291D * g + 0.439D * b + 128.0D));
                        int vVal = clamp((int) Math.round(0.439D * r - 0.368D * g - 0.071D * b + 128.0D));

                        y[yPos * imageWidth + xPos] = (byte) yVal;

                        uAcc += uVal;
                        vAcc += vVal;

                        samples++;
                    }
                }
                int uvX = px / 2;
                int uvY = py / 2;
                int uvIndex = uvY * uvWidth + uvX;
                u[uvIndex] = (byte) (uAcc / samples);
                v[uvIndex] = (byte) (vAcc / samples);
            }
        }

        return ByteBuffer.allocateDirect(needed).put(y).put(u).put(v).rewind().asReadOnlyBuffer();
    }

    private static int clamp(int value) {
        if (value < 0) return 0;
        return Math.min(value, 255);
    }

    private static final class CacheEntry {
        int width; int height; ByteBuffer buffer;
    }
}