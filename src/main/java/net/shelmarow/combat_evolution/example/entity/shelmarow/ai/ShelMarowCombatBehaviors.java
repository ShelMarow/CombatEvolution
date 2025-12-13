package net.shelmarow.combat_evolution.example.entity.shelmarow.ai;

import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.HitEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class ShelMarowCombatBehaviors {

    public static final CECombatBehaviors.Builder<MobPatch<?>> COMMON;

    static{
        COMMON = CECombatBehaviors.builder()

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .maxCooldown(50)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,4)
                                .animationBehavior(Animations.BIPED_MOB_SWORD_DUAL1, 0F)
                                .addHitEvent(
                                        new HitEvent(0, AttackResult.ResultType.BLOCKED,(mobPatch, entity) -> {
                                            System.out.println("这是第一段，被格挡");
                                        }),
                                        new HitEvent(1, AttackResult.ResultType.MISSED,(mobPatch, entity) -> {
                                            System.out.println("这是第二段，未命中");
                                        })
                                )

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .animationBehavior(Animations.BIPED_MOB_SWORD_DUAL2, 0F)
                                        .addHitEvent(
                                                new HitEvent((mobPatch, entity) -> {
                                                    System.out.println("所有Phase都触发命中");
                                                })
                                        )

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .animationBehavior(Animations.BIPED_MOB_SWORD_DUAL3, 0F)
                                        )
                                )
                        )
                )

        ;
    }
}
