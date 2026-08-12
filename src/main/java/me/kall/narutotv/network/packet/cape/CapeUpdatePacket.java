package me.kall.narutotv.network.packet.cape;

import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.data.world.cape.SavedCapes;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Supplier;

public class CapeUpdatePacket {
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

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                ClientCapes.add(new Cape(this.player, this.relPath)).ifPresent(CapeTV.DEATH);
            } else {
                SavedCapes.get(player.serverLevel()).add(this.player, this.relPath);

                UUID excluded = player.getUUID();

                for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                    if (other.getUUID().equals(excluded)) continue;
                    NarutoPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> other), this);
                }
            }
        });
    }
}
