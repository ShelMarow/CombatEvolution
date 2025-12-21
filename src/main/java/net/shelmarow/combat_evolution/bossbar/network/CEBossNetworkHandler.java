package net.shelmarow.combat_evolution.bossbar.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.BossData;
import net.shelmarow.combat_evolution.bossbar.network.packet.S2CRemoveBossDataPacket;
import net.shelmarow.combat_evolution.bossbar.network.packet.S2CUpdateBossDataPacket;
import net.shelmarow.combat_evolution.bossbar.network.packet.S2CUpdateStaminaDataPacket;

import java.util.UUID;

public class CEBossNetworkHandler {

    public static void updateBossData(ServerPlayer serverPlayer, UUID uuid, BossData data) {
        CombatEvolution.CHANNEL.send(PacketDistributor.PLAYER.with(()->serverPlayer), new S2CUpdateBossDataPacket(uuid, data));
    }

    public static void updateStaminaData(ServerPlayer serverPlayer, UUID uuid, BossData data) {
        CombatEvolution.CHANNEL.send(PacketDistributor.PLAYER.with(()->serverPlayer), new S2CUpdateStaminaDataPacket(uuid, data.stamina, data.staminaStatus));
    }

    public static void removeBossData(ServerPlayer serverPlayer, UUID uuid) {
        CombatEvolution.CHANNEL.send(PacketDistributor.PLAYER.with(()->serverPlayer), new S2CRemoveBossDataPacket(uuid));
    }

}
