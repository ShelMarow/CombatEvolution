package net.shelmarow.combat_evolution.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.function.BiFunction;

public class RegisterCustomExecutionEvent extends Event implements IModBusEvent {

    public void RegisterExecutionByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, Style style, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, style, (item,entityPatch)->type);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, Style style, BiFunction<Item, LivingEntityPatch<?>, ExecutionTypeManager.Type> biFunction) {
        ExecutionTypeManager.registerByItem(resourceLocation, style, biFunction);
    }

    public void RegisterExecutionByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, Style style, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, style, (item,entityPatch) -> type);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, Style style, BiFunction<Item, LivingEntityPatch<?>, ExecutionTypeManager.Type> biFunction) {
        ExecutionTypeManager.registerByCategory(weaponCategory, style, biFunction);
    }
}
