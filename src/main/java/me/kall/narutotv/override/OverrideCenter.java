package me.kall.narutotv.override;

import me.kall.narutotv.NarutoTV;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public final class OverrideCenter {
    private static final OverrideCenter INSTANCE = new OverrideCenter();

    private final BooleanSupplier overridable = () -> GuiSceneControl.active.isRunnable();
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
    public static void tick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            OverrideCenter.getInstance().setTickConsumed(false);
        }
    }
}