package me.kall.narutotv.data.world.wall;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.renderer.ImageFrameRenderer;
import me.kall.narutotv.util.NarutoMath;
import me.kall.narutotv.world.WallTV;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientWalls {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Wall, WallTV<?>>> DATA = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<WallTV<?>>> POSITION_CACHE = new Object2ObjectOpenHashMap<>();

    public static @NotNull ObjectCollection<WallTV<?>> getIn(ResourceLocation dimension) {
        return DATA.getOrDefault(dimension, Object2ObjectMaps.emptyMap()).values();
    }

    public static @Nullable Wall getNearest(ResourceLocation dimension, @NotNull Player player) {
        float partialTick = Minecraft.getInstance().getPartialTick();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 view = player.getViewVector(partialTick);
        var tvs = ClientWalls.getIn(dimension);
        if (tvs.isEmpty()) return null;

        Wall target = null;
        double minDist = Double.MAX_VALUE;

        for (WallTV<?> tv : tvs) {
            Wall wall = tv.wall;
            Vec3 intersection = NarutoMath.getIntersection(eye, view, wall);
            if (intersection == null) continue;
            double dist = eye.distanceToSqr(intersection);
            if (dist < minDist) {
                minDist = dist;
                target = wall;
            }
        }

        return target;
    }

    public static @Nullable WallTV<?> get(@NotNull Wall wall) {
        Object2ObjectMap<Wall, WallTV<?>> inDimension = DATA.get(wall.dimension);
        if (inDimension == null) return null;
        return inDimension.get(wall);
    }

    public static @Nullable WallTV<?> get(ResourceLocation dimension, long position) {
        Long2ObjectMap<WallTV<?>> cacheMap = POSITION_CACHE.get(dimension);
        return cacheMap == null ? null : cacheMap.get(position);
    }

    public static @NotNull Optional<WallTV<?>> remove(@NotNull Wall wall) {
        Object2ObjectMap<Wall, WallTV<?>> dimMap = DATA.get(wall.dimension);
        if (dimMap == null) return Optional.empty();

        WallTV<?> removed = dimMap.remove(wall);
        if (removed != null) clearCacheForWall(wall.dimension, wall.areaInvolved());
        return Optional.ofNullable(removed);
    }

    public static @NotNull Optional<WallTV<?>> add(Wall wall) {
        if (ClientWalls.isCompatMode()) {
            return add(new WallTV.Image(wall));
        } else {
            return add(new WallTV.Buffer(wall));
        }
    }

    private static @NotNull Optional<WallTV<?>> add(@NotNull WallTV<?> tv) {
        Wall wall = tv.wall;
        Object2ObjectMap<Wall, WallTV<?>> dimMap = DATA.computeIfAbsent(wall.dimension, key -> new Object2ObjectOpenHashMap<>());

        WallTV<?> old = dimMap.put(wall, tv);
        if (old != null) clearCacheForWall(wall.dimension, old.wall.areaInvolved());

        Long2ObjectMap<WallTV<?>> cacheMap = POSITION_CACHE.computeIfAbsent(wall.dimension, key -> new Long2ObjectOpenHashMap<>());
        for (long pos : wall.areaInvolved()) cacheMap.put(pos, tv);
        return Optional.ofNullable(old);
    }

    private static void clearCacheForWall(ResourceLocation dimension, LongSet area) {
        Long2ObjectMap<WallTV<?>> cacheMap = POSITION_CACHE.get(dimension);
        if (cacheMap != null) {
            for (long pos : area) cacheMap.remove(pos);
            if (cacheMap.isEmpty()) POSITION_CACHE.remove(dimension);
        }
    }

    public static boolean isCompatMode() {
        if (DATA.isEmpty()) return CompatCenter.shaderUsing();
        for (Object2ObjectMap<Wall, WallTV<?>> walls : DATA.values()) {
            for (WallTV<?> tv : walls.values()) return tv.renderer instanceof ImageFrameRenderer;
        }
        return CompatCenter.shaderUsing();
    }

    public static boolean isEmpty() {
        return DATA.isEmpty();
    }

    public static void swap() {
        boolean isCompatMode = ClientWalls.isCompatMode();

        ObjectSet<WallTV<?>> latestSet = new ObjectOpenHashSet<>();

        for (Object2ObjectMap<Wall, WallTV<?>> value : DATA.values()) {
            for (Object2ObjectMap.Entry<Wall, WallTV<?>> wallEntry : value.object2ObjectEntrySet()) {
                Wall wall = wallEntry.getKey();
                WallTV<?> outdated = wallEntry.getValue();

                LifetimeController life = outdated.video == null ? null : outdated.video.life();

                WallTV<?> latest = isCompatMode ? new WallTV.Buffer(wall) : new WallTV.Image(wall);

                latest.mediaArgs = outdated.mediaArgs;

                latest.setup(life == null ? 0D : life.sinceSetupSec());
                outdated.shutdownEntire(false);

                latestSet.add(latest);
            }
        }

        DATA.clear();
        POSITION_CACHE.clear();
        latestSet.forEach(ClientWalls::add);
    }

    public static void setCompatMode() {
        if (!ClientWalls.isCompatMode()) swap();
    }

    public static void forEach(Consumer<WallTV<?>> action) {
        for (Object2ObjectMap<Wall, WallTV<?>> inDimension : DATA.values()) {
            for (WallTV<?> tv : inDimension.values()) {
                action.accept(tv);
            }
        }
    }

    @SubscribeEvent
    public static void logOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientWalls.forEach(WallTV.DEATH);
        DATA.clear();
        POSITION_CACHE.clear();
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        assert level != null;

        if (player == null) return;

        Object2ObjectMap<Wall, WallTV<?>> inDimension = DATA.get(level.dimension().location());

        if (inDimension == null || inDimension.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        RenderCaptured.bufferSource(bufferSource);
        RenderCaptured.camera(camera);

        for (WallTV<?> tv : inDimension.values()) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, - cameraPos.y, - cameraPos.z);
            RenderCaptured.poseStack(poseStack);

            tv.render();

            RenderCaptured.poseStack(null);
            poseStack.popPose();
        }

        RenderCaptured.bufferSource(null);
        RenderCaptured.camera(null);
    }
}
