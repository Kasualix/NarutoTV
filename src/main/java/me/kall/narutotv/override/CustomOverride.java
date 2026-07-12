package me.kall.narutotv.override;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public final class CustomOverride implements OverrideApi {
    private static final CustomOverride INSTANCE = new CustomOverride();
    public static CustomOverride getInstance() {
        return INSTANCE;
    }

    public static void register(@NotNull IEventBus forgeBus) {
        forgeBus.addListener(CustomOverride::tick);
    }

    private static void tick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) CustomOverride.getInstance().setTickConsumed(false);
    }

    private final OverrideObject overrideObject = new OverrideObject();
    private boolean tickConsumed = false;

    @Override
    public void setOverride(BooleanSupplier overridable, Runnable overrideTask) {
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

    public void setTickConsumed(boolean tickConsumed) {
        this.tickConsumed = tickConsumed;
    }

    public boolean isTickConsumed() {
        return this.tickConsumed;
    }

    static final class OverrideObject {
        public @Nullable BooleanSupplier overridable;
        public @Nullable Runnable overrideTask;

        public boolean overridable() {
            return this.overridable != null && this.overridable.getAsBoolean();
        }

        public void override() {
            if (this.overrideTask != null) this.overrideTask.run();
        }
    }
}