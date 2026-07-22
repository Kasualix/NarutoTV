package me.kall.narutotv.impl.world.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.duplicationless.data.ChunkData;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Executor;
import me.kall.duplicationless.util.Positions;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.config.NarutoConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class Displayers extends ChunkData.BlockData {
    private final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<Long>>> data = new Object2ObjectOpenHashMap<>();

    @Override
    public @NotNull Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<Long>>> data() {
        return this.data;
    }

    @Override
    public boolean dataTrustable() {
        return false;
    }

    @Override
    public @Nullable Predicate<BlockState> validation() {
        return null;
    }

    public static @NotNull ChunkData<Long, BlockState> get(ServerLevel level) {
        return get(level, Displayers::new, "NarutoDisplayers");
    }

    public static boolean isDisplayer(BlockState state) {
        return NarutoConfig.Server.displayers().contains(RegistryEntry.get(state));
    }

    public static boolean nonDisplayer(ServerLevel level, long position) {
        return !Displayers.get(level).has(level, Positions.toChunk(position), position);
    }

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.@NotNull Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChunkPos chunk = event.getChunk().getPos();
            Executor.run(() -> get(level).rebuildChunk(level, chunk));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void blockChange(@NotNull BlockChangeEvent event) {
        long chunk = event.chunkPos();
        long block = event.blockPos();

        boolean was = isDisplayer(event.oldState());
        boolean is = isDisplayer(event.newState());

        ServerLevel level = event.level();

        if (was) Executor.run(() -> get(level).remove(level, chunk, block));
        if (is) Executor.run(() -> get(level).add(level, chunk, block));
    }
}
