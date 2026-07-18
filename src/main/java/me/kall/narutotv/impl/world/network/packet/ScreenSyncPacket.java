package me.kall.narutotv.impl.world.network.packet;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.narutotv.base.data.Paths;
import me.kall.narutotv.base.renderer.AbstractRenderer;
import me.kall.narutotv.impl.world.data.BlockScreen;
import me.kall.narutotv.impl.world.data.client.ClientRenderers;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import me.kall.narutotv.impl.world.util.AudioZipGenerator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ScreenSyncPacket {
    private final ObjectCollection<ObjectOpenHashSet<BlockScreen>> blockScreens;

    public ScreenSyncPacket(ObjectCollection<ObjectOpenHashSet<BlockScreen>> blockScreens) {
        this.blockScreens = blockScreens;
    }

    public ScreenSyncPacket(@NotNull FriendlyByteBuf buffer) {
        int outerSize = buffer.readVarInt();
        this.blockScreens = new ObjectOpenHashSet<>(outerSize);
        for (int outerIndex = 0; outerIndex < outerSize; outerIndex++) {
            int innerSize = buffer.readVarInt();
            ObjectOpenHashSet<BlockScreen> screens = new ObjectOpenHashSet<>();
            for (int innerIndex = 0; innerIndex < innerSize; innerIndex++) {
                screens.add(new BlockScreen(buffer.readLongArray(), buffer.readResourceLocation(), buffer.readResourceLocation()));
            }
            this.blockScreens.add(screens);
        }
    }

    public void encode(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.blockScreens.size());
        this.blockScreens.forEach(screens -> {
            buffer.writeVarInt(screens.size());
            screens.forEach(blockScreen -> {
                buffer.writeLongArray(blockScreen.toLongArray());
                buffer.writeResourceLocation(blockScreen.dimension);
                buffer.writeResourceLocation(blockScreen.localSound);
            });
        });
    }

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            try {
                this.blockScreens.forEach(screens -> screens.forEach(screen -> {
                    Runnable validation = () -> ClientRenderers.getInstance().add(screen).ifPresent(AbstractRenderer::shutdown);
                    if (screen.hasLocalSound()) {
                        AudioZipGenerator.get(Paths.absolute(screen.audio)).generate(validation);
                    } else {
                        validation.run();
                    }
                }));
            } catch (Throwable throwable) {
                ScreenPacket.LOGGER.error("Error handing ScreenDeathPacket", throwable);
            }
        });
    }
}
