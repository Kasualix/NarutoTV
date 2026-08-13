package me.kall.narutotv.context;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.core.AbstractTV;
import me.kall.narutotv.data.system.RenderProps;
import me.kall.narutotv.data.world.wall.ClientWalls;
import me.kall.narutotv.override.GuiSceneControl;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class GpuAccel {
    private static boolean last = false;

    @SubscribeEvent
    public static void detect(TickEvent.@NotNull ClientTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.END)) return;

        boolean now = Minecraft.getInstance().level != null;
        if (now != last) {
            last = now;

            RenderProps.turnAccel(!now);

            ClientWalls.forEach(AbstractTV::restartSince);

            GuiSceneControl.active.restartSince();
        }
    }
}
