package me.kall.narutotv.data.world.wall;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.network.packet.wall.WallDeathPacket;
import me.kall.narutotv.network.packet.wall.WallSyncPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class SavedWalls extends SavedData {
    private static final String SCREENS_KEY = "Walls";
    private static final String DIMENSION_KEY = "Dimension";

    public static final String DATA_NAME = "NarutoWalls";

    public static final String CORNERS_KEY = "Corners";
    public static final String LOCAL_SOUND_KEY = "LocalSound";
    public static final String VIDEO_KEY = "Video";
    public static final String AUDIO_KEY = "Audio";
    public static final String VOLUME_KEY = "Volume";
    public static final String LIGHT_KEY = "Light";

    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<Wall>> data = new Object2ObjectOpenHashMap<>();

    public void update(@NotNull Wall argSource) {
        ObjectSet<Wall> screens = this.data.computeIfAbsent(argSource.dimension, key -> new ObjectOpenHashSet<>());
        screens.remove(argSource);
        screens.add(argSource);
        this.setDirty();
    }

    public @Nullable Wall get(ResourceLocation dimension, long position) {
        ObjectOpenHashSet<Wall> walls = this.data.get(dimension);
        if (walls == null || walls.isEmpty()) return null;
        for (Wall wall : walls) {
            if (wall.areaInvolved().contains(position)) return wall;
        }
        return null;
    }

    public ObjectOpenHashSet<Wall> getIn(ResourceLocation dimension) {
        return this.data.get(dimension);
    }

    public void remove(ResourceLocation dimension, long position) {
        ObjectOpenHashSet<Wall> screens = this.data.get(dimension);
        if (screens == null || screens.isEmpty()) return;

        ObjectIterator<Wall> iterator = screens.iterator();

        while (iterator.hasNext()) {
            Wall next = iterator.next();
            if (next.borderInvolved().contains(position)) {
                iterator.remove();
                this.setDirty();
                PacketDistributor.sendToAllPlayers(new WallDeathPacket(next));
            }
        }
    }

    public ObjectCollection<ObjectOpenHashSet<Wall>> values() {
        return this.data.values();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        ListTag screensList = new ListTag();
        for (Object2ObjectMap.Entry<ResourceLocation, ObjectOpenHashSet<Wall>> entry : this.data.object2ObjectEntrySet()) {
            ResourceLocation dimension = entry.getKey();
            for (Wall wall : entry.getValue()) {
                CompoundTag screenTag = new CompoundTag();
                screenTag.putString(DIMENSION_KEY, dimension.toString());
                screenTag.putLongArray(CORNERS_KEY, wall.toLongArray());
                screenTag.putString(LOCAL_SOUND_KEY, wall.localSound.toString());
                screenTag.putString(VIDEO_KEY, wall.video);
                screenTag.putString(AUDIO_KEY, wall.audio);
                screenTag.putFloat(VOLUME_KEY, wall.volume);
                screenTag.putBoolean(LIGHT_KEY, wall.light);
                screensList.add(screenTag);
            }
        }
        compoundTag.put(SCREENS_KEY, screensList);
        return compoundTag;
    }

    public static @NotNull SavedWalls load(@NotNull CompoundTag tag) {
        SavedWalls savedWalls = new SavedWalls();

        ListTag screensList = tag.getList(SCREENS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < screensList.size(); index++) {
            CompoundTag screenTag = screensList.getCompound(index);

            ResourceLocation dimension = ResourceLocation.parse(screenTag.getString(DIMENSION_KEY));

            savedWalls.data.computeIfAbsent(dimension, key -> new ObjectOpenHashSet<>()).add(new Wall(screenTag.getLongArray(CORNERS_KEY), dimension, ResourceLocation.parse(screenTag.getString(LOCAL_SOUND_KEY)), screenTag.getString(VIDEO_KEY), screenTag.getString(AUDIO_KEY), screenTag.getFloat(VOLUME_KEY), screenTag.getBoolean(LIGHT_KEY)));
        }

        return savedWalls;
    }

    public static @NotNull SavedWalls get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(SavedWalls::new, (tag, provider) -> load(tag)), DATA_NAME);
    }

    @SubscribeEvent
    public static void sync(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new WallSyncPacket(SavedWalls.get(player.serverLevel()).values()));
        }
    }
}
