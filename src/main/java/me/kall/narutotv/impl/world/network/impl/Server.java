package me.kall.narutotv.impl.world.network.impl;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.BlockScreens;
import me.kall.narutotv.impl.world.ext.ScreenLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class Server {
    public static void cleanScreen(BlockScreen blockScreen, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ServerLevel level = player.serverLevel();

        LongSet areaInvolved = blockScreen.areaInvolved();
        LongSet borderInvolved = blockScreen.borderInvolved();
        areaInvolved.removeIf(borderInvolved::contains);

        ScreenLevel.setCleaning(level, true);
        areaInvolved.forEach(position -> level.removeBlock(BlockPos.of(position), false));
        ScreenLevel.setCleaning(level, false);
    }

    public static void updateScreen(BlockScreen blockScreen, NetworkEvent.@NotNull Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        BlockScreens.get(player.serverLevel()).update(blockScreen);
    }
}
