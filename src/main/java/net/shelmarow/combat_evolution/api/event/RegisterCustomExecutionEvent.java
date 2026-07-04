package net.shelmarow.combat_evolution.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import org.apache.commons.lang3.function.TriFunction;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.function.BiFunction;

public class RegisterCustomExecutionEvent extends Event implements IModBusEvent {

    @Deprecated(forRemoval = true)
    public void RegisterExecutionByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, CapabilityItem.Styles.COMMON, type);
    }

    @Deprecated(forRemoval = true)
    public void RegisterExecutionByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, CapabilityItem.Styles.COMMON, type);
    }


    public void registerExecutionByEntity(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        registerExecutionByEntity(resourceLocation, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByEntity(ResourceLocation resourceLocation, Style style, ExecutionTypeManager.Type type) {
        registerExecutionByEntity(resourceLocation, style, (item,entityPatch)-> type);
    }

    public void registerExecutionByEntity(ResourceLocation resourceLocation, Style style, BiFunction<Item, LivingEntityPatch<?>, ExecutionTypeManager.Type> biFunction){
        registerExecutionByEntity(resourceLocation, style, (i, e, t)-> biFunction.apply(i ,e));
    }

    public void registerExecutionByEntity(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, ExecutionTypeManager.Type> function){
        ExecutionTypeManager.registerByEntity(resourceLocation, style, function);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, Style style, ExecutionTypeManager.Type type) {
        registerExecutionByItem(resourceLocation, style, (item,entityPatch)->type);
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, Style style, BiFunction<Item, LivingEntityPatch<?>, ExecutionTypeManager.Type> biFunction) {
        registerExecutionByItem(resourceLocation, style, (i, e, t)-> biFunction.apply(i ,e));
    }

    public void registerExecutionByItem(ResourceLocation resourceLocation, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, ExecutionTypeManager.Type> function) {
        ExecutionTypeManager.registerByItem(resourceLocation, style, function);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, CapabilityItem.Styles.COMMON, type);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, Style style, ExecutionTypeManager.Type type) {
        registerExecutionByCategory(weaponCategory, style, (item,entityPatch) -> type);
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, Style style, BiFunction<Item, LivingEntityPatch<?>, ExecutionTypeManager.Type> biFunction) {
        registerExecutionByCategory(weaponCategory, style, (i, e, t)-> biFunction.apply(i ,e));
    }

    public void registerExecutionByCategory(WeaponCategory weaponCategory, Style style, TriFunction<Item, LivingEntityPatch<?>, LivingEntityPatch<?>, ExecutionTypeManager.Type> function) {
        ExecutionTypeManager.registerByCategory(weaponCategory, style, function);
    }
}
