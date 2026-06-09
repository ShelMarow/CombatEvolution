package net.shelmarow.combat_evolution.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.bgm.network.S2CRemoveMusicPacket;
import net.shelmarow.combat_evolution.bgm.network.S2CRequestMusicPacket;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;
import net.shelmarow.combat_evolution.network.server.*;

public class CENetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    public static void sendToPlayer(ServerPlayer player, Object... packet) {
        for (Object o : packet) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), o);
        }
    }

    public static void sendToServer(Object... packet) {
        for (Object o : packet) {
            CHANNEL.sendToServer(o);
        }
    }

    public static void registerPackets() {
        int packetId = 0;

        CHANNEL.registerMessage(packetId++, S2CUpdateBossDataPacket.class, S2CUpdateBossDataPacket::encode, S2CUpdateBossDataPacket::decode, S2CUpdateBossDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CRemoveBossDataPacket.class, S2CRemoveBossDataPacket::encode, S2CRemoveBossDataPacket::decode, S2CRemoveBossDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CUpdateBossBarTexture.class, S2CUpdateBossBarTexture::encode, S2CUpdateBossBarTexture::decode, S2CUpdateBossBarTexture::handle);
        CHANNEL.registerMessage(packetId++, S2CUpdateBossCustomDataPacket.class, S2CUpdateBossCustomDataPacket::encode, S2CUpdateBossCustomDataPacket::decode, S2CUpdateBossCustomDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CUpdateStaminaDataPacket.class, S2CUpdateStaminaDataPacket::encode, S2CUpdateStaminaDataPacket::decode, S2CUpdateStaminaDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CRequestMusicPacket.class, S2CRequestMusicPacket::encode, S2CRequestMusicPacket::decode, S2CRequestMusicPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CRemoveMusicPacket.class, S2CRemoveMusicPacket::encode, S2CRemoveMusicPacket::decode, S2CRemoveMusicPacket::handle);
        CHANNEL.registerMessage(packetId++, SPCEDataPacket.class, SPCEDataPacket::encode, SPCEDataPacket::decode, SPCEDataPacket::handle);

        CHANNEL.registerMessage(packetId++, C2STryExecutionPacket.class, C2STryExecutionPacket::encode,C2STryExecutionPacket::decode, C2STryExecutionPacket::handle);
    }

}
