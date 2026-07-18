package me.kall.narutotv.impl.world.data;

import it.unimi.dsi.fastutil.objects.*;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.ScreenDeathPacket;
import me.kall.narutotv.impl.world.network.packet.ScreenSyncPacket;
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

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class BlockScreens extends SavedData {
    private static final String SCREENS_KEY = "Screens";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String CORNERS_KEY = "Corners";
    private static final String LOCAL_SOUND_KEY = "LocalSound";

    private final Object2ObjectOpenHashMap<ResourceLocation, ObjectOpenHashSet<BlockScreen>> data = new Object2ObjectOpenHashMap<>();

    public void update(@NotNull BlockScreen argSource, boolean removeLast) {
        ObjectSet<BlockScreen> screens = this.data.computeIfAbsent(argSource.dimension, key -> new ObjectOpenHashSet<>());
        if (removeLast) screens.remove(argSource);
        screens.add(argSource);
        this.setDirty();
    }

    public void remove(ResourceLocation dimension, long position) {
        ObjectOpenHashSet<BlockScreen> screens = this.data.get(dimension);
        if (screens == null || screens.isEmpty()) return;

        ObjectIterator<BlockScreen> iterator = screens.iterator();

        while (iterator.hasNext()) {
            BlockScreen next = iterator.next();
            if (next.borderInvolved().contains(position)) {
                iterator.remove();
                this.setDirty();
                NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenDeathPacket(next));
            }
        }
    }

    public ObjectCollection<ObjectOpenHashSet<BlockScreen>> values() {
        return this.data.values();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag) {
        ListTag screensList = new ListTag();
        for (Object2ObjectMap.Entry<ResourceLocation, ObjectOpenHashSet<BlockScreen>> entry : this.data.object2ObjectEntrySet()) {
            ResourceLocation dimension = entry.getKey();
            for (BlockScreen blockScreen : entry.getValue()) {
                CompoundTag screenTag = new CompoundTag();
                screenTag.putString(DIMENSION_KEY, dimension.toString());
                screenTag.putLongArray(CORNERS_KEY, blockScreen.toLongArray());
                screenTag.putString(LOCAL_SOUND_KEY, blockScreen.localSound.toString());
                screensList.add(screenTag);
            }
        }
        compoundTag.put(SCREENS_KEY, screensList);
        return compoundTag;
    }

    public static @NotNull BlockScreens load(@NotNull CompoundTag tag) {
        BlockScreens blockScreens = new BlockScreens();

        ListTag screensList = tag.getList(SCREENS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < screensList.size(); index++) {
            CompoundTag screenTag = screensList.getCompound(index);

            ResourceLocation dimension = ResourceLocation.parse(screenTag.getString(DIMENSION_KEY));

            blockScreens.data.computeIfAbsent(dimension, key -> new ObjectOpenHashSet<>()).add(new BlockScreen(screenTag.getLongArray(CORNERS_KEY), dimension, ResourceLocation.parse(screenTag.getString(LOCAL_SOUND_KEY))));
        }

        return blockScreens;
    }

    public static @NotNull BlockScreens get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BlockScreens::load, BlockScreens::new, "NarutoScreens");
    }

    @SubscribeEvent
    public static void syncScreens(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ScreenSyncPacket(BlockScreens.get(level).values()));
        }
    }
}
