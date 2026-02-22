package net.shelmarow.combat_evolution.example.event;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.api.event.RegisterCustomExecutionEvent;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarow;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarowPatch;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.item.EpicFightItems;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvent {

    @SubscribeEvent
    public static void registerExecutionType(RegisterCustomExecutionEvent event) {
    }

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event){
        event.put(CEEntities.SHELMAROW.get(), ShelMarow.createAttributes().build());
    }

    @SubscribeEvent
    public static void setPatch(EntityPatchRegistryEvent event) {
        event.getTypeEntry().put(CEEntities.SHELMAROW.get(), (entity) -> ShelMarowPatch::new);
    }
}
