package net.shelmarow.combat_evolution.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.bgm.network.S2CRemoveMusicPacket;
import net.shelmarow.combat_evolution.bgm.network.S2CRequestMusicPacket;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;
import net.shelmarow.combat_evolution.network.server.*;

public class CENetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload... packet) {
        for (CustomPacketPayload o : packet) {
            PacketDistributor.sendToPlayer(player, o);
        }
    }

    public static void sendToServer(CustomPacketPayload... packet) {
        for (CustomPacketPayload o : packet) {
            PacketDistributor.sendToServer(o);
        }
    }

    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(S2CUpdateBossDataPacket.TYPE, S2CUpdateBossDataPacket.STREAM_CODEC, S2CUpdateBossDataPacket::handle);
        registrar.playToClient(S2CRemoveBossDataPacket.TYPE, S2CRemoveBossDataPacket.STREAM_CODEC, S2CRemoveBossDataPacket::handle);
        registrar.playToClient(S2CUpdateBossBarTexture.TYPE, S2CUpdateBossBarTexture.STREAM_CODEC, S2CUpdateBossBarTexture::handle);
        registrar.playToClient(S2CUpdateBossCustomDataPacket.TYPE, S2CUpdateBossCustomDataPacket.STREAM_CODEC, S2CUpdateBossCustomDataPacket::handle);
        registrar.playToClient(S2CUpdateStaminaDataPacket.TYPE, S2CUpdateStaminaDataPacket.STREAM_CODEC, S2CUpdateStaminaDataPacket::handle);
        registrar.playToClient(S2CRequestMusicPacket.TYPE, S2CRequestMusicPacket.STREAM_CODEC, S2CRequestMusicPacket::handle);
        registrar.playToClient(S2CRemoveMusicPacket.TYPE, S2CRemoveMusicPacket.STREAM_CODEC, S2CRemoveMusicPacket::handle);
        registrar.playToClient(SPCEDataPacket.TYPE, SPCEDataPacket.STREAM_CODEC, SPCEDataPacket::handle);

        registrar.playToServer(C2STryExecutionPacket.TYPE, C2STryExecutionPacket.STREAM_CODEC, C2STryExecutionPacket::handle);
    }

}
