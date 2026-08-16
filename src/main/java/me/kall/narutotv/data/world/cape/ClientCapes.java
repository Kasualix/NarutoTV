package me.kall.narutotv.data.world.cape;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.compat.CompatCenter;
import me.kall.narutotv.produce.util.LifetimeController;
import me.kall.narutotv.renderer.ImageFrameRenderer;
import me.kall.narutotv.world.CapeTV;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(value = Dist.CLIENT, modid = NarutoTV.MOD_ID)
public class ClientCapes {
    private static final Object2ObjectMap<UUID, Pair<Cape, CapeTV<?>>> DATA = new Object2ObjectOpenHashMap<>();

    public static @Nullable CapeTV<?> get(UUID player) {
        Pair<Cape, CapeTV<?>> pair = DATA.get(player);
        if (pair == null) return null;
        return pair.getSecond();
    }

    public static @Nullable CapeTV<?> get(@NotNull Cape cape) {
        return get(cape.player());
    }

    public static @NotNull Optional<CapeTV<?>> remove(@NotNull Cape cape) {
        return remove(cape.player());
    }

    public static @NotNull Optional<CapeTV<?>> remove(UUID player) {
        Pair<Cape, CapeTV<?>> removed = DATA.remove(player);
        if (removed == null) return Optional.empty();
        return Optional.of(removed.getSecond());
    }

    public static @NotNull Optional<CapeTV<?>> add(Cape cape) {
        if (ClientCapes.isCompatMode()) {
            return add(new CapeTV.Image(cape));
        } else {
            return add(new CapeTV.Buffer(cape));
        }
    }

    public static @NotNull Optional<CapeTV<?>> add(@NotNull CapeTV<?> tv) {
        Cape cape = tv.cape;
        UUID uuid = cape.player();

        Pair<Cape, CapeTV<?>> outdated = DATA.put(uuid, Pair.of(cape, tv));
        if (outdated == null) return Optional.empty();
        return Optional.of(outdated.getSecond());
    }

    public static boolean isCompatMode() {
        if (DATA.isEmpty()) return CompatCenter.shaderUsing();
        for (Pair<Cape, CapeTV<?>> value : DATA.values()) {
            return value.getSecond().renderer instanceof ImageFrameRenderer;
        }
        return CompatCenter.shaderUsing();
    }

    public static boolean isEmpty() {
        return DATA.isEmpty();
    }

    public static void swap() {
        boolean isCompatMode = ClientCapes.isCompatMode();

        ObjectSet<CapeTV<?>> latestSet = new ObjectOpenHashSet<>();

        for (Pair<Cape, CapeTV<?>> capeEntry : DATA.values()) {
            Cape cape = capeEntry.getFirst();
            CapeTV<?> outdated = capeEntry.getSecond();

            LifetimeController life = outdated.video == null ? null : outdated.video.life();

            CapeTV<?> latest = isCompatMode ? new CapeTV.Buffer(cape) : new CapeTV.Image(cape);

            latest.mediaArgs = outdated.mediaArgs;

            latest.setup(life == null ? 0D : life.sinceSetupSec());
            outdated.shutdownEntire(false);
            latestSet.add(latest);
        }

        DATA.clear();
        latestSet.forEach(ClientCapes::add);
    }

    public static void setCompatMode() {
        if (!ClientCapes.isCompatMode()) swap();
    }

    public static void forEach(Consumer<CapeTV<?>> action) {
        for (Pair<Cape, CapeTV<?>> value : DATA.values()) {
            action.accept(value.getSecond());
        }
    }

    @SubscribeEvent
    public static void logOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCapes.forEach(CapeTV.DEATH);
        DATA.clear();
    }
}
