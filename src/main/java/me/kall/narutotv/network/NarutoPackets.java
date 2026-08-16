package me.kall.narutotv.network;

import me.kall.narutotv.NarutoTV;
import me.kall.narutotv.network.packet.cape.CapeSyncPacket;
import me.kall.narutotv.network.packet.cape.CapeUpdatePacket;
import me.kall.narutotv.network.packet.wall.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = NarutoTV.MOD_ID)
public class NarutoPackets {
    public static final Logger LOGGER = LogManager.getLogger(NarutoPackets.class);

    public static final Type<WallLifePacket> WALL_LIFE_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_life"));
    public static final Type<WallCleanPacket> WALL_CLEAN_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_clean"));
    public static final Type<WallDeathPacket> WALL_DEATH_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_death"));
    public static final Type<WallSyncPacket> WALL_SYNC_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_sync"));
    public static final Type<WallUpdatePacket> WALL_UPDATE_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_update"));
    public static final Type<WallConfigPacket> WALL_CONFIG_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "wall_config"));

    public static final Type<CapeSyncPacket> CAPE_SYNC_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "cape_sync"));
    public static final Type<CapeUpdatePacket> CAPE_UPDATE_PACKET_TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoTV.MOD_ID, "cape_update"));

    @SubscribeEvent
    public static void setup(@NotNull RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(WALL_LIFE_PACKET_TYPE, CustomPacketPayload.codec(WallLifePacket::encode, WallLifePacket::new), WallLifePacket::handle);
        registrar.playToServer(WALL_CLEAN_PACKET_TYPE, CustomPacketPayload.codec(WallCleanPacket::encode, WallCleanPacket::new), WallCleanPacket::handle);
        registrar.playToClient(WALL_DEATH_PACKET_TYPE, CustomPacketPayload.codec(WallDeathPacket::encode, WallDeathPacket::new), WallDeathPacket::handle);
        registrar.playToClient(WALL_SYNC_PACKET_TYPE, CustomPacketPayload.codec(WallSyncPacket::encode, WallSyncPacket::new), WallSyncPacket::handle);
        registrar.playBidirectional(WALL_UPDATE_PACKET_TYPE, CustomPacketPayload.codec(WallUpdatePacket::encode, WallUpdatePacket::new), WallUpdatePacket::handle);
        registrar.playToClient(WALL_CONFIG_PACKET_TYPE, CustomPacketPayload.codec(WallConfigPacket::encode, WallConfigPacket::new), WallConfigPacket::handle);

        registrar.playToClient(CAPE_SYNC_PACKET_TYPE, CustomPacketPayload.codec(CapeSyncPacket::encode, CapeSyncPacket::new), CapeSyncPacket::handle);
        registrar.playBidirectional(CAPE_UPDATE_PACKET_TYPE, CustomPacketPayload.codec(CapeUpdatePacket::encode, CapeUpdatePacket::new), CapeUpdatePacket::handle);
    }
}
