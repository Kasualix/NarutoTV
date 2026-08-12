package me.kall.narutotv.network.packet.cape;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.kall.narutotv.data.world.cape.Cape;
import me.kall.narutotv.data.world.cape.ClientCapes;
import me.kall.narutotv.world.CapeTV;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CapeSyncPacket {
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

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().setPacketHandled(true);
        contextSupplier.get().enqueueWork(() -> this.capes.forEach((uuid, relPath) -> ClientCapes.add(new Cape(uuid, relPath)).ifPresent(CapeTV.DEATH)));
    }
}