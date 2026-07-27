package me.kall.narutotv.impl.world.data.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.app.file.AppInstances;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class VideoCapes {
    private static final Object2ObjectMap<UUID, VideoCape> DATA = new Object2ObjectOpenHashMap<>();

    public static @Nullable ResourceLocation get(UUID uuid) {
        VideoCape cape = DATA.get(uuid);
        return cape == null ? null : cape.narutoTexture.textureLocation;
    }

    public static void register(UUID uuid, String absVideoPath) {
        CompletableFuture.supplyAsync(() -> AppInstances.ffmpeg().read(absVideoPath, null), NarutoTV.io())
                .whenCompleteAsync((mediaArgs, throwable) -> {
                    if (throwable != null) throw new IllegalStateException(throwable);
                    if (DATA.containsKey(uuid)) DATA.remove(uuid).narutoTexture.close();

                    ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "cape_" + System.currentTimeMillis());
                    DATA.put(uuid, new VideoCape(mediaArgs, NarutoTexture.of(textureLocation, new DynamicTexture(256, 128, false))));
                }, Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void logOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DATA.values().forEach(videoCape -> videoCape.narutoTexture.close());
        DATA.clear();
    }

    public record VideoCape(MediaArgs mediaArgs, NarutoTexture narutoTexture) {}
}
