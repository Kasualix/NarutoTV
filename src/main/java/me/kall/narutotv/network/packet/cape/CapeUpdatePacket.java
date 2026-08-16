package me.kall.narutotv.network.packet.cape;

import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.data.world.cape.SavedCapes;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CapeUpdatePacket implements CustomPacketPayload {
    private final UUID player;
    private final String relPath;

    public CapeUpdatePacket(UUID player, String relPath) {
        this.player = player;
        this.relPath = relPath;
    }

    public CapeUpdatePacket(@NotNull FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readUtf());
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUUID(this.player);
        buffer.writeUtf(this.relPath);
    }

    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer)) {
                ClientCapes.add(new Cape(this.player, this.relPath)).ifPresent(CapeTV.DEATH);
            } else {
                SavedCapes.get(((ServerPlayer) player).serverLevel()).add(this.player, this.relPath);

                UUID excluded = player.getUUID();

                for (ServerPlayer other : ((ServerPlayer) player).server.getPlayerList().getPlayers()) {
                    if (other.getUUID().equals(excluded)) continue;
                    PacketDistributor.sendToPlayer(other, this);
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return NarutoPackets.CAPE_UPDATE_PACKET_TYPE;
    }
}
