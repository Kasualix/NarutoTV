package me.kall.narutotv.impl.qol;

import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;

public class ShaderDetection {
    public static void register(@NotNull IEventBus forgeBus) {
        forgeBus.addListener(ShaderDetection::tickClient);
    }

    private static boolean last;
    private static int interval;

    private static void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.START)) return;

        if (interval >= 0) {
            interval--;
            return;
        }

        interval = 10;

        boolean current = IrisApi.getInstance().isShaderPackInUse();
        if (current != last) ClientRenderers.compat();

        last = current;
    }
}
