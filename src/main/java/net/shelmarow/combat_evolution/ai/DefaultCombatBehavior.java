package net.shelmarow.combat_evolution.ai;

import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class DefaultCombatBehavior {
    public static final CECombatBehaviors.Builder<MobPatch<?>> FIST;

    static {
        //默认拳头
        FIST = CECombatBehaviors.builder()
                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .priority(1).weight(1)
                                .withinEyeHeight().withinDistance(0,3)
                                .animationBehavior(Animations.ZOMBIE_ATTACK1,0)
                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .priority(1).weight(1)
                                        .withinEyeHeight().withinDistance(0,3)
                                        .animationBehavior(Animations.ZOMBIE_ATTACK2,0)
                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .priority(1).weight(1)
                                                .withinEyeHeight().withinDistance(0,3)
                                                .animationBehavior(Animations.ZOMBIE_ATTACK3,0)))));
    }
}
