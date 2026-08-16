package me.kall.narutotv.network.packet.cape;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.network.NarutoPackets;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class CapeSyncPacket implements CustomPacketPayload {
    private final Map<UUID, String> capes;

    public CapeSyncPacket(Map<UUID, String> capes) {
        this.capes = capes;
    }

    public CapeSyncPacket(@NotNull FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<UUID, String> capes = new Object2ObjectOpenHashMap<>(size);
        for (int i = 0; i < size; i++) capes.put(buffer.readUUID(), buffer.readUtf());
        this.capes = capes;
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.capes.size());
        for (Map.Entry<UUID, String> entry : this.capes.entrySet()) {
            buffer.writeUUID(entry.getKey());
            buffer.writeUtf(entry.getValue());
        }
    }

    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> this.capes.forEach((uuid, relPath) -> ClientCapes.add(new Cape(uuid, relPath)).ifPresent(CapeTV.DEATH)));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return NarutoPackets.CAPE_SYNC_PACKET_TYPE;
    }
}