package me.kall.narutotv.impl.world.data.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.base.renderer.gl.WorldGLEngine;
import me.kall.narutotv.impl.world.ext.BindScreen;
import me.kall.narutotv.impl.world.WorldBufferRenderer;
import me.kall.narutotv.impl.world.WorldImageRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public class ClientRenderers {
    private static final ClientRenderers INSTANCE = new ClientRenderers();
    
    public static ClientRenderers getInstance() {
        return INSTANCE;
    }

    public static void register(@NotNull IEventBus forgeBus) {
        forgeBus.addListener(INSTANCE::logOutClean);
        forgeBus.addListener(INSTANCE::tick);
        forgeBus.addListener(INSTANCE::renderLevel);
    }

    private final Object2ObjectMap<ResourceLocation, Object2ObjectMap<Vec3, AbstractRenderer<?>>> renderers = new Object2ObjectOpenHashMap<>();

    public @Nullable AbstractRenderer<?> get(ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        this.validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = this.renderers.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return null;
        return inDimension.get(new Vec3(centerX, centerY, centerZ));
    }

    public @Nullable AbstractRenderer<?> get(@NotNull BlockScreen blockScreen) {
        return this.get(blockScreen.dimension, blockScreen.centerX, blockScreen.centerY, blockScreen.centerZ);
    }

    public @NotNull Optional<AbstractRenderer<?>> remove(@NotNull ResourceLocation dimension, double centerX, double centerY, double centerZ) {
        this.validateThread();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = this.renderers.get(dimension);
        if (inDimension == null || inDimension.isEmpty()) return Optional.empty();
        return Optional.ofNullable(inDimension.remove(new Vec3(centerX, centerY, centerZ)));
    }

    public @NotNull Optional<AbstractRenderer<?>> remove(@NotNull AbstractRenderer<?> renderer) {
        BlockScreen screen = ((BindScreen)renderer).screen();
        return this.remove(screen.dimension, screen.centerX, screen.centerY, screen.centerZ);
    }

    public @NotNull Optional<AbstractRenderer<?>> remove(@NotNull BlockScreen screen) {
        return this.remove(screen.dimension, screen.centerX, screen.centerY, screen.centerZ);
    }

    public @NotNull Optional<AbstractRenderer<?>> add(@NotNull AbstractRenderer<?> renderer) {
        this.validateThread();
        BlockScreen screen = ((BindScreen)renderer).screen();
        return Optional.ofNullable(this.renderers.computeIfAbsent(screen.dimension, key -> new Object2ObjectOpenHashMap<>()).put(new Vec3(screen.centerX, screen.centerY, screen.centerZ), renderer));
    }

    public @NotNull Optional<AbstractRenderer<?>> add(@NotNull BlockScreen screen) {
        if (this.isImageRenderer()) {
            return this.add(new WorldImageRenderer(screen));
        } else {
            return this.add(new WorldBufferRenderer(screen));
        }
    }

    public boolean isImageRenderer() {
        if (this.renderers.isEmpty()) return NarutoTV.COMPAT.shaderUsing();
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : this.renderers.values()) {
            for (AbstractRenderer<?> renderer : inDimension.values()) return !(renderer instanceof WorldBufferRenderer);
        }
        return NarutoTV.COMPAT.shaderUsing();
    }

    public void swap() {
        this.validateThread();
        this.forEach(AbstractRenderer::shutdown);
        boolean isImageRenderer = this.isImageRenderer();
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : this.renderers.values()) {
            inDimension.replaceAll((pos, renderer) -> {
                BlockScreen screen = ((BindScreen)renderer).screen();
                return isImageRenderer ? new WorldBufferRenderer(screen) : new WorldImageRenderer(screen);
            });
        }
    }

    public void forEach(@NotNull Consumer<AbstractRenderer<?>> action) {
        this.validateThread();
        for (Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension : this.renderers.values()) {
            for (AbstractRenderer<?> renderer : inDimension.values()) {
                action.accept(renderer);
            }
        }
    }

    public void reload() {
        Minecraft.getInstance().execute(() -> this.forEach((renderer) -> {
            renderer.shutdown();
            renderer.setup(0D);
        }));
    }

    private void logOutClean(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft.getInstance().execute(() -> {
            this.forEach(AbstractRenderer::shutdown);
            this.renderers.clear();
        });
    }

    private void tick(TickEvent.@NotNull RenderTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            if (this.renderers.isEmpty()) return;
            this.forEach(AbstractRenderer::render);
        }
    }

    private void renderLevel(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        assert level != null;

        if (player == null) return;

        ResourceLocation dimension = level.dimension().location();
        Object2ObjectMap<Vec3, AbstractRenderer<?>> inDimension = this.renderers.get(dimension);

        if (inDimension == null || inDimension.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();

        for (AbstractRenderer<?> renderer : inDimension.values()) {
            BlockScreen screen = ((BindScreen)renderer).screen();
            if (screen.tooFar(player)) continue;

            poseStack.pushPose();
            poseStack.translate(-camera.x, - camera.y, - camera.z);

            if (renderer instanceof WorldBufferRenderer bufferRenderer) {
                WorldGLEngine engine = bufferRenderer.engine();
                if (engine != null) engine.render(poseStack, camera);
            } else if (renderer instanceof WorldImageRenderer imageRenderer) {
                imageRenderer.render(poseStack, bufferSource, camera);
            } else {
                throw new UnsupportedOperationException("Invalid renderer type: " + renderer.getClass().getSimpleName());
            }

            poseStack.popPose();
        }
    }

    private void validateThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new UnsupportedOperationException("Invalid thread: " + Thread.currentThread().getName());
    }
}
