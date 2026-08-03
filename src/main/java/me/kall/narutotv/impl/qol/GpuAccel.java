package me.kall.narutotv.impl.qol;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.NarutoProperties;
import me.kall.narutotv.impl.gui.NarutoGuiCenter;
import me.kall.narutotv.impl.world.data.client.ClientWalls;
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

            if (now) {
                System.clearProperty(NarutoProperties.GPU_ACCEL);
            } else {
                System.setProperty(NarutoProperties.GPU_ACCEL, "");
            }

            ClientWalls.forEach(renderer -> {
                LifetimeController life = renderer.life();
                if (life != null) renderer.restart(life.sinceSetupSec());
            });

            AbstractRenderer<?> renderer = NarutoGuiCenter.getActive();
            LifetimeController life = renderer.life();
            if (life != null) renderer.restart(life.sinceSetupSec());
        }
    }
}
