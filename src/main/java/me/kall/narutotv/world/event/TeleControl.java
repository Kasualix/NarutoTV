package me.kall.narutotv.world.event;

import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.data.world.ClientWalls;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.screen.NarutoWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class TeleControl {
    @SubscribeEvent
    public static void configureScreen(PlayerInteractEvent.@NotNull RightClickItem event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;

        if (!RegistryEntry.get(player.getMainHandItem()).toString().equals(NarutoConfig.teleControl())) return;
        if (!player.isShiftKeyDown()) return;


        Wall target = ClientWalls.getNearest(level.dimension().location(), player);

        if (target != null) minecraft.setScreen(new NarutoWorldScreen(minecraft.screen, target));
    }
}