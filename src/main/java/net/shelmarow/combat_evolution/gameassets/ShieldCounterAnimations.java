package net.shelmarow.combat_evolution.gameassets;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

public class ShieldCounterAnimations {

    public static AnimationManager.AnimationAccessor<ActionAnimation> SHIELD_COUNTER;
    public static AnimationManager.AnimationAccessor<LongHitAnimation> COUNTERED;

    public static void build(AnimationManager.AnimationBuilder builder) {
        SHIELD_COUNTER = builder.nextAccessor("biped/skill/shield_counter/shield_counter", accessor->
                new ActionAnimation(0.05F, accessor, Armatures.BIPED)
                        .newTimePair(0.15F,0.35F)
                        .addState(CEEntityState.COUNTER_SUSSED, true)

                        .addEvents(AnimationEvent.InTimeEvent.create(0.15F, (entityPatch, animation, params) -> {
                            entityPatch.playSound(EpicFightSounds.WHOOSH_BIG.get(), 0,0);
                        }, AnimationEvent.Side.CLIENT))
        );

        COUNTERED = builder.nextAccessor("biped/skill/shield_counter/countered", accessor->
                new LongHitAnimation(0.1F, accessor, Armatures.BIPED)
                        .newTimePair(0F,10F)
                        .addState(EntityState.ATTACK_RESULT, damageSource -> {
                            if(damageSource instanceof EpicFightDamageSource epicFightDamageSource){
                                epicFightDamageSource.setStunType(StunType.NONE);
                            }
                            return AttackResult.ResultType.SUCCESS;
                        })
        );
    }
}
