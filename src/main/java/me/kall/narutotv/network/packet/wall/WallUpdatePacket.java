package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.impl.Server;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WallUpdatePacket extends WallPacket {
    public WallUpdatePacket(Wall wall) {
        super(wall);
    }

    public WallUpdatePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@Nullable ServerPlayer player, boolean s2c) {
        try {
            if (s2c) {
                Client.updateWall(this.wall);
            } else {
                Server.updateWall(this.wall, player);
            }
        } catch (Throwable throwable) {
            NarutoPackets.LOGGER.error("Error handling ScreenUpdatePacket", throwable);
        }
    }
}
