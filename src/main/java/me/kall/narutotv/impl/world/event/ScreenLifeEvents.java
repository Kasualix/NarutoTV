package me.kall.narutotv.impl.world.event;

import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Executor;
import me.kall.narutotv.impl.config.NarutoConfig;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.BlockScreens;
import me.kall.narutotv.impl.world.data.Displayers;
import me.kall.narutotv.impl.world.ext.ScreenLevel;
import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.packet.ScreenLifePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ScreenLifeEvents {
    public static void register(@NotNull IEventBus forgeBus) {
        ScreenLifeEvents screenLifeEvents = new ScreenLifeEvents();
        forgeBus.addListener(screenLifeEvents::blockChange);
        forgeBus.addListener(screenLifeEvents::rightClick);
    }

    private final Object2ObjectMap<ResourceLocation, Object2ObjectMap<UUID, BlockPos>> blockCorners = new Object2ObjectOpenHashMap<>();

    private void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        ResourceLocation dimension = event.dim();
        long block = event.blockPos();
        if (Displayers.isDisplayer(event.oldState()) && !Displayers.isDisplayer(event.newState())) {
            Executor.run(() -> {
                if (ScreenLevel.isCleaning(level)) return;
                BlockScreens.get(level).remove(dimension, block);
            });
        }
    }

    private void rightClick(PlayerInteractEvent.@NotNull RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!player.isShiftKeyDown()) return;
        if (!RegistryEntry.get(event.getItemStack()).equals(NarutoConfig.Server.builder())) return;

        BlockPos pos = event.getPos();

        if (Displayers.nonDisplayer(level, pos.asLong())) return;

        ResourceLocation dimension = level.dimension().location();
        UUID uuid = player.getUUID();

        Object2ObjectMap<UUID, BlockPos> perCreator = this.blockCorners.computeIfAbsent(dimension, key -> new Object2ObjectOpenHashMap<>());

        if (perCreator.containsKey(uuid)) {
            BlockPos last = perCreator.get(uuid);
            perCreator.remove(uuid);

            BlockScreen built = build(level, last, pos);
            if (built == null) {
                System.err.println("Failed to build screen");
                return;
            }

            BlockScreens.get(level).update(built);
            NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(built));
        } else {
            perCreator.put(uuid, pos);
        }
    }


    @Nullable
    public static BlockScreen build(@NotNull ServerLevel level, @NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2) {
        LongList bottomEdge = BlockScreen.getLine(bottomCorner1, bottomCorner2);

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

                    for (int length = 1; length <= maxLength; length++) {
                        BlockPos topCorner1 = bottomCorner1.offset(hx * length, hy * length, hz * length);
                        BlockPos topCorner2 = bottomCorner2.offset(hx * length, hy * length, hz * length);

                        LongList leftEdge = BlockScreen.getLine(bottomCorner1, topCorner1);
                        long anyIncorrect = Long.MAX_VALUE;
                        for (long pos : leftEdge) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                anyIncorrect = pos;
                                break;
                            }
                        }
                        if (anyIncorrect != Long.MAX_VALUE) break;

                        LongList rightEdge = BlockScreen.getLine(bottomCorner2, topCorner2);
                        for (long pos : rightEdge) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                anyIncorrect = pos;
                                break;
                            }
                        }
                        if (anyIncorrect != Long.MAX_VALUE) break;

                        LongList topEdge = BlockScreen.getLine(topCorner1, topCorner2);
                        for (long pos : topEdge) {
                            if (Displayers.nonDisplayer(level, pos)) {
                                anyIncorrect = pos;
                                break;
                            }
                        }
                        if (anyIncorrect != Long.MAX_VALUE) continue;

                        int nx = dy * hz - dz * hy;
                        int ny = dz * hx - dx * hz;
                        int nz = dx * hy - dy * hx;

                        if (!(ny == 0) && !(nx == 0 && nz == 0)) continue;

                        return new BlockScreen(bottomCorner1, bottomCorner2, topCorner1, topCorner2, dimension);
                    }
                }
            }
        }

        return null;
    }
}
