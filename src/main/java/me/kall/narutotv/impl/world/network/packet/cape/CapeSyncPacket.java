package me.kall.narutotv.impl.world.network.packet.cape;

import me.kall.narutotv.impl.world.network.NarutoPackets;
import me.kall.narutotv.impl.world.network.impl.Client;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CapeSyncPacket {
    private final List<UUID> players;
    private final List<String> capes;

    public CapeSyncPacket(@NotNull List<UUID> players, @NotNull List<String> capes) {
        this.players = players;
        this.capes = capes;
    }

    public CapeSyncPacket(@NotNull FriendlyByteBuf buffer) {
        this.players = new ArrayList<>();
        this.capes = new ArrayList<>();
        int size = buffer.readVarInt();
        for (int index = 0; index < size; index++) {
            this.players.add(buffer.readUUID());
            this.capes.add(buffer.readUtf());
        }
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.players.size());
        this.players.forEach(buffer::writeUUID);
        this.capes.forEach(buffer::writeUtf);
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                Client.syncCapes(this.players, this.capes);
            } catch (Throwable throwable) {
                NarutoPackets.LOGGER.error("Error handling CapeSyncPacket", throwable);
            }
        });
    }
}
