package net.shelmarow.combat_evolution.client.bossbar;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;

public class CEBossEvent extends ServerBossEvent {
    private Component ceDisplayType = Component.empty();
    private float stamina = 1F;

    public CEBossEvent(Component pName) {
        super(pName, BossBarColor.RED, BossBarOverlay.PROGRESS);
    }

    @Override
    public void removeAllPlayers(){
        super.removeAllPlayers();
        ClientBossData.removeBoss(getId());
    }

    public void setDisplayType(Component pType){
        ceDisplayType = pType;
        ClientBossData.partialUpdate(getId(),data -> data.displayType = ceDisplayType);
    }

    public void setStamina(float value) {
        if(stamina != value){
            this.stamina = value;
            ClientBossData.partialUpdate(getId(),data -> {
                data.staminaO = ClientBossData.getStaminaProgress(data);
                data.stamina = value;
                data.staminaSetTime = Util.getMillis();
            });
        }
    }

    public float getStamina() {
        return stamina;
    }
}
