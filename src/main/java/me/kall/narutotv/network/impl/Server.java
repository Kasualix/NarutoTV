package me.kall.narutotv.network.impl;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.data.world.saved.Displayers;
import me.kall.narutotv.data.world.saved.Walls;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.WallUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class Server {
    public static void cleanWall(Wall wall, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        LongSet areaInvolved = wall.areaInvolved();
        LongSet borderInvolved = wall.borderInvolved();
        areaInvolved.removeIf(borderInvolved::contains);

        Displayers.Cleaner.setCleaning(level, true);
        areaInvolved.forEach(position -> level.destroyBlock(BlockPos.of(position), true, player));
        Displayers.Cleaner.setCleaning(level, false);
    }

    public static void updateWall(Wall wall, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        Walls.get(level).update(wall);

        WallUpdatePacket packet = new WallUpdatePacket(wall);
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> other), packet);
        }
    }
}
