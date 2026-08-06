package me.kall.narutotv.data.world;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.context.RenderCaptured;
import me.kall.narutotv.core.renderer.ImageFrameRenderer;
import me.kall.narutotv.core.world.WallTV;
import me.kall.narutotv.produce.util.LifetimeController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientWalls {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Wall, WallTV<?>>> DATA = new Object2ObjectOpenHashMap<>();

    public static @NotNull ObjectCollection<WallTV<?>> getIn(ResourceLocation dimension) {
        return DATA.getOrDefault(dimension, Object2ObjectMaps.emptyMap()).values();
    }

    public static @Nullable WallTV<?> get(@NotNull Wall wall) {
        Object2ObjectMap<Wall, WallTV<?>> inDimension = DATA.get(wall.dimension);
        if (inDimension == null) return null;
        return inDimension.get(wall);
    }

    public static @Nullable WallTV<?> get(ResourceLocation dimension, long position) {
        Object2ObjectMap<Wall, WallTV<?>> inDimension = DATA.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        for (Map.Entry<Wall, WallTV<?>> entry : inDimension.entrySet()) {
            if (entry.getKey().areaInvolved().contains(position)) return entry.getValue();
        }
        return null;
    }

    public static @NotNull Optional<WallTV<?>> remove(@NotNull Wall wall) {
        Object2ObjectMap<Wall, WallTV<?>> inDimension = DATA.get(wall.dimension);
        if (inDimension == null) return Optional.empty();
        return Optional.ofNullable(inDimension.remove(wall));
    }

    public static @NotNull Optional<WallTV<?>> add(@NotNull WallTV<?> tv) {
        Wall wall = tv.wall;
        return Optional.ofNullable(DATA.computeIfAbsent(wall.dimension, key -> new Object2ObjectOpenHashMap<>()).put(wall, tv));
    }

    public static @NotNull Optional<WallTV<?>> add(Wall wall) {
        if (ClientWalls.isCompatMode()) {
            return add(new WallTV.Image(wall));
        } else {
            return add(new WallTV.Buffer(wall));
        }
    }

    public static boolean isCompatMode() {
        if (DATA.isEmpty()) return CompatCenter.shaderUsing();
        for (Object2ObjectMap<Wall, WallTV<?>> walls : DATA.values()) {
            for (WallTV<?> tv : walls.values()) return tv.renderer instanceof ImageFrameRenderer;
        }
        return CompatCenter.shaderUsing();
    }

    public static void swap() {
        boolean isCompatMode = ClientWalls.isCompatMode();
        Object2ObjectMap<ResourceLocation, Object2ObjectMap<Wall, WallTV<?>>> swapped = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<ResourceLocation, Object2ObjectMap<Wall, WallTV<?>>> dimEntry : DATA.object2ObjectEntrySet()) {
            ResourceLocation dimension = dimEntry.getKey();
            for (Object2ObjectMap.Entry<Wall, WallTV<?>> wallEntry : dimEntry.getValue().object2ObjectEntrySet()) {
                Wall wall = wallEntry.getKey();
                WallTV<?> outdated = wallEntry.getValue();

                LifetimeController life = outdated.video == null ? null : outdated.video.life();

                WallTV<?> latest = isCompatMode ? new WallTV.Buffer(wall) : new WallTV.Image(wall);

                swapped.computeIfAbsent(dimension, key -> new Object2ObjectOpenHashMap<>()).put(wall, latest);

                latest.mediaArgs = outdated.mediaArgs;

                latest.setup(life == null ? 0D : life.sinceSetupSec());
                outdated.shutdownEntire(false);
            }
        }

        DATA.clear();
        DATA.putAll(swapped);
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
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
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
