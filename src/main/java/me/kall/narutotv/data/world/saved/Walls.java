package me.kall.narutotv.data.world.saved;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.WallDeathPacket;
import me.kall.narutotv.network.packet.WallSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class Walls extends SavedData {
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
                NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new WallDeathPacket(next));
            }
        }
    }

    public ObjectCollection<ObjectOpenHashSet<Wall>> values() {
        return this.data.values();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
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

    public static @NotNull Walls load(@NotNull CompoundTag tag) {
        Walls walls = new Walls();

        ListTag screensList = tag.getList(SCREENS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < screensList.size(); index++) {
            CompoundTag screenTag = screensList.getCompound(index);

            ResourceLocation dimension = ResourceLocation.parse(screenTag.getString(DIMENSION_KEY));

            walls.data.computeIfAbsent(dimension, key -> new ObjectOpenHashSet<>()).add(new Wall(screenTag.getLongArray(CORNERS_KEY), dimension, ResourceLocation.parse(screenTag.getString(LOCAL_SOUND_KEY)), screenTag.getString(VIDEO_KEY), screenTag.getString(AUDIO_KEY), screenTag.getFloat(VOLUME_KEY), screenTag.getBoolean(LIGHT_KEY)));
        }

        return walls;
    }

    public static @NotNull Walls get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Walls::load, Walls::new, DATA_NAME);
    }

    @SubscribeEvent
    public static void syncScreens(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new WallSyncPacket(Walls.get(level).values()));
        }
    }
}
