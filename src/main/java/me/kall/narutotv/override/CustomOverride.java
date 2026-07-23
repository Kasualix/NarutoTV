package me.kall.narutotv.override;

import me.kall.narutotv.NarutoTV;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public final class CustomOverride implements OverrideApi {
    private static final CustomOverride INSTANCE = new CustomOverride();
    public static CustomOverride getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public static void tick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            CustomOverride.getInstance().setTickConsumed(false);
        }
    }

    private final OverrideObject overrideObject = new OverrideObject();
    private boolean tickConsumed = false;

    @Override
    public void set(BooleanSupplier overridable, Runnable overrideTask) {
        this.overrideObject.overridable = overridable;
        this.overrideObject.overrideTask = overrideTask;
    }

    public boolean overridable() {
        return this.overrideObject.overridable();
    }

    public void override() {
        if (this.isTickConsumed()) return;
        this.overrideObject.override();
        this.setTickConsumed(true);
    }

    void setTickConsumed(boolean tickConsumed) {
        this.tickConsumed = tickConsumed;
    }

    boolean isTickConsumed() {
        return this.tickConsumed;
    }

    static final class OverrideObject {
        @Nullable BooleanSupplier overridable;
        @Nullable Runnable overrideTask;

        boolean overridable() {
            return this.overridable != null && this.overridable.getAsBoolean();
        }

        void override() {
            if (this.overrideTask != null) this.overrideTask.run();
        }
    }
}