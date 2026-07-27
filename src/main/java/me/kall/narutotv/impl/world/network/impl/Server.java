package me.kall.narutotv.impl.world.network.impl;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.data.Walls;
import me.kall.narutotv.impl.world.ext.ScreenLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class Server {
    public static void cleanWall(Wall wall, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        LongSet areaInvolved = wall.areaInvolved();
        LongSet borderInvolved = wall.borderInvolved();
        areaInvolved.removeIf(borderInvolved::contains);

        ScreenLevel.setCleaning(level, true);
        areaInvolved.forEach(position -> level.destroyBlock(BlockPos.of(position), true, player));
        ScreenLevel.setCleaning(level, false);
    }

    public static void updateWall(Wall wall, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        Walls.get(player.serverLevel()).update(wall);
    }
}
