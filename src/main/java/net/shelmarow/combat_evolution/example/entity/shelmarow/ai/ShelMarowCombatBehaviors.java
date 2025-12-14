package net.shelmarow.combat_evolution.example.entity.shelmarow.ai;

import net.minecraft.sounds.SoundEvents;
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


                //攻击间隙的游荡和随机闪避
                .newGlobalBehavior(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1).maxCooldown(20)
                        .backAfterFinished(false).rootName("randomDodgeGround")

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("全局闪避1")
                                .phaseBetween(0,4)
                                .withinDistance(4,12)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_FORWARD,0F)
                                .addPhase(1)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("全局闪避2")
                                .phaseBetween(0,4)
                                .withinDistance(0,4)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_BACKWARD,0F)
                                .addPhase(1)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("全局闪避3")
                                .phaseBetween(0,4)
                                .withinDistance(0,8)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_LEFT,0F)
                                .addPhase(1)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("全局闪避4")
                                .phaseBetween(0,4)
                                .withinDistance(0,8)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_RIGHT,0F)
                                .addPhase(1)
                        )
                )

                //闪避
                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .rootName("普通闪避")
                        .priority(2).weight(1).maxCooldown(100)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("闪避1")
                                .withinDistance(3,12)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_FORWARD,0F)
                                .setPhase(0)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("闪避2")
                                .withinDistance(0,3)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_BACKWARD,0F)
                                .setPhase(0)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("闪避3")
                                .withinDistance(0,8)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_LEFT,0F)
                                .setPhase(0)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .name("闪避4")
                                .withinDistance(0,8)
                                .canInsertGlobalBehavior(true,"randomDodgeGround")
                                .animationBehavior(Animations.BIPED_STEP_RIGHT,0F)
                                .setPhase(0)
                        )
                )

        ;
    }
}
