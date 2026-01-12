package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.bossbar.network.CEBossNetworkHandler;
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
        CEBossNetworkHandler.updateBossData(pPlayer,getId(),bossData);
    }

    @Override
    public void removePlayer(@NotNull ServerPlayer pPlayer) {
        super.removePlayer(pPlayer);
        CEBossNetworkHandler.removeBossData(pPlayer,getId());
    }

    public void setDisplayType(String pType){
        bossData.displayType = pType;
    }

    public void setStaminaStatus(StaminaStatus staminaStatus) {
        bossData.staminaStatus = staminaStatus;
    }

    public void setStamina(float value) {
        if(bossData.stamina != value){
            bossData.stamina = value;
            for (ServerPlayer player : getPlayers()) {
                CEBossNetworkHandler.updateStaminaData(player, getId(), bossData);
            }
        }
    }
}
