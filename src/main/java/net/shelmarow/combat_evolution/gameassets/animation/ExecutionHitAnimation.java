package net.shelmarow.combat_evolution.gameassets.animation;

import net.minecraft.tags.DamageTypeTags;
import net.shelmarow.combat_evolution.damage_source.CEDamageTypeTags;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

public class ExecutionHitAnimation extends ActionAnimation {

    public ExecutionHitAnimation(float transitionTime, AnimationManager.AnimationAccessor<? extends ExecutionHitAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, accessor, armature);

        this.addProperty(AnimationProperty.ActionAnimationProperty.STOP_MOVEMENT, true);
        this.addProperty(AnimationProperty.ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true);
        this.addProperty(AnimationProperty.StaticAnimationProperty.FIXED_HEAD_ROTATION, true);

        this.stateSpectrumBlueprint.clear()
                .newTimePair(0.0F, Float.MAX_VALUE)
                .addState(EntityState.TURNING_LOCKED, true)
                .addState(EntityState.MOVEMENT_LOCKED, true)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.CAN_BASIC_ATTACK, false)
                .addState(EntityState.CAN_SKILL_EXECUTION, false)
                .addState(EntityState.INACTION, true)
                .addState(EntityState.HURT_LEVEL,3)
                .addState(EntityState.ATTACK_RESULT, damageSource ->{
                    if(damageSource instanceof EpicFightDamageSource epicFightDamageSource){
                        epicFightDamageSource.setStunType(StunType.NONE);
                        epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.NO_STUN);
                    }
                    return damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damageSource.is(CEDamageTypeTags.EXECUTION) ?
                            AttackResult.ResultType.SUCCESS : AttackResult.ResultType.MISSED;
                });
    }

    public ExecutionHitAnimation(float transitionTime, String path, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, Float.MAX_VALUE, path, armature);

        this.addProperty(AnimationProperty.ActionAnimationProperty.STOP_MOVEMENT, true);
        this.addProperty(AnimationProperty.ActionAnimationProperty.REMOVE_DELTA_MOVEMENT, true);
        this.addProperty(AnimationProperty.StaticAnimationProperty.FIXED_HEAD_ROTATION, true);

        this.stateSpectrumBlueprint.clear()
                .newTimePair(0.0F, Float.MAX_VALUE)
                .addState(EntityState.TURNING_LOCKED, true)
                .addState(EntityState.MOVEMENT_LOCKED, true)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.CAN_BASIC_ATTACK, false)
                .addState(EntityState.CAN_SKILL_EXECUTION, false)
                .addState(EntityState.INACTION, true)
                .addState(EntityState.HURT_LEVEL,3)
                .addState(EntityState.ATTACK_RESULT, damageSource ->
                        damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damageSource.is(CEDamageTypeTags.EXECUTION) ?
                                AttackResult.ResultType.SUCCESS : AttackResult.ResultType.MISSED);
    }
}
