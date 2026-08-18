package net.shelmarow.combat_evolution.example.event;

import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.api.event.RegisterCustomExecutionEvent;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarow;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarowPatch;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.EntityPatchRegistryEvent;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class ModEvent {

    @SubscribeEvent
    public static void registerExecutionType(RegisterCustomExecutionEvent event) {

    }

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event){
        event.put(CEEntities.SHELMAROW.get(), ShelMarow.createAttributes().build());
    }

    public static void setPatch(EntityPatchRegistryEvent event) {
        event.registerEntityPatch(CEEntities.SHELMAROW.get(), ShelMarowPatch::new);
    }

    public static void registerEpicFightEvents() {
        EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(ModEvent::setPatch, CombatEvolution.MOD_ID);
    }
}
