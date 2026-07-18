package me.kall.narutotv.impl.world.network;

import me.kall.duplicationless.network.Networker;
import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.impl.world.network.packet.*;
import me.kall.narutotv.impl.world.network.packet.base.ScreenPacket;
import net.minecraftforge.network.simple.SimpleChannel;

public class NarutoPackets {
    public static final SimpleChannel INSTANCE = Networker.create(NarutoTV.MOD_ID, "1");
    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, ScreenCleanPacket.class, ScreenPacket::encode, ScreenCleanPacket::new, ScreenCleanPacket::handle);
        INSTANCE.registerMessage(id++, ScreenDeathPacket.class, ScreenPacket::encode, ScreenDeathPacket::new, ScreenDeathPacket::handle);
        INSTANCE.registerMessage(id++, ScreenLifePacket.class, ScreenPacket::encode, ScreenLifePacket::new, ScreenLifePacket::handle);
        INSTANCE.registerMessage(id++, ScreenSyncPacket.class, ScreenSyncPacket::encode, ScreenSyncPacket::new, ScreenSyncPacket::handle);
        INSTANCE.registerMessage(id++, ScreenUpdatePacket.class, ScreenUpdatePacket::encode, ScreenUpdatePacket::new, ScreenUpdatePacket::handle);
    }
}
