package me.kall.narutotv.impl.world.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.cape.CapeSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class SavedCapePaths extends SavedData {
    private static final String TAG_LIST = "CapePaths";

    private final Object2ObjectMap<UUID, String> data = new Object2ObjectOpenHashMap<>();

    public void add(UUID player, String relPath) {
        this.data.put(player, relPath);
        this.setDirty();
    }

    public static @NotNull SavedCapePaths load(@NotNull CompoundTag tag) {
        SavedCapePaths savedCapePaths = new SavedCapePaths();
        CompoundTag list = tag.getCompound(TAG_LIST);
        for (String key : list.getAllKeys()) {
            savedCapePaths.data.put(UUID.fromString(key), list.getString(key));
        }
        return savedCapePaths;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag list = new CompoundTag();
        this.data.forEach((uuid, string) -> list.putString(uuid.toString(), string));
        tag.put(TAG_LIST, list);
        return tag;
    }

    public static @NotNull SavedCapePaths get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SavedCapePaths::load, SavedCapePaths::new, "NarutoVideoCapes");
    }

    @SubscribeEvent
    public static void syncCapes(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            SavedCapePaths savedCapePaths = get(level);
            List<UUID> players = new ArrayList<>();
            List<String> capes = new ArrayList<>();
            savedCapePaths.data.forEach((key, value) -> {
                players.add(key);
                capes.add(value);
            });
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new CapeSyncPacket(players, capes));
        }
    }
}
