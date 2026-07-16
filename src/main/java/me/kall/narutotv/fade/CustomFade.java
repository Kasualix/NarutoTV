package me.kall.narutotv.fade;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

public class CustomFade implements FadeApi {
    private static final CustomFade INSTANCE = new CustomFade();

    private final ObjectOpenHashSet<ResourceLocation> unfadableResources = new ObjectOpenHashSet<>();

    public static CustomFade getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void setUnfadable(ResourceLocation id, boolean unfadable) {
        if (unfadable) {
            this.unfadableResources.add(id);
        } else {
            this.unfadableResources.remove(id);
        }
    }

    public synchronized boolean isUnfadable(ResourceLocation id) {
        return this.unfadableResources.contains(id);
    }
}