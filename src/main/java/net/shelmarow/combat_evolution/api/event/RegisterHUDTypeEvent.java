package net.shelmarow.combat_evolution.api.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.shelmarow.combat_evolution.client.execution.HUDTypeManager;
import net.shelmarow.combat_evolution.client.execution.types.HUDType;

public class RegisterHUDTypeEvent extends Event implements IModBusEvent {

    public void registerHUDType(String modId, HUDType hudType) {
        HUDTypeManager.registerHUDType(modId, hudType);
    }
}
