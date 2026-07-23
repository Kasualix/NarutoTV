package me.kall.narutotv.impl.qol;

import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;

public class ShaderDetection {
    public static void register(@NotNull IEventBus forgeBus) {
        forgeBus.addListener(new ShaderDetection()::tickClient);
    }

    private boolean shaderUsed;
    private int interval;

    private void tickClient(TickEvent.@NotNull ClientTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.START)) return;

        if (this.interval >= 0) {
            this.interval--;
            return;
        }

        this.interval = 10;

        boolean shaderUsed = IrisApi.getInstance().isShaderPackInUse();
        if (shaderUsed != this.shaderUsed) ClientRenderers.getInstance().compat();

        this.shaderUsed = shaderUsed;
    }
}
