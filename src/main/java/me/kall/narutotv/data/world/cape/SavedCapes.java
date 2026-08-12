package me.kall.narutotv.data.world.cape;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.cape.CapeSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class SavedCapes extends SavedData {
    private static final String DATA_NAME = "NarutoSavedCapes";
    private static final String TAG_LIST = "VideoCapes";

    public final Map<UUID, String> data = new Object2ObjectOpenHashMap<>();

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag list = new CompoundTag();
        for (Map.Entry<UUID, String> entry : this.data.entrySet()) {
            list.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TAG_LIST, list);
        return tag;
    }

    public void add(UUID uuid, String relPath) {
        this.data.put(uuid, relPath);
        this.setDirty();
    }

    public static @NotNull SavedCapes load(@NotNull CompoundTag tag) {
        SavedCapes data = new SavedCapes();
        CompoundTag list = tag.getCompound(TAG_LIST);
        for (String key : list.getAllKeys()) data.data.put(UUID.fromString(key), list.getString(key));
        return data;
    }

    public static @NotNull SavedCapes get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SavedCapes::load, SavedCapes::new, DATA_NAME);
    }

    @SubscribeEvent
    public static void sync(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new CapeSyncPacket(SavedCapes.get(player.serverLevel()).data));
        }
    }
}
