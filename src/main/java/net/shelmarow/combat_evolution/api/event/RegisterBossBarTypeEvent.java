package net.shelmarow.combat_evolution.api.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.shelmarow.combat_evolution.bossbar.client.BossBarTypeManager;
import net.shelmarow.combat_evolution.bossbar.client.types.AbstractBossBarType;

public class RegisterBossBarTypeEvent extends Event implements IModBusEvent {

    public void registerBossBarType(AbstractBossBarType bossBarType){
        BossBarTypeManager.getInstance().register(bossBarType.getName(), bossBarType);
    }

}
