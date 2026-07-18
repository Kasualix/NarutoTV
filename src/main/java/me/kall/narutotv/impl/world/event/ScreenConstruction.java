package me.kall.narutotv.impl.world.event;

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
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScreenConstruction {
    public static void register(@NotNull IEventBus forgeBus) {
        ScreenConstruction screenConstruction = new ScreenConstruction();
        forgeBus.addListener(screenConstruction::blockChange);
        forgeBus.addListener(screenConstruction::rightClick);
    }

    private final Object2ObjectMap<ResourceLocation, Object2ObjectMap<UUID, BlockPos>> blockCorners = new Object2ObjectOpenHashMap<>();

    private void blockChange(@NotNull BlockChangeEvent event) {
        ServerLevel level = event.level();
        ResourceLocation dimension = event.dim();
        long block = event.blockPos();
        if (Displayers.isDisplayer(event.oldState())) {
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

        if (!Displayers.isDisplayer(level, pos.asLong())) return;

        ResourceLocation dimension = level.dimension().location();
        UUID uuid = player.getUUID();

        Object2ObjectMap<UUID, BlockPos> blockCorner = this.blockCorners.computeIfAbsent(dimension, key -> new Object2ObjectOpenHashMap<>());

        if (blockCorner.containsKey(uuid)) {
            BlockPos lastCorner = blockCorner.get(uuid);
            blockCorner.remove(uuid);

            BlockScreen built = Detector.build(level, lastCorner, pos);

            BlockScreens.get(level).update(built, false);
            NarutoPackets.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScreenLifePacket(built));
        } else {
            blockCorner.put(uuid, pos);
        }
    }

    public static final class Detector {
        @NotNull
        public static BlockScreen build(@NotNull ServerLevel level, @NotNull BlockPos bottomCorner1, @NotNull BlockPos bottomCorner2) {
            MutableBlockPos m1 = new MutableBlockPos();
            MutableBlockPos m2 = new MutableBlockPos();
            MutableBlockPos cursor = new MutableBlockPos();

            m1.set(bottomCorner2);
            m1.subtract(bottomCorner1);
            int dx = m1.getX();
            int dy = m1.getY();
            int dz = m1.getZ();

            Direction.Axis edgeAxis;
            int edgeSign;
            int edgeLength;

            if (dx != 0) {
                edgeAxis = Direction.Axis.X;
                edgeSign = Integer.signum(dx);
                edgeLength = Math.abs(dx) + 1;
            } else if (dy != 0) {
                edgeAxis = Direction.Axis.Y;
                edgeSign = Integer.signum(dy);
                edgeLength = Math.abs(dy) + 1;
            } else if (dz != 0) {
                edgeAxis = Direction.Axis.Z;
                edgeSign = Integer.signum(dz);
                edgeLength = Math.abs(dz) + 1;
            } else {
                throw new IllegalArgumentException("bottomCorner1 and bottomCorner2 are the same block");
            }

            Detector.setAxisStep(m1, edgeAxis, edgeSign);

            List<Direction.Axis> perpendicularAxes = new ArrayList<>(2);
            for (Direction.Axis axis : Direction.Axis.VALUES) {
                if (axis != edgeAxis) perpendicularAxes.add(axis);
            }

            for (Direction.Axis axis : perpendicularAxes) {
                for (int sign : new int[]{1, -1}) {
                    Detector.setAxisStep(m2, axis, sign);

                    int sideLength = 0;
                    cursor.set(bottomCorner1);
                    while (Displayers.isDisplayer(level, cursor.asLong())) {
                        sideLength++;
                        cursor.move(m2);
                    }

                    if (sideLength <= 1) continue;

                    if (Detector.isCorrectBorder(level, bottomCorner1, m1, edgeLength, m2, sideLength)) {
                        MutableBlockPos top1 = new MutableBlockPos();
                        top1.set(bottomCorner1);
                        top1.move(m2.getX() * (sideLength - 1), m2.getY() * (sideLength - 1), m2.getZ() * (sideLength - 1));

                        MutableBlockPos top2 = new MutableBlockPos();
                        top2.set(bottomCorner2);
                        top2.move(m2.getX() * (sideLength - 1), m2.getY() * (sideLength - 1), m2.getZ() * (sideLength - 1));

                        return new BlockScreen(bottomCorner1, bottomCorner2, top1.immutable(), top2.immutable(), level.dimension().location());
                    }
                }
            }

            throw new IllegalArgumentException("No valid glass rectangle found for the given bottom corners");
        }

        private static boolean isCorrectBorder(ServerLevel level, BlockPos start, MutableBlockPos edgeStep, int edgeLength, MutableBlockPos sideDir, int sideLength) {
            MutableBlockPos pos = new MutableBlockPos();
            MutableBlockPos neighbor = new MutableBlockPos();

            for (int edgeIndex = 0; edgeIndex < edgeLength; edgeIndex++) {
                pos.set(start);
                pos.move(edgeStep.getX() * edgeIndex, edgeStep.getY() * edgeIndex, edgeStep.getZ() * edgeIndex);
                if (!Displayers.isDisplayer(level, pos.asLong())) return false;

                neighbor.set(pos);
                neighbor.move(-sideDir.getX(), -sideDir.getY(), -sideDir.getZ());
                if (Displayers.isDisplayer(level, neighbor.asLong())) return false;
            }

            int lastSideOff = sideLength - 1;
            for (int edgeIndex = 0; edgeIndex < edgeLength; edgeIndex++) {
                pos.set(start);
                pos.move(edgeStep.getX() * edgeIndex, edgeStep.getY() * edgeIndex, edgeStep.getZ() * edgeIndex);
                pos.move(sideDir.getX() * lastSideOff, sideDir.getY() * lastSideOff, sideDir.getZ() * lastSideOff);
                if (!Displayers.isDisplayer(level, pos.asLong())) return false;

                neighbor.set(pos);
                neighbor.move(sideDir.getX(), sideDir.getY(), sideDir.getZ());
                if (Displayers.isDisplayer(level, neighbor.asLong())) return false;
            }

            for (int sideIndex = 1; sideIndex < sideLength - 1; sideIndex++) {
                pos.set(start);
                pos.move(sideDir.getX() * sideIndex, sideDir.getY() * sideIndex, sideDir.getZ() * sideIndex);
                if (!Displayers.isDisplayer(level, pos.asLong())) return false;
            }

            int lastEdgeOff = edgeLength - 1;
            for (int sideIndex = 1; sideIndex < sideLength - 1; sideIndex++) {
                pos.set(start);
                pos.move(edgeStep.getX() * lastEdgeOff, edgeStep.getY() * lastEdgeOff, edgeStep.getZ() * lastEdgeOff);
                pos.move(sideDir.getX() * sideIndex, sideDir.getY() * sideIndex, sideDir.getZ() * sideIndex);
                if (!Displayers.isDisplayer(level, pos.asLong())) return false;
            }

            return true;
        }

        private static void setAxisStep(@NotNull MutableBlockPos pos, Direction.@NotNull Axis axis, int sign) {
            pos.set(0, 0, 0);
            switch (axis) {
                case X -> pos.setX(sign);
                case Y -> pos.setY(sign);
                case Z -> pos.setZ(sign);
            }
        }
    }
}
