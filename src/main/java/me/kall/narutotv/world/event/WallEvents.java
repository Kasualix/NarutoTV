package me.kall.narutotv.world.event;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Executor;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.config.NarutoConfig;
import me.kall.narutotv.data.world.Wall;
import me.kall.narutotv.data.world.saved.Displayers;
import me.kall.narutotv.data.world.saved.Walls;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.network.packet.WallConfigPacket;
import me.kall.narutotv.network.packet.WallLifePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class WallEvents {
    private static final Object2ObjectMap<ResourceLocation, Object2ObjectMap<UUID, BlockPos>> BLOCK_CORNERS = new Object2ObjectOpenHashMap<>();

    private static int interval = 0;

    private static final Logger LOGGER = LogManager.getLogger(WallEvents.class);

    @SubscribeEvent
    public static void tickServer(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.END)) {
            if (interval == 0) return;
            interval--;
        }
    }

    @SubscribeEvent
    public static void removeScreen(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        ResourceLocation dimension = event.dim();
        long block = event.blockPos();
        if (Displayers.isDisplayer(event.oldState()) && !Displayers.isDisplayer(event.newState())) {
            Executor.run(() -> {
                if (Displayers.Cleaner.isCleaning(level)) return;
                Walls.get(level).remove(dimension, block);
            });
        }
    }

    @SubscribeEvent
    public static void configScreen(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!player.isShiftKeyDown()) return;
        if (!player.getMainHandItem().isEmpty()) return;
        Wall wall = Walls.get(level).get(level.dimension().location(), event.getPos().asLong());
        if (wall == null) return;
        NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new WallConfigPacket(wall));
    }

    @SubscribeEvent
    public static void buildScreen(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (interval != 0) return;
        if (!player.isShiftKeyDown()) return;
        if (!RegistryEntry.get(event.getItemStack()).toString().equals(NarutoConfig.builder())) return;

        interval = 10;

        BlockPos pos = event.getPos();

        if (Displayers.nonDisplayer(level, pos.asLong())) return;

        ResourceLocation dimension = level.dimension().location();
        UUID uuid = player.getUUID();

        Object2ObjectMap<UUID, BlockPos> perCreator = BLOCK_CORNERS.computeIfAbsent(dimension, key -> new Object2ObjectOpenHashMap<>());

        if (perCreator.containsKey(uuid)) {
            BlockPos last = perCreator.get(uuid);
            perCreator.remove(uuid);
            player.displayClientMessage(WallEvents.secCorner(pos), false);

            Wall built;

            try {
                built = build(level, last, pos);
            } catch (Exception exception) {
                LOGGER.error("Exception building screen.", exception);
                player.displayClientMessage(WallEvents.fail(last, pos), false);
                return;
            }

            if (built == null) {
                player.displayClientMessage(WallEvents.none(last, pos), false);
                return;
            }

            player.displayClientMessage(WallEvents.success(last, pos), false);
            Walls.get(level).update(built);
            NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new WallLifePacket(built));
        } else {
            perCreator.put(uuid, pos);
            player.displayClientMessage(WallEvents.firstCorner(pos), false);
        }
    }

    private static @NotNull Component firstCorner(@NotNull BlockPos pos) {
        return Component.translatable("message.narutotv.corner.first", pos.toShortString()).withStyle(ChatFormatting.GREEN);
    }

    private static @NotNull Component secCorner(@NotNull BlockPos pos) {
        return Component.translatable("message.narutotv.corner.second", pos.toShortString()).withStyle(ChatFormatting.GREEN);
    }

    private static @NotNull Component fail(@NotNull BlockPos last, @NotNull BlockPos now) {
        return Component.translatable("message.narutotv.corner.fail", last.toShortString(), now.toShortString()).withStyle(ChatFormatting.DARK_RED);
    }

    private static @NotNull Component none(@NotNull BlockPos last, @NotNull BlockPos now) {
        return Component.translatable("message.narutotv.corner.none", last.toShortString(), now.toShortString()).withStyle(ChatFormatting.YELLOW);
    }

    private static @NotNull Component success(@NotNull BlockPos last, @NotNull BlockPos now) {
        return Component.translatable("message.narutotv.corner.success", last.toShortString(), now.toShortString()).withStyle(ChatFormatting.GREEN);
    }

    @Nullable
    private static Wall build(@NotNull ServerLevel level, @NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2) {
        LongList bottomEdge = Wall.getLine(bottomCorner1, bottomCorner2);

        for (long pos : bottomEdge) {
            if (Displayers.nonDisplayer(level, pos)) {
                throw new IllegalArgumentException("Bottom edge contains non-displayer block at " + BlockPos.of(pos));
            }
        }

        int dx = bottomCorner2.getX() - bottomCorner1.getX();
        int dy = bottomCorner2.getY() - bottomCorner1.getY();
        int dz = bottomCorner2.getZ() - bottomCorner1.getZ();

        ResourceLocation dimension = level.dimension().location();

        int maxLength = level.getMaxBuildHeight() - level.getMinBuildHeight();

        for (int hx = -1; hx <= 1; hx++) {
            for (int hy = -1; hy <= 1; hy++) {
                for (int hz = -1; hz <= 1; hz++) {
                    if (hx == 0 && hy == 0 && hz == 0) continue;
                    if (hx * dx + hy * dy + hz * dz != 0) continue;

                    int nx = dy * hz - dz * hy;
                    int ny = dz * hx - dx * hz;
                    int nz = dx * hy - dy * hx;
                    if (!(ny == 0) && !(nx == 0 && nz == 0)) continue;

                    BlockPos bestTop1 = null;
                    BlockPos bestTop2 = null;

                    for (int length = 1; length <= maxLength; length++) {
                        BlockPos topCorner1 = bottomCorner1.offset(hx * length, hy * length, hz * length);
                        BlockPos topCorner2 = bottomCorner2.offset(hx * length, hy * length, hz * length);

                        boolean leftOk = true;
                        for (long pos : Wall.getLine(bottomCorner1, topCorner1)) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                leftOk = false;
                                break;
                            }
                        }
                        if (!leftOk) break;

                        boolean rightOk = true;
                        for (long pos : Wall.getLine(bottomCorner2, topCorner2)) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                rightOk = false;
                                break;
                            }
                        }
                        if (!rightOk) break;

                        boolean topOk = true;
                        for (long pos : Wall.getLine(topCorner1, topCorner2)) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                topOk = false;
                                break;
                            }
                        }
                        if (!topOk) continue;

                        bestTop1 = topCorner1;
                        bestTop2 = topCorner2;
                    }

                    if (bestTop1 != null) return new Wall(bottomCorner1, bottomCorner2, bestTop1, bestTop2, dimension);
                }
            }
        }

        return null;
    }
}
