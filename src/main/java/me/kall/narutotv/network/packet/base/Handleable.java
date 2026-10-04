package me.kall.narutotv.network.packet.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class Handleable {
    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().setPacketHandled(true);
        this.handle(contextSupplier.get().getSender(), contextSupplier.get().getDirection().equals(NetworkDirection.PLAY_TO_CLIENT));
    }

    public abstract void handle(@Nullable ServerPlayer player, boolean s2c);
}
