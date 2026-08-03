package me.kall.narutotv.impl.world.network;

import me.kall.duplicationless.network.Networker;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.world.network.packet.cape.CapeSavePacket;
import me.kall.narutotv.impl.world.network.packet.cape.CapeSyncPacket;
import me.kall.narutotv.impl.world.network.packet.wall.*;
import me.kall.narutotv.impl.world.network.packet.wall.base.WallPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = NarutoTV.MOD_ID)
public class NarutoPackets {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoTV.MOD_ID, "1");
    public static final Logger LOGGER = LogManager.getLogger(WallPacket.class);
    private static int id = 0;

    @SubscribeEvent
    public static void setup(@NotNull FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            INSTANCE.registerMessage(id++, WallCleanPacket.class, WallPacket::encode, WallCleanPacket::new, WallCleanPacket::handle);
            INSTANCE.registerMessage(id++, WallDeathPacket.class, WallPacket::encode, WallDeathPacket::new, WallDeathPacket::handle);
            INSTANCE.registerMessage(id++, WallLifePacket.class, WallPacket::encode, WallLifePacket::new, WallLifePacket::handle);
            INSTANCE.registerMessage(id++, WallSyncPacket.class, WallSyncPacket::encode, WallSyncPacket::new, WallSyncPacket::handle);
            INSTANCE.registerMessage(id++, WallUpdatePacket.class, WallPacket::encode, WallUpdatePacket::new, WallUpdatePacket::handle);
            INSTANCE.registerMessage(id++, WallGuiPacket.class, WallPacket::encode, WallGuiPacket::new, WallGuiPacket::handle);

            INSTANCE.registerMessage(id++, CapeSyncPacket.class, CapeSyncPacket::encode, CapeSyncPacket::new, CapeSyncPacket::handle);
            INSTANCE.registerMessage(id++, CapeSavePacket.class, CapeSavePacket::encode, CapeSavePacket::new, CapeSavePacket::handle);
        });
    }
}
