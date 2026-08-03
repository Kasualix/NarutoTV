package me.kall.narutotv.impl.world.data.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import me.kall.narutotv.app.util.LifetimeController;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.impl.world.cape.CapeBufferRenderer;
import me.kall.narutotv.impl.world.cape.CapeImageRenderer;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.cape.CapeSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientVideoCapes {
    private static final Object2ObjectMap<UUID, VideoCape> DATA = new Object2ObjectOpenHashMap<>();

    public static @Nullable VideoCape get(UUID uuid) {
        return DATA.get(uuid);
    }

    public static void register(UUID uuid, String absVideoPath) {
        CompletableFuture.supplyAsync(() -> AppInstances.ffmpeg().read(absVideoPath, null), NarutoTV.io())
                .whenCompleteAsync((mediaArgs, throwable) -> {
                    if (throwable != null) throw new IllegalStateException(throwable);

                    if (DATA.containsKey(uuid)) {
                        VideoCape old = DATA.remove(uuid);
                        AbstractRenderer<?> oldRenderer = old.renderer();
                        if (oldRenderer != null) oldRenderer.shutdown();
                        old.narutoTexture.close();
                    }

                    ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "cape_" + System.currentTimeMillis() + "_" + System.nanoTime());
                    DATA.put(uuid, new VideoCape(mediaArgs, NarutoTexture.of(textureLocation, new DynamicTexture(256, 128, false))));
                    NarutoPackets.INSTANCE.sendToServer(new CapeSavePacket(uuid, absVideoPath));
                }, Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void logOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DATA.values().forEach(videoCape -> {
            AbstractRenderer<?> renderer = videoCape.renderer();
            if (renderer != null) renderer.shutdown();
            videoCape.narutoTexture.close();
        });
        DATA.clear();
    }

    public static boolean isImageRenderer() {
        if (DATA.isEmpty()) return CompatCenter.shaderUsing();
        for (VideoCape cape : DATA.values()) {
            AbstractRenderer<?> renderer = cape.renderer();
            if (renderer != null) return !(renderer instanceof CapeBufferRenderer);
        }
        return CompatCenter.shaderUsing();
    }

    public static void swap() {
        boolean targetIsBuffer = isImageRenderer();
        for (VideoCape cape : DATA.values()) {
            AbstractRenderer<?> outdated = cape.renderer();
            LifetimeController life = outdated != null ? outdated.life() : null;

            AbstractRenderer<?> latest = targetIsBuffer ? new CapeBufferRenderer(cape) : new CapeImageRenderer(cape);
            cape.setRenderer(latest);
            latest.setup(life != null ? life.sinceSetupSec() : 0D);

            if (outdated != null) outdated.shutdown();
        }
    }

    public static void compat() {
        if (isImageRenderer()) return;
        swap();
    }

    public static final class VideoCape {
        public final MediaArgs mediaArgs;
        public final NarutoTexture narutoTexture;

        private @Nullable AbstractRenderer<?> renderer;

        public VideoCape(MediaArgs mediaArgs, NarutoTexture narutoTexture) {
            this.mediaArgs = mediaArgs;
            this.narutoTexture = narutoTexture;
        }

        public MediaArgs mediaArgs() {
            return this.mediaArgs;
        }

        public NarutoTexture narutoTexture() {
            return this.narutoTexture;
        }

        public @Nullable AbstractRenderer<?> renderer() {
            return this.renderer;
        }

        public void setRenderer(@NotNull AbstractRenderer<?> renderer) {
            this.renderer = renderer;
        }
    }
}