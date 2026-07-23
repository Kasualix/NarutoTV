package me.kall.narutotv.impl.world.data.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
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
import java.util.function.Predicate;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientRenderers {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> RENDERERS = new Object2ObjectOpenHashMap<>();

    public static @Nullable AbstractRenderer<?> get(ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        return inDimension.get(new Vec3(centerX, centerY, centerZ));
    }

    public static @Nullable AbstractRenderer<?> get(@NotNull BlockScreen blockScreen) {
        return get(blockScreen.dimension, blockScreen.centerX, blockScreen.centerY, blockScreen.centerZ);
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
        Minecraft.getInstance().execute(() -> {
            forEach(AbstractRenderer::shutdown);
            boolean isImageRenderer = isImageRenderer();
            for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
                inDimension.replaceAll((pos, renderer) -> {
                    BlockScreen screen = ((InWorld)renderer).screen();
                    return isImageRenderer ? new WorldBufferRenderer(screen) : new WorldImageRenderer(screen);
                });
            }
            forEach(renderer -> {
                BlockScreen screen = ((InWorld)renderer).screen();
                if (!screen.video.isBlank() && !screen.audio.isBlank()) Sources.cutInLine(screen.video, screen.audio);
                renderer.setup(0D);
            });
        });
    }

    public static void compat() {
        if (isImageRenderer()) return;
        swap();
    }

    public static void forEach(@NotNull Consumer<AbstractRenderer<?>> action) {
        Minecraft.getInstance().execute(() -> {
            for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
                for (AbstractRenderer<?> renderer : inDimension.values()) {
                    action.accept(renderer);
                }
            }
        });
    }

    public static void forSpecific(Predicate<AbstractRenderer<?>> condition, Consumer<AbstractRenderer<?>> action) {
        Minecraft.getInstance().execute(() -> {
            for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : RENDERERS.values()) {
                for (AbstractRenderer<?> renderer : inDimension.values()) {
                    if (condition.test(renderer)) {
                        action.accept(renderer);
                        break;
                    }
                }
            }
        });
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
        }
    }

    @SubscribeEvent
    public static void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        assert level != null;

        if (player == null) return;

        ResourceLocation dimension = level.dimension().location();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = RENDERERS.get(dimension);

        if (inDimension == null || inDimension.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        for (AbstractRenderer<?> renderer : inDimension.values()) {
            BlockScreen screen = ((InWorld)renderer).screen();
            if (screen.tooFar(player)) continue;

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
