package net.shelmarow.combat_evolution.gameassets;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionAttackAnimation;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionHitAnimation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.collider.MultiCollider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.world.damagesource.ExtraDamageInstance;

import java.util.Set;

public class ExecutionSkillAnimations {
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_SWORD;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_DAGGER;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_TACHI;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_GREATSWORD;
    public static AnimationManager.AnimationAccessor<ExecutionHitAnimation> EXECUTED_FULL;


    private static final ExtraDamageInstance.ExtraDamage TARGET_MAX_HEALTH = new ExtraDamageInstance.ExtraDamage(
            (attacker, itemstack, target, baseDamage, params) -> {
                //参数说明
                //1.基础固定伤害
                //2.最大生命值百分比
                return params[0] + target.getMaxHealth() * params[1];
            },(itemstack, tooltips, baseDamage, params) -> {});


    public static void build(AnimationManager.AnimationBuilder builder) {

        MultiCollider<OBBCollider> executionCollider = new MultiOBBCollider(3, 1.25F, 1.5F, 1.5F, 0.0F, 1.5F, -1.5F);

        //处决
        AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTION =
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> 1.0F;
        AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTED =
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> 1.0F/1.2F;

        EXECUTION_SWORD = builder.nextAccessor("biped/skill/execution/execution_sword", accessor ->
                getExecutionAttackAnimation(accessor, executionCollider, CONSTANT_EXECUTION)
        );

        EXECUTION_DAGGER = builder.nextAccessor("biped/skill/execution/execution_dagger", accessor ->
                getExecutionAttackAnimation(accessor, executionCollider, CONSTANT_EXECUTION)
        );

        EXECUTION_TACHI = builder.nextAccessor("biped/skill/execution/execution_tachi", accessor ->
                getExecutionAttackAnimation(accessor, executionCollider, CONSTANT_EXECUTION)
        );

        EXECUTION_GREATSWORD = builder.nextAccessor("biped/skill/execution/execution_greatsword", accessor ->
                getExecutionAttackAnimation(accessor, executionCollider, CONSTANT_EXECUTION)
        );


        EXECUTED_FULL = builder.nextAccessor("biped/skill/execution/executed_full", accessor ->
                new ExecutionHitAnimation(0.01F, accessor, Armatures.BIPED)
                        .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTED)
        );
    }

    private static ExecutionAttackAnimation getExecutionAttackAnimation(AnimationManager.AnimationAccessor<ExecutionAttackAnimation> accessor, MultiCollider<OBBCollider> executionCollider, AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTION) {
        return new ExecutionAttackAnimation(0.01F, accessor, Armatures.BIPED,

                new ExecutionAttackAnimation.ExecutionPhase(false,0.0F, 0.0F, 0.76F, 0.93F, 0.93F, 0.93F, Armatures.BIPED.get().rootJoint, executionCollider)
                        .addProperty(AnimationProperty.AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLADE_RUSH_FINISHER.get()),

                new ExecutionAttackAnimation.ExecutionPhase(true,0.93F, 0.0F, 3.16F, 3.36F, 5.0F, 5.0F, Armatures.BIPED.get().rootJoint, executionCollider)
                        .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.5F))
                        .addProperty(AnimationProperty.AttackPhaseProperty.EXTRA_DAMAGE, Set.of(TARGET_MAX_HEALTH.create(15, 0.08F)))
                        .addProperty(AnimationProperty.AttackPhaseProperty.HIT_SOUND, EpicFightSounds.EVISCERATE.get()))

                .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTION)
                .addEvents(AnimationEvent.InTimeEvent.create(0.6F, (livingEntityPatch, assetAccessor, animationParameters) -> {
                    livingEntityPatch.getOriginal().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 9, false, false));
                }, AnimationEvent.Side.BOTH));
    }
}
