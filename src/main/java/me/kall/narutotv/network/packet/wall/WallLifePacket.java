package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WallLifePacket extends WallPacket {
    public WallLifePacket(Wall wall) {
        super(wall);
    }

    public WallLifePacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    public void handle(@Nullable ServerPlayer player, boolean s2c) {
        try {
            Client.newWall(this.wall);
        } catch (Throwable throwable) {
            NarutoPackets.LOGGER.error("Error handing ScreenLifePacket", throwable);
        }
    }
}
