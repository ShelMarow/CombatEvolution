package net.shelmarow.combat_evolution.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

public class RegisterCustomExecutionEvent extends Event implements IModBusEvent {

    public void RegisterExecutionByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        ExecutionTypeManager.registerByItem(resourceLocation, type);
    }

    public void RegisterExecutionByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        ExecutionTypeManager.registerByCategory(weaponCategory,type);
    }
}
