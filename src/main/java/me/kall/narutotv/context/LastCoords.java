package me.kall.narutotv.context;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.util.NarutoMath;
import me.kall.narutotv.world.api.RenderCoordsEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class LastCoords {
    public static final Object2ObjectMap<ResourceLocation, Object2IntMap<NarutoMath.Coords>> DATA = new Object2ObjectOpenHashMap<>();

    private static final double SAME_SURFACE = 0.01D;

    private static final Predicate<Object2IntMap.Entry<NarutoMath.Coords>> TICK = entry -> {
        if (entry.setValue(entry.getIntValue() - 1) <= 1) {
            LastCoords.refresh(entry.getKey());
            return true;
        }

        return false;
    };

    public static @NotNull ObjectSet<NarutoMath.Coords> get(ResourceLocation dimension) {
        return DATA.getOrDefault(dimension, Object2IntMaps.emptyMap()).keySet();
    }

    @SubscribeEvent
    public static void tick(RenderFrameEvent.Pre event) {
        if (!DATA.isEmpty()) {
            synchronized (DATA) {
                for (Object2IntMap<NarutoMath.Coords> coordsMap : DATA.values()) {
                    coordsMap.object2IntEntrySet().removeIf(TICK);
                }

                DATA.values().removeIf(Object2IntMap::isEmpty);
            }
        }
    }

    @SubscribeEvent
    public static void render(@NotNull RenderCoordsEvent event) {
        synchronized (DATA) {
            Object2IntMap<NarutoMath.Coords> coordsMap = DATA.computeIfAbsent(event.dimension, key -> Object2IntMaps.synchronize(new Object2IntOpenHashMap<>()));
            NarutoMath.Coords coords = event.coords;

            if (!coordsMap.containsKey(coords)) LastCoords.refresh(coords);

            coordsMap.put(coords, 2);
        }
    }

    private static void refresh(NarutoMath.@NotNull Coords coords) {
        double minX = Math.min(coords.bottomFromX(), Math.min(coords.bottomToX(), coords.topFromX()));
        double minY = Math.min(coords.bottomFromY(), Math.min(coords.bottomToY(), coords.topFromY()));
        double minZ = Math.min(coords.bottomFromZ(), Math.min(coords.bottomToZ(), coords.topFromZ()));
        double maxX = Math.max(coords.bottomFromX(), Math.max(coords.bottomToX(), coords.topFromX()));
        double maxY = Math.max(coords.bottomFromY(), Math.max(coords.bottomToY(), coords.topFromY()));
        double maxZ = Math.max(coords.bottomFromZ(), Math.max(coords.bottomToZ(), coords.topFromZ()));

        Minecraft.getInstance().levelRenderer.setBlocksDirty((int) Math.floor(minX - 1), (int) Math.floor(minY - 1), (int) Math.floor(minZ - 1), (int) Math.ceil(maxX + 1), (int) Math.ceil(maxY + 1), (int) Math.ceil(maxZ + 1));
    }

    public static boolean isIn(NarutoMath.@NotNull Coords coords, double pointX, double pointY, double pointZ) {
        double bottomFromX = coords.bottomFromX(), bottomFromY = coords.bottomFromY(), bottomFromZ = coords.bottomFromZ();
        double bottomToX   = coords.bottomToX(),   bottomToY   = coords.bottomToY(),   bottomToZ   = coords.bottomToZ();
        double topFromX    = coords.topFromX(),    topFromY    = coords.topFromY(),    topFromZ    = coords.topFromZ();

        double edgeBottomX = bottomToX - bottomFromX, edgeBottomY = bottomToY - bottomFromY, edgeBottomZ = bottomToZ - bottomFromZ;
        double edgeSideX   = topFromX - bottomFromX,  edgeSideY   = topFromY - bottomFromY,  edgeSideZ   = topFromZ - bottomFromZ;

        double normalX = coords.normalX(), normalY = coords.normalY(), normalZ = coords.normalZ();
        double normalLenSq = Mth.square(normalX) + Mth.square(normalY) + Mth.square(normalZ);

        double dotNormal = (pointX - bottomFromX) * normalX + (pointY - bottomFromY) * normalY + (pointZ - bottomFromZ) * normalZ;

        if (Math.abs(dotNormal / Math.sqrt(normalLenSq)) >= SAME_SURFACE) return false;

        double factor = dotNormal / normalLenSq;

        double toProjX = pointX - factor * normalX - bottomFromX;
        double toProjY = pointY - factor * normalY - bottomFromY;
        double toProjZ = pointZ - factor * normalZ - bottomFromZ;

        double paramU = Math.max(0, Math.min(1, (toProjX * edgeBottomX + toProjY * edgeBottomY + toProjZ * edgeBottomZ) / (edgeBottomX * edgeBottomX + edgeBottomY * edgeBottomY + edgeBottomZ * edgeBottomZ)));
        double paramV = Math.max(0, Math.min(1, (toProjX * edgeSideX   + toProjY * edgeSideY   + toProjZ * edgeSideZ)   / (edgeSideX   * edgeSideX   + edgeSideY   * edgeSideY   + edgeSideZ   * edgeSideZ)));

        double xDistSq = Mth.square(pointX - (bottomFromX + paramU * edgeBottomX + paramV * edgeSideX));
        double yDistSq = Mth.square(pointY - (bottomFromY + paramU * edgeBottomY + paramV * edgeSideY));
        double zDistSq = Mth.square(pointZ - (bottomFromZ + paramU * edgeBottomZ + paramV * edgeSideZ));

        return xDistSq + yDistSq + zDistSq < Mth.square(SAME_SURFACE);
    }
}
