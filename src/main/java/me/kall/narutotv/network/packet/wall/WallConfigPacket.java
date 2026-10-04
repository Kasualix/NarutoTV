package me.kall.narutotv.network.packet.wall;

import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.impl.Client;
import me.kall.narutotv.network.packet.base.WallPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WallConfigPacket extends WallPacket {
    public WallConfigPacket(Wall wall) {
        super(wall);
    }

    public WallConfigPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@Nullable ServerPlayer player, boolean s2c) {
        try {
            Client.configWall(this.wall);
        } catch (Throwable throwable) {
            NarutoPackets.LOGGER.error("Error handling ScreenGuiPacket.", throwable);
        }
    }
}
