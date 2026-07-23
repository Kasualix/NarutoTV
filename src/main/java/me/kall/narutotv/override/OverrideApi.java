package me.kall.narutotv.override;

import java.util.function.BooleanSupplier;

public interface OverrideApi {
    static void setTask(BooleanSupplier overridable, Runnable overrideTask) {
        CustomOverride.getInstance().set(overridable, overrideTask);
    }

    void set(BooleanSupplier overridable, Runnable overrideTask);
}