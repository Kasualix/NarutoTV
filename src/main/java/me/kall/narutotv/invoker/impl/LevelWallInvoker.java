package me.kall.narutotv.invoker.impl;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.world.WallTV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class LevelWallInvoker extends VideoDropInvoker {
    private static final Component TOOLTIP = Component.translatable("invoker.narutotv.wall.tooltip");

    private @Nullable Wall wall;

    @Override
    protected void forResolved(MediaArgs mediaArgs) {
        assert this.wall != null;
        WallTV<?> tv = ClientWalls.get(this.wall);
        if (tv == null) return;
        tv.shutdownEntire(true);
        tv.mediaArgs = mediaArgs;
    }

    @Override
    public Component tooltip() {
        return TOOLTIP;
    }

    @Override
    public boolean isRunnable() {
        this.wall = null;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return false;

        this.wall = ClientWalls.getNearest(level.dimension().location(), player);
        return this.wall != null;
    }
}
