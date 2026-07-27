package me.kall.narutotv.impl.world.data.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.impl.world.data.Wall;
import me.kall.narutotv.impl.world.ext.InWorld;
import me.kall.narutotv.impl.world.wall.WorldBufferRenderer;
import me.kall.narutotv.impl.world.wall.WorldImageRenderer;
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
public class ClientRenderers {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> RENDERERS = new Object2ObjectOpenHashMap<>();

    public static @NotNull ObjectCollection<AbstractRenderer<?>> getIn(ResourceLocation dimension) {
        return RENDERERS.getOrDefault(dimension, Object2ObjectMaps.emptyMap()).values();
    }

    public static @Nullable AbstractRenderer<?> get(ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        return inDimension.get(new Vec3(centerX, centerY, centerZ));
    }

    public static @Nullable AbstractRenderer<?> get(@NotNull Wall wall) {
        return get(wall.dimension, wall.centerX, wall.centerY, wall.centerZ);
    }

    public static @Nullable AbstractRenderer<?> get(ResourceLocation dimension, long position) {
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        for (AbstractRenderer<?> renderer : inDimension.values()) {
            Wall wall = ((InWorld)renderer).wall();
            if (wall.areaInvolved().contains(position)) {
                return renderer;
            }
        }
        return null;
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return Optional.empty();
        return Optional.ofNullable(inDimension.remove(new Vec3(centerX, centerY, centerZ)));
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull AbstractRenderer<?> renderer) {
        Wall wall = ((InWorld)renderer).wall();
        return remove(wall.dimension, wall.centerX, wall.centerY, wall.centerZ);
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull Wall wall) {
        return remove(wall.dimension, wall.centerX, wall.centerY, wall.centerZ);
    }

    public static @NotNull Optional<AbstractRenderer<?>> add(@NotNull AbstractRenderer<?> renderer) {
        Wall wall = ((InWorld)renderer).wall();
        return Optional.ofNullable(RENDERERS.computeIfAbsent(wall.dimension, key -> new Object2ObjectOpenHashMap<>()).put(new Vec3(wall.centerX, wall.centerY, wall.centerZ), renderer));
    }

    public static @NotNull Optional<AbstractRenderer<?>> add(@NotNull Wall wall) {
        if (isImageRenderer()) {
            return add(new WorldImageRenderer(wall));
        } else {
            return add(new WorldBufferRenderer(wall));
        }
    }

    public static boolean isImageRenderer() {
        if (RENDERERS.isEmpty()) return CompatCenter.shaderUsing();
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
            for (AbstractRenderer<?> renderer : inDimension.values()) return !(renderer instanceof WorldBufferRenderer);
        }
        return CompatCenter.shaderUsing();
    }

    public static void swap() {
        boolean targetIsBuffer = isImageRenderer();
        Object2ObjectMap<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> swapped = new Object2ObjectOpenHashMap<>();

        for (Map.Entry<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> dimEntry : RENDERERS.entrySet()) {
            ResourceLocation dimension = dimEntry.getKey();
            Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = dimEntry.getValue();
            for (Map.Entry<Vec3, AbstractRenderer<?>> posEntry : inDimension.entrySet()) {
                Vec3 center = posEntry.getKey();
                AbstractRenderer<?> renderer = posEntry.getValue();

                Wall wall = ((InWorld) renderer).wall();
                LifetimeController life = renderer.life();

                AbstractRenderer<?> newRenderer = targetIsBuffer ? new WorldBufferRenderer(wall) : new WorldImageRenderer(wall);

                swapped.computeIfAbsent(dimension, key -> new Object2ObjectOpenHashMap<>()).put(center, newRenderer);

                MediaArgs mediaArgs = renderer.mediaArgs();
                if (mediaArgs != null) Sources.cutInLine(mediaArgs.absVideoPath(), mediaArgs.absAudioPath());

                renderer.shutdown();
                newRenderer.setup(life != null ? life.sinceSetupSec() : 0D);
            }
        }

        RENDERERS.clear();
        RENDERERS.putAll(swapped);
    }

    public static void compat() {
        if (isImageRenderer()) return;
        swap();
    }

    public static void forEach(@NotNull Consumer<AbstractRenderer<?>> action) {
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
            for (AbstractRenderer<?> renderer : inDimension.values()) {
                action.accept(renderer);
            }
        }
    }

    @SubscribeEvent
    public static void logOutClean(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(() -> {
            forEach(AbstractRenderer::shutdown);
            RENDERERS.clear();
        });
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        assert level != null;

        if (player == null) return;

        ResourceLocation dimension = level.dimension().location();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);

        if (inDimension == null || inDimension.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        for (AbstractRenderer<?> renderer : inDimension.values()) {
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, - cameraPos.y, - cameraPos.z);

            if (renderer instanceof WorldBufferRenderer bufferRenderer) {
                bufferRenderer.capture(poseStack, camera);
                bufferRenderer.render();
                bufferRenderer.deprecate();
            } else if (renderer instanceof WorldImageRenderer imageRenderer) {
                imageRenderer.capture(poseStack, bufferSource, camera);
                imageRenderer.render();
                imageRenderer.deprecate();
            }

            poseStack.popPose();
        }
    }
}
