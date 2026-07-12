package me.kall.narutotv.fade;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FadeConfig implements FadeApi {
    private static final FadeConfig INSTANCE = new FadeConfig();
    private final ObjectOpenHashSet<ResourceLocation> unfadableResources = new ObjectOpenHashSet<>();
    private final AtomicInteger ticksBeforeFade = new AtomicInteger(200);
    private final AtomicBoolean fadable = new AtomicBoolean(true);

    public static FadeConfig getInstance() {
        return INSTANCE;
    }

    public boolean fadable() {
        return this.fadable.get();
    }

    public void setFadable(boolean value) {
        this.fadable.set(value);
    }

    @Override
    public void setUnfadable(ResourceLocation id, boolean unfadable) {
        synchronized (this.unfadableResources) {
            if (unfadable) {
                this.unfadableResources.add(id);
            } else {
                this.unfadableResources.remove(id);
            }
        }
    }

    public int ticksBeforeFade() {
        return ticksBeforeFade.get();
    }

    public void setTicksBeforeFade(int value) {
        this.ticksBeforeFade.set(value);
    }

    public boolean isUnfadable(ResourceLocation id) {
        synchronized (this.unfadableResources) {
            return this.unfadableResources.contains(id);
        }
    }
}