package me.kall.narutotv.world.light;

import me.kall.narutotv.data.world.Wall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class PosLighter implements LightAccessor {
    private byte[] blockLightMap;
    private int lightMapWidth, lightMapHeight;

    private final Wall wall;

    @SuppressWarnings("SuspiciousNameCombination")
    public PosLighter(@NotNull Wall wall) {
        this.wall = wall;
        switch (wall.axisThickness) {
            case X -> {
                this.lightMapWidth = wall.widthZ;
                this.lightMapHeight = wall.widthY;
            }
            case Y -> {
                this.lightMapWidth = wall.widthX;
                this.lightMapHeight = wall.widthZ;
            }
            case Z -> {
                this.lightMapWidth = wall.widthX;
                this.lightMapHeight = wall.widthY;
            }
        }
        this.blockLightMap = new byte[this.lightMapWidth * this.lightMapHeight];
    }

    @Override
    public int getLight(BlockPos pos) {
        int x, y;
        switch (this.wall.axisThickness) {
            case X -> { x = pos.getZ() - this.wall.minZ; y = pos.getY() - this.wall.minY; }
            case Y -> { x = pos.getX() - this.wall.minX; y = pos.getZ() - this.wall.minZ; }
            case Z -> { x = pos.getX() - this.wall.minX; y = pos.getY() - this.wall.minY; }
            default -> throw new IllegalStateException();
        }
        if (x < 0 || x >= this.lightMapWidth || y < 0 || y >= this.lightMapHeight) return 0;
        return this.blockLightMap[y * this.lightMapWidth + x] & 0xFF;
    }

    public void updateLight(byte[] lightMap, int imageWidth, int imageHeight) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        byte[] newBlockMap = new byte[this.lightMapWidth * this.lightMapHeight];

        for (int gridY = 0; gridY < this.lightMapHeight; gridY++) {
            for (int gridX = 0; gridX < this.lightMapWidth; gridX++) {
                int pixelStartX = gridX * imageWidth / this.lightMapWidth;
                int pixelEndX = (gridX + 1) * imageWidth / this.lightMapWidth;

                int pixelStartY = gridY * imageHeight / this.lightMapHeight;
                int pixelEndY = (gridY + 1) * imageHeight / this.lightMapHeight;

                long sum = 0;
                int count = 0;
                for (int pixelY = pixelStartY; pixelY < pixelEndY; pixelY++) {
                    int rowBase = pixelY * imageWidth;
                    for (int pixelX = pixelStartX; pixelX < pixelEndX; pixelX++) {
                        sum += lightMap[rowBase + pixelX] & 0xFF;
                        count++;
                    }
                }

                newBlockMap[gridY * this.lightMapWidth + gridX] = (byte) (count == 0 ? 0 : (int) (sum / count));
            }
        }

        for (int index = 0; index < newBlockMap.length; index++) {
            if (newBlockMap[index] != this.blockLightMap[index]) {
                ((LevelLightEngineAccessor)level.getLightEngine()).naruto$checkBlock(this.indexToBlock(index));
            }
        }

        this.blockLightMap = newBlockMap;
    }

    private long indexToBlock(int index) {
        int gx = index % this.lightMapWidth;
        int gy = index / this.lightMapWidth;
        return switch (this.wall.axisThickness) {
            case X -> BlockPos.asLong(this.wall.minX, this.wall.minY + gy, this.wall.minZ + gx);
            case Y -> BlockPos.asLong(this.wall.minX + gx, this.wall.minY, this.wall.minZ + gy);
            case Z -> BlockPos.asLong(this.wall.minX + gx, this.wall.minY + gy, this.wall.minZ);
        };
    }

    public interface LevelLightEngineAccessor {
        void naruto$checkBlock(long pos);
    }

    public interface LightEngineAccessor {
        void naruto$checkBlock(long pos);
    }
}
