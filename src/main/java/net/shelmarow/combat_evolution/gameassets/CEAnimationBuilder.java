package net.shelmarow.combat_evolution.gameassets;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.api.animation.AnimationManager;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = EventBusSubscriber.Bus.MOD)
public class CEAnimationBuilder {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(CombatEvolution.MOD_ID, CEAnimationBuilder::buildAll);
    }

    private static void buildAll(AnimationManager.AnimationBuilder builder) {
        ExecutionSkillAnimations.build(builder);
        ShieldCounterAnimations.build(builder);
    }

}
