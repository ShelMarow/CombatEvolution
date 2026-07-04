package net.shelmarow.combat_evolution.gameassets;


import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.shelmarow.combat_evolution.client.fov.CEFovManager;
import net.shelmarow.combat_evolution.client.shader.ExecutionShaderManager;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionAttackAnimation;
import net.shelmarow.combat_evolution.gameassets.animation.ExecutionHitAnimation;
import net.shelmarow.combat_evolution.sounds.CESounds;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.collider.MultiCollider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.world.damagesource.ExtraDamageInstance;

import java.util.Set;

public class ExecutionSkillAnimations {
    public static AnimationManager.AnimationAccessor<ExecutionHitAnimation> EXECUTED_FULL;
    public static AnimationManager.AnimationAccessor<ExecutionHitAnimation> EXECUTED_FULL2;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_SWORD;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_DAGGER;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_TACHI;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_GREATSWORD;
    public static AnimationManager.AnimationAccessor<ExecutionAttackAnimation> EXECUTION_COLOSSALSWORD;


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


        EXECUTED_FULL = builder.nextAccessor("biped/skill/execution/executed_full", accessor ->
                new ExecutionHitAnimation(0.01F, accessor, Armatures.BIPED)
                        .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTED)
        );

        EXECUTED_FULL2 = builder.nextAccessor("biped/skill/execution/executed_full2", accessor ->
                new ExecutionHitAnimation(0.01F, accessor, Armatures.BIPED)
                        .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTION)
        );

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

        EXECUTION_COLOSSALSWORD = builder.nextAccessor("biped/skill/execution/execution_colossalsword", accessor ->
                getExecutionAttackAnimation(accessor, executionCollider, CONSTANT_EXECUTION)
        );
    }

    private static ExecutionAttackAnimation getExecutionAttackAnimation(AnimationManager.AnimationAccessor<ExecutionAttackAnimation> accessor, MultiCollider<OBBCollider> executionCollider, AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTION) {

        return new ExecutionAttackAnimation(0.01F, accessor, Armatures.BIPED,
                new ExecutionAttackAnimation.ExecutionPhase(false,0.0F, 0.0F, 0.76F, 0.93F, 0.93F, 0.93F,
                        InteractionHand.MAIN_HAND, Armatures.BIPED.get().rootJoint, executionCollider)
                        .addProperty(AnimationProperty.AttackPhaseProperty.PARTICLE, EpicFightParticles.EVISCERATE),
                new ExecutionAttackAnimation.ExecutionPhase(true,0.93F, 0.0F, 3.16F, 3.36F, 5.0F, 5.0F,
                        InteractionHand.MAIN_HAND, Armatures.BIPED.get().rootJoint, executionCollider)
                        .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.5F))
                        .addProperty(AnimationProperty.AttackPhaseProperty.EXTRA_DAMAGE, Set.of(TARGET_MAX_HEALTH.create(15, 0.08F)))
                        .addProperty(AnimationProperty.AttackPhaseProperty.PARTICLE, EpicFightParticles.BLADE_RUSH_SKILL))
                .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTION)
                .addEvents(
                        AnimationEvent.InTimeEvent.create(0.76F, (entitypatch, animation, params) -> {
                            entitypatch.playSound(CESounds.EXECUTION_1.get(), 0.65F,0,0);
                        }, AnimationEvent.Side.SERVER),
                        AnimationEvent.InTimeEvent.create(3.16F, (entitypatch, animation, params) -> {
                            entitypatch.playSound(CESounds.EXECUTION_2.get(), 0.65F,0,0);
                        }, AnimationEvent.Side.SERVER)
                )
                .addEvents(AnimationEvent.InTimeEvent.create(0.6F, (entitypatch, assetAccessor, animationParameters) -> {
                    if(entitypatch.getOriginal() instanceof LocalPlayer player) {
                        CEFovManager.setFovModifierTask(player,-0.55F, 60);
                    }}, AnimationEvent.Side.CLIENT))
                .addEvents(AnimationEvent.InTimeEvent.create(0.86F, (entitypatch, assetAccessor, animationParameters) -> {
                    if(entitypatch.getOriginal() instanceof LocalPlayer) {
                        ExecutionShaderManager.trigger(4, 10);
                    }}, AnimationEvent.Side.CLIENT))
                .addEvents(AnimationEvent.InTimeEvent.create(3.16F, (entitypatch, assetAccessor, animationParameters) -> {
                    if(entitypatch.getOriginal() instanceof LocalPlayer player) {
                        CEFovManager.resetFovModifier(player);
                        ExecutionShaderManager.trigger(10, 10);
                    }}, AnimationEvent.Side.CLIENT));
    }
}
