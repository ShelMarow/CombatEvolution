package net.shelmarow.combat_evolution.gameassets;


import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.damage_source.CEDamageTypeTags;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.collider.MultiCollider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.world.damagesource.ExtraDamageInstance;

import java.util.Set;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExecutionSkillAnimations {
    public static AnimationManager.AnimationAccessor<AttackAnimation> EXECUTION;
    public static AnimationManager.AnimationAccessor<LongHitAnimation> EXECUTED_FULL;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(CombatEvolution.MOD_ID, ExecutionSkillAnimations::build);
    }

    private static final ExtraDamageInstance.ExtraDamage TARGET_MAX_HEALTH = new ExtraDamageInstance.ExtraDamage(
            (attacker, itemstack, target, baseDamage, params) -> target.getMaxHealth() * params[0],
            (itemstack, tooltips, baseDamage, params) -> {});

    public static void build(AnimationManager.AnimationBuilder builder) {

        MultiCollider<OBBCollider> executionCollider = new MultiOBBCollider(3, 1.25F, 1.5F, 1.5F, 0.0F, 1.5F, -1.5F);

        //处决
        AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTION =
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> 1.0F;
        AnimationProperty.PlaybackSpeedModifier CONSTANT_EXECUTED =
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> 1.0F/1.2F;

        EXECUTION = builder.nextAccessor("biped/skill/execution/execution_sword", accessor ->
                new AttackAnimation(0.01F, accessor, Armatures.BIPED,
                        new AttackAnimation.Phase(0.0F, 0.0F, 0.76F, 0.93F, 0.93F, 0.93F, Armatures.BIPED.get().rootJoint,
                                executionCollider)
                                .addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100F))
                                .addProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0))
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(0.001F))
                                .addProperty(AnimationProperty.AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLADE_RUSH_FINISHER.get())
                                .addProperty(AnimationProperty.AttackPhaseProperty.SOURCE_TAG,
                                        Set.of(
                                                CEDamageTypeTags.EXECUTION,DamageTypeTags.BYPASSES_ARMOR,
                                                DamageTypeTags.BYPASSES_ENCHANTMENTS,DamageTypeTags.BYPASSES_EFFECTS
                                        )
                                ),
                        new AttackAnimation.Phase(0.93F, 0.0F, 3.16F, 3.36F, 5.0F, 5.0F, Armatures.BIPED.get().rootJoint,
                                executionCollider)
                                .addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100F))
                                .addProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0))
                                .addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.5F))
                                .addProperty(AnimationProperty.AttackPhaseProperty.EXTRA_DAMAGE,Set.of(TARGET_MAX_HEALTH.create(0.08F)))
                                .addProperty(AnimationProperty.AttackPhaseProperty.HIT_SOUND, EpicFightSounds.EVISCERATE.get())
                                .addProperty(AnimationProperty.AttackPhaseProperty.SOURCE_TAG,
                                        Set.of(
                                                CEDamageTypeTags.EXECUTION,DamageTypeTags.BYPASSES_ARMOR,
                                                DamageTypeTags.BYPASSES_ENCHANTMENTS,DamageTypeTags.BYPASSES_EFFECTS
                                        )
                                ))
                        .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTION)
                        .newTimePair(0.0F,5F).addStateRemoveOld(EntityState.CAN_BASIC_ATTACK,false)
                        .newTimePair(0.0F,5F).addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION,false)
                        .addEvents(AnimationEvent.InTimeEvent.create(0.6F,(livingEntityPatch, assetAccessor, animationParameters) -> {
                            livingEntityPatch.getOriginal().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,50,9,false,false));
                        }, AnimationEvent.Side.BOTH))
        );


        EXECUTED_FULL = builder.nextAccessor("biped/skill/execution/executed_full", accessor ->
                new LongHitAnimation(0.01F, accessor, Armatures.BIPED)
                        .addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, CONSTANT_EXECUTED)
                        .addState(EntityState.ATTACK_RESULT,damageSource -> {
                            if(damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damageSource.is(CEDamageTypeTags.EXECUTION)) {
                                return AttackResult.ResultType.SUCCESS;
                            }
                            return AttackResult.ResultType.MISSED;
                        })
        );
    }
}
