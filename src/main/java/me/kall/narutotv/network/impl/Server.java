package me.kall.narutotv.network.impl;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.data.world.Displayers;
import me.kall.narutotv.data.world.wall.SavedWalls;
import me.kall.narutotv.data.world.wall.Wall;
import me.kall.narutotv.network.packet.wall.WallUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class Server {
    public static void cleanWall(Wall wall, @NotNull IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer)) return;
        ServerLevel level = ((ServerPlayer) player).serverLevel();

        LongSet areaInvolved = wall.areaInvolved();
        LongSet borderInvolved = wall.borderInvolved();
        areaInvolved.removeIf(borderInvolved::contains);

        Displayers.Cleaner.setCleaning(level, true);
        areaInvolved.forEach(position -> level.destroyBlock(BlockPos.of(position), true, player));
        Displayers.Cleaner.setCleaning(level, false);
    }

    public static void updateWall(Wall wall, @NotNull IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer)) return;
        ServerLevel level = ((ServerPlayer) player).serverLevel();

        SavedWalls.get(level).update(wall);

        WallUpdatePacket packet = new WallUpdatePacket(wall);
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            PacketDistributor.sendToPlayer(other, packet);
        }
    }
}
