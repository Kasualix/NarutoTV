package me.kall.narutotv.context;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.data.world.wall.ClientWalls;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class GpuAccel {
    private static boolean last = false;

    @SubscribeEvent
    public static void detect(ClientTickEvent.Post event) {
        boolean now = Minecraft.getInstance().level != null;
        if (now != last) {
            last = now;

            RenderProps.turnAccel(!now);

            ClientWalls.forEach(AbstractTV::restartSince);

            GuiSceneControl.active.restartSince();
        }
    }
}
