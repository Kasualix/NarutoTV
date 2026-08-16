package me.kall.narutotv.override;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.config.NarutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.function.BooleanSupplier;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public final class OverrideCenter {
    private static final OverrideCenter INSTANCE = new OverrideCenter();

    private final BooleanSupplier overridable = () -> GuiSceneControl.active.isRunnable() && NarutoConfig.enableGuiScreen();
    private final Runnable task = () -> GuiSceneControl.active.render();

    private boolean tickConsumed = false;

    public boolean overridable() {
        return this.overridable.getAsBoolean();
    }

    public void override() {
        if (this.isTickConsumed()) return;
        this.task.run();
        this.setTickConsumed(true);
    }

    public void setTickConsumed(boolean tickConsumed) {
        this.tickConsumed = tickConsumed;
    }

    boolean isTickConsumed() {
        return this.tickConsumed;
    }

    public static OverrideCenter getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public static void tick(RenderFrameEvent.Pre event) {
        OverrideCenter.getInstance().setTickConsumed(false);
    }
}