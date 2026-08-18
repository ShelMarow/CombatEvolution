package net.shelmarow.combat_evolution.example.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarowRenderer;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.registry.RegisterPatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = EventBusSubscriber.Bus.MOD,value = Dist.CLIENT)
public class ModEventClient {

    @SubscribeEvent
    public static void rendererRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CEEntities.SHELMAROW.get(), ShelMarowRenderer::new);
    }

    public static void onPatchedRenderer(RegisterPatchedRenderersEvent.AddEntity event){
        event.addPatchedEntityRenderer(CEEntities.SHELMAROW.get(),
                entityType -> new PHumanoidRenderer<>(Meshes.BIPED_OLD_TEX, event.getContext(), entityType)
                        .initLayerLast(event.getContext(), entityType));
    }

    public static void registerEpicFightEvents() {
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(ModEventClient::onPatchedRenderer, CombatEvolution.MOD_ID);
    }
}
