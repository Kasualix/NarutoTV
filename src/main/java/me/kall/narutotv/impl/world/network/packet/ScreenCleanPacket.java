package me.kall.narutotv.impl.world.network.packet;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.ext.ScreenLevel;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenCleanPacket extends ScreenPacket {
    public ScreenCleanPacket(BlockScreen blockScreen) {
        super(blockScreen);
    }

    public ScreenCleanPacket(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                ServerLevel level = player.serverLevel();

                LongSet areaInvolved = this.blockScreen.areaInvolved();
                LongSet borderInvolved = this.blockScreen.borderInvolved();
                areaInvolved.removeIf(borderInvolved::contains);

                ScreenLevel.setCleaning(level, true);
                areaInvolved.forEach(position -> level.removeBlock(BlockPos.of(position), false));
                ScreenLevel.setCleaning(level, false);
            } catch (Throwable throwable) {
                LOGGER.error("Error handling ScreenCleanPacket", throwable);
            }
        });
    }
}
