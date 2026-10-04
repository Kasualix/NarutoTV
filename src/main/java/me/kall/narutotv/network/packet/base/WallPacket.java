package me.kall.narutotv.network.packet.base;

import me.kall.narutotv.data.world.wall.Wall;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public abstract class WallPacket extends Handleable {
    protected final Wall wall;

    public WallPacket(Wall wall) {
        this.wall = wall;
    }

    public WallPacket(@NotNull FriendlyByteBuf buffer) {
        this(new Wall(buffer.readLongArray(), buffer.readResourceLocation(), buffer.readResourceLocation(), buffer.readUtf(), buffer.readUtf(), buffer.readFloat(), buffer.readBoolean()));
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.wall.toLongArray());
        buffer.writeResourceLocation(this.wall.dimension);
        buffer.writeResourceLocation(this.wall.localSound);
        buffer.writeUtf(this.wall.video);
        buffer.writeUtf(this.wall.audio);
        buffer.writeFloat(this.wall.volume);
        buffer.writeBoolean(this.wall.light);
    }
}
