package me.kall.narutotv.context;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.data.world.wall.ClientWalls;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID, value = Dist.CLIENT)
public class ShaderDetection {
    private static boolean last;
    private static int interval;

    @SubscribeEvent
    public static void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (CompatCenter.hasShaderMod() && event.phase.equals(TickEvent.Phase.START)) {
            if (interval >= 0) {
                interval--;
                return;
            }

            interval = 10;

            boolean current = CompatCenter.shaderUsing();
            if (current != last) {
                ClientWalls.setCompatMode();
                ClientCapes.setCompatMode();
            }
            last = current;
        }
    }
}