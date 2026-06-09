package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import net.shelmarow.combat_evolution.network.server.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class CEBossEvent extends ServerBossEvent {
    private final BossData bossData = new BossData();

    public CEBossEvent(Component pName) {
        super(pName, BossBarColor.RED, BossBarOverlay.PROGRESS);
        setDisplayType("[CE:DefaultType]");
    }

    public CEBossEvent(String displayType, Component pName) {
        super(pName, BossBarColor.RED, BossBarOverlay.PROGRESS);
        setDisplayType(displayType);
    }

    @Override
    public void addPlayer(@NotNull ServerPlayer pPlayer) {
        super.addPlayer(pPlayer);
        CENetworkHandler.sendToPlayer(pPlayer, new S2CUpdateBossDataPacket(getId(), bossData));
    }

    @Override
    public void removePlayer(@NotNull ServerPlayer pPlayer) {
        super.removePlayer(pPlayer);
        CENetworkHandler.sendToPlayer(pPlayer, new S2CRemoveBossDataPacket(getId()));
    }

    public void setDisplayType(String pType){
        bossData.displayType = pType;
    }

    public void setBossBarTexture(@NonNull ResourceLocation bossBarTexture) {
        bossData.bossBarTexture = bossBarTexture;
        for (ServerPlayer serverPlayer : getPlayers()){
            CENetworkHandler.sendToPlayer(serverPlayer, new S2CUpdateBossBarTexture(getId(), bossData.bossBarTexture.toString()));
        }
    }

    public void setStaminaStatus(@NonNull StaminaStatus staminaStatus) {
        bossData.staminaStatus = staminaStatus;
    }

    public void setStamina(float value) {
        if(bossData.stamina != value){
            bossData.stamina = value;
            for (ServerPlayer player : getPlayers()) {
                CENetworkHandler.sendToPlayer(player, new S2CUpdateStaminaDataPacket(getId(), bossData.stamina, bossData.staminaStatus));
            }
        }
    }

    public void updateCustomData(CompoundTag tag){
        bossData.customData = tag;
        for (ServerPlayer player : getPlayers()) {
            CENetworkHandler.sendToPlayer(player, new S2CUpdateBossCustomDataPacket(getId(), bossData.customData));
        }
    }

    public CompoundTag getCustomData(){
        return bossData.customData;
    }
}
