package me.kall.narutotv.override;

import java.util.function.BooleanSupplier;

public interface OverrideApi {
    static OverrideApi getInstance() {
        return CustomOverride.getInstance();
    }

    void setOverride(BooleanSupplier overridable, Runnable overrideTask);
}