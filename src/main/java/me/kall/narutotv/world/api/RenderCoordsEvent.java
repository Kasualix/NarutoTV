package me.kall.narutotv.world.api;

import me.kall.narutotv.util.NarutoMath;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

public class RenderCoordsEvent extends Event {
    public final NarutoMath.Coords coords;
    public final ResourceLocation dimension;

    public RenderCoordsEvent(NarutoMath.Coords coords, ResourceLocation dimension) {
        this.coords = coords;
        this.dimension = dimension;
    }
}
