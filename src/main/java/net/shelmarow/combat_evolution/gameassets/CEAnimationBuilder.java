package net.shelmarow.combat_evolution.gameassets;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.api.animation.AnimationManager;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
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
