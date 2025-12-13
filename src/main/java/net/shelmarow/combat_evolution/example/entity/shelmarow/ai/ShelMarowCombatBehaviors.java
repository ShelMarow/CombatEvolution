package net.shelmarow.combat_evolution.example.entity.shelmarow.ai;

import com.hm.efn.gameasset.animations.EFNShortSwordAnimations;
import net.shelmarow.combat_evolution.ai.AnimationParams;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.HitEvent;
import net.shelmarow.combat_evolution.ai.PhaseParams;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;

public class ShelMarowCombatBehaviors {

    public static final CECombatBehaviors.Builder<MobPatch<?>> COMMON;

    static{
        COMMON = CECombatBehaviors.builder()

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .maxCooldown(20)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,4)
                                .animationBehavior(Animations.BIPED_MOB_SWORD_DUAL1, new AnimationParams()
                                        .transitionTime(0.5F).playSpeed(0.25F)
                                        .addPhase(0, new PhaseParams().damageMultiplier(0.01F).stunType(StunType.LONG))
                                        .addPhase(1, new PhaseParams().damageMultiplier(0.01F).stunType(StunType.KNOCKDOWN))
                                )
                                .addHitEvent(
                                        new HitEvent(0, AttackResult.ResultType.SUCCESS,(mobPatch, entity) -> {
                                            System.out.println("第一段成功命中");
                                        }),
                                        new HitEvent(1, AttackResult.ResultType.BLOCKED,(mobPatch, entity) -> {
                                            System.out.println("第二段被防御");
                                        })
                                )
                        )
                )

        ;
    }
}
