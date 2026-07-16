package me.kall.narutotv.fade;

import net.minecraft.resources.ResourceLocation;

public interface FadeApi {
    static FadeApi getInstance() {
        return CustomFade.getInstance();
    }

    void setUnfadable(ResourceLocation id, boolean unfadable);
}