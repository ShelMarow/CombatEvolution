package net.shelmarow.combat_evolution.example.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarowRenderer;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD,value = Dist.CLIENT)
public class ModEventClient {

    @SubscribeEvent
    public static void rendererRegister(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CEEntities.SHELMAROW.get(), ShelMarowRenderer::new);
    }

    @SubscribeEvent
    public static void onPatchedRenderer(PatchedRenderersEvent.Add event){
        event.addPatchedEntityRenderer(CEEntities.SHELMAROW.get(),
                entityType -> new PHumanoidRenderer<>(Meshes.BIPED_OLD_TEX, event.getContext(), entityType)
                        .initLayerLast(event.getContext(), entityType));

    }
}
