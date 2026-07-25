package me.kall.narutotv.impl.world.network.packet.base;

import me.kall.narutotv.impl.world.data.BlockScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ScreenPacket {
    public static final Logger LOGGER = LogManager.getLogger(ScreenPacket.class);

    protected final BlockScreen blockScreen;

    public ScreenPacket(BlockScreen blockScreen) {
        this.blockScreen = blockScreen;
    }

    public ScreenPacket(@NotNull FriendlyByteBuf buffer) {
        this(new BlockScreen(buffer.readLongArray(), buffer.readResourceLocation(), buffer.readResourceLocation(), buffer.readUtf(), buffer.readUtf(), buffer.readFloat()));
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeLongArray(this.blockScreen.toLongArray());
        buffer.writeResourceLocation(this.blockScreen.dimension);
        buffer.writeResourceLocation(this.blockScreen.localSound);
        buffer.writeUtf(this.blockScreen.video);
        buffer.writeUtf(this.blockScreen.audio);
        buffer.writeFloat(this.blockScreen.volume);
    }

    public abstract void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier);
}
