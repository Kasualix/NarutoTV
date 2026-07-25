package me.kall.narutotv.impl.world.data.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.data.Sources;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.base.renderer.gl.AbstractGLEngine;
import me.kall.narutotv.base.renderer.gl.WorldGLEngine;
import me.kall.narutotv.impl.world.WorldBufferRenderer;
import me.kall.narutotv.impl.world.WorldImageRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.ext.InWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientRenderers {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> RENDERERS = new Object2ObjectOpenHashMap<>();

    private static boolean tickConsumed = false;

    public static @Nullable AbstractRenderer<?> get(ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        return inDimension.get(new Vec3(centerX, centerY, centerZ));
    }

    public static @Nullable AbstractRenderer<?> get(@NotNull BlockScreen blockScreen) {
        return get(blockScreen.dimension, blockScreen.centerX, blockScreen.centerY, blockScreen.centerZ);
    }

    public static @Nullable AbstractRenderer<?> get(ResourceLocation dimension, long position) {
        validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        for (AbstractRenderer<?> renderer : inDimension.values()) {
            BlockScreen screen = ((InWorld)renderer).screen();
            if (screen.areaInvolved().contains(position)) {
                return renderer;
            }
        }
        return null;
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return Optional.empty();
        return Optional.ofNullable(inDimension.remove(new Vec3(centerX, centerY, centerZ)));
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull AbstractRenderer<?> renderer) {
        BlockScreen screen = ((InWorld)renderer).screen();
        return remove(screen.dimension, screen.centerX, screen.centerY, screen.centerZ);
    }

    public static @NotNull Optional<AbstractRenderer<?>> remove(@NotNull BlockScreen screen) {
        return remove(screen.dimension, screen.centerX, screen.centerY, screen.centerZ);
    }

    public static @NotNull Optional<AbstractRenderer<?>> add(@NotNull AbstractRenderer<?> renderer) {
        validateThread();
        BlockScreen screen = ((InWorld)renderer).screen();
        return Optional.ofNullable(RENDERERS.computeIfAbsent(screen.dimension, key -> new Object2ObjectOpenHashMap<>()).put(new Vec3(screen.centerX, screen.centerY, screen.centerZ), renderer));
    }

    public static @NotNull Optional<AbstractRenderer<?>> add(@NotNull BlockScreen screen) {
        if (isImageRenderer()) {
            return add(new WorldImageRenderer(screen));
        } else {
            return add(new WorldBufferRenderer(screen));
        }
    }

    public static boolean isImageRenderer() {
        if (RENDERERS.isEmpty()) return NarutoTV.shaderUsing();
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
            for (AbstractRenderer<?> renderer : inDimension.values()) return !(renderer instanceof WorldBufferRenderer);
        }
        return NarutoTV.shaderUsing();
    }

    public static void swap() {
        boolean targetIsBuffer = isImageRenderer();

        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
            for (Object2ObjectMap.Entry<Vec3, AbstractRenderer<?>> entry : inDimension.object2ObjectEntrySet()) {
                AbstractRenderer<?> oldRenderer = entry.getValue();
                BlockScreen screen = ((InWorld) oldRenderer).screen();
                oldRenderer.shutdown();
                entry.setValue(targetIsBuffer ? new WorldBufferRenderer(screen) : new WorldImageRenderer(screen));
            }
        }

        forEach(renderer -> {
            BlockScreen screen = ((InWorld) renderer).screen();
            if (!screen.video.isBlank() && !screen.audio.isBlank()) Sources.cutInLine(Paths.absolute(screen.video), Paths.absolute(screen.audio));
            renderer.setup(0D);
        });
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
    public static void tick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            if (RENDERERS.isEmpty()) return;
            forEach(AbstractRenderer::render);
            tickConsumed = false;
        }
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
        if (tickConsumed) return;
        tickConsumed = true;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        for (AbstractRenderer<?> renderer : inDimension.values()) {
            poseStack.pushPose();
            poseStack.translate(-camera.x, - camera.y, - camera.z);

            if (renderer instanceof WorldBufferRenderer bufferRenderer) {
                AbstractGLEngine engine = bufferRenderer.engine();
                if (engine instanceof WorldGLEngine worldGLEngine) {
                    worldGLEngine.capture(poseStack, camera);
                    worldGLEngine.render();
                    worldGLEngine.deprecate();
                }
            } else if (renderer instanceof WorldImageRenderer imageRenderer) {
                imageRenderer.render(poseStack, bufferSource, camera);
            }

            poseStack.popPose();
        }
    }

    private static void validateThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new UnsupportedOperationException("Invalid thread: " + Thread.currentThread().getName());
    }
}
