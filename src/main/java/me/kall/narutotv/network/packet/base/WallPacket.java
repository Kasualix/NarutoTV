package me.kall.narutotv.network.packet.base;

import me.kall.narutotv.data.world.Wall;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class WallPacket {
    protected final Wall wall;

    public WallPacket(Wall wall) {
        this.wall = wall;
    }

    public WallPacket(@NotNull FriendlyByteBuf buffer) {
        this(new Wall(buffer.readLongArray(), buffer.readResourceLocation(), buffer.readResourceLocation(), buffer.readUtf(), buffer.readUtf(), buffer.readFloat()));
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.wall.toLongArray());
        buffer.writeResourceLocation(this.wall.dimension);
        buffer.writeResourceLocation(this.wall.localSound);
        buffer.writeUtf(this.wall.video);
        buffer.writeUtf(this.wall.audio);
        buffer.writeFloat(this.wall.volume);
    }

    public abstract void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier);
}
