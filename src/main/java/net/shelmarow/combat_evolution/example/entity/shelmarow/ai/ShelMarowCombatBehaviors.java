package net.shelmarow.combat_evolution.example.entity.shelmarow.ai;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.event.BlockedEvent;
import net.shelmarow.combat_evolution.ai.event.HitEvent;
import net.shelmarow.combat_evolution.ai.event.OnHurtEvent;
import net.shelmarow.combat_evolution.ai.event.TimeEvent;
import net.shelmarow.combat_evolution.ai.params.AnimationParams;
import net.shelmarow.combat_evolution.ai.util.CEParticleUtils;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.gameassets.ShieldCounterAnimations;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Set;

public class ShelMarowCombatBehaviors {

    public static final CECombatBehaviors.Builder<MobPatch<?>> COMMON;

    static{
        COMMON = CECombatBehaviors.builder()

                //Global Behavior
                .newGlobalBehavior(CECombatBehaviors.BehaviorRoot.builder()
                        .backAfterFinished(true).rootName("TachiSkill")

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .phaseContain(1)
                                .canInterruptParent(true)
                                .animationBehavior(Animations.RUSHING_TEMPO1, new AnimationParams()
                                        .transitionTime(0.15F).playSpeed(0.75F)
                                        .stunType(StunType.LONG)
                                )
                                .setPhase(0)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .phaseContain(2)
                                .canInterruptParent(true)
                                .animationBehavior(Animations.RUSHING_TEMPO2, new AnimationParams()
                                        .transitionTime(0.15F).playSpeed(0.75F)
                                        .stunType(StunType.LONG)
                                )
                                .setPhase(0)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .phaseContain(3)
                                .canInterruptParent(true)
                                .animationBehavior(Animations.RUSHING_TEMPO3, new AnimationParams()
                                        .transitionTime(0.15F).playSpeed(0.75F)
                                        .stunType(StunType.LONG)
                                )
                                .setPhase(0)
                                .addBlockedEvent(
                                        new BlockedEvent(false, (mobPatch, entityPatch) -> {
                                            mobPatch.playAnimationSynchronized(ShieldCounterAnimations.COUNTERED, 0F);
                                        })
                                )
                        )
                )

                //Common Behavior
                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .maxCooldown(20)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 4)
                                .animationBehavior(Animations.LONGSWORD_AUTO1, 0.25F)
                                .addExBehavior(mobPatch -> {
                                    mobPatch.playSound(SoundEvents.GUARDIAN_AMBIENT, 0, 0);
                                })

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .withinDistance(0, 4)
                                        .animationBehavior(Animations.LONGSWORD_AUTO2, new AnimationParams()
                                                .playSpeed(1.25F).damageMultiplier(1.15F)
                                        )

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .withinDistance(0, 4)
                                                .animationBehavior(Animations.LONGSWORD_AUTO3, new AnimationParams()
                                                        .playSpeed(1.25F).damageMultiplier(1.3F)
                                                )
                                                .addTimeEvent(
                                                        new TimeEvent(0.15F, mobPatch -> {
                                                            ServerLevel level = (ServerLevel) mobPatch.getOriginal().level();
                                                            Vec3 position = mobPatch.getOriginal().position();
                                                            level.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z, 3, 0, 1.25, 0 ,1);
                                                        })
                                                )
                                        )
                                )

                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 4)
                                .animationBehavior(Animations.LONGSWORD_LIECHTENAUER_AUTO1, 0.25F)
                                .addExBehavior(mobPatch -> {
                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 30));
                                })
                                .setOnHurtEvent(new OnHurtEvent(onLongswordSkillBlocked()))

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .withinDistance(0, 4)
                                        .animationBehavior(Animations.LONGSWORD_LIECHTENAUER_AUTO2, 0.25F)
                                        .addExBehavior(mobPatch -> {
                                            mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 30));
                                        })
                                        .setOnHurtEvent(new OnHurtEvent(onLongswordSkillBlocked()))

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .withinDistance(0, 4)
                                                .animationBehavior(Animations.LONGSWORD_LIECHTENAUER_AUTO3, 0.25F)
                                                .addExBehavior(mobPatch -> {
                                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 30));
                                                })
                                                .setOnHurtEvent(new OnHurtEvent(onLongswordSkillBlocked()))
                                        )
                                )

                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(2).weight(1)
                        .maxCooldown(160)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 4)
                                .interruptedByLevel(3)
                                .canInsertGlobalBehavior(true, "TachiSkill")
                                .animationBehavior(Animations.TACHI_AUTO1, 0.25F)
                                .addHitEvent(new HitEvent(AttackResult.ResultType.SUCCESS, (mobPatch, entity) -> {
                                    CEPatchUtils.setPhase(mobPatch, 1);
                                }))

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .withinDistance(0, 4)
                                        .interruptedByLevel(3)
                                        .canInsertGlobalBehavior(true, "TachiSkill")
                                        .animationBehavior(Animations.TACHI_AUTO2, 0.25F)
                                        .addHitEvent(new HitEvent(AttackResult.ResultType.SUCCESS, (mobPatch, entity) -> {
                                            CEPatchUtils.setPhase(mobPatch, 2);
                                        }))

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .withinDistance(0, 4)
                                                .interruptedByLevel(3)
                                                .canInsertGlobalBehavior(true, "TachiSkill")
                                                .animationBehavior(Animations.TACHI_AUTO3, 0.25F)
                                                .setPhase(3)
                                        )
                                )
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .maxCooldown(200).cooldown(100)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 10)
                                .interruptedByTime(40 * 0.05F, 80 * 0.05F)
                                .wander(80, 0, 1)

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .canInterruptParent(true)
                                        .attackLevel(1, 2)
                                        .animationBehavior(Animations.BIPED_ROLL_BACKWARD, 0F)
                                )
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .stopByStun(4)
                                .withinDistance(0, 10)
                                .interruptedByTime(40 * 0.05F, 80 * 0.05F)
                                .wander(80, 0, -1)

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .canInterruptParent(true)
                                        .attackLevel(1, 2)
                                        .animationBehavior(Animations.BIPED_ROLL_BACKWARD, 0F)
                                )
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1)
                        .maxCooldown(100)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 4)
                                .guard(60)
                                .counterAnimation(Animations.THE_GUILLOTINE, 0.15F)
                                .counterType(CECombatBehaviors.CounterType.END)
                                .maxGuardHit(3)
                                .resetGuardTime(true)
                                .onCounterStart(mobPatch -> {
                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 30));
                                })
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0, 4)
                                .guard(60)
                                .counterAnimation(Animations.SWEEPING_EDGE, new AnimationParams()
                                        .transitionTime(0.25F).playSpeed(0.75F)
                                        .damageMultiplier(1.5F).impactMultiplier(1.5F).armorNegationMultiplier(2F)
                                        .damageSource(Set.of(EpicFightDamageTypeTags.BYPASS_DODGE))
                                )
                                .counterType(CECombatBehaviors.CounterType.RANDOM)
                                .counterChance(0.35F)
                                .onCounterStart(mobPatch -> {
                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 30));
                                    if(mobPatch.getTarget() != null){
                                        CEParticleUtils.spawnWarningParticle(CEParticles.BYPASS_DODGE_WARNING.get(), mobPatch.getTarget(), new Vec3(0,1.25,0));
                                    }
                                })
                        )
                )

        ;
    }

    private static @NotNull TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> onLongswordSkillBlocked() {
        return (mobPatch, damageSource, attackResult) -> {
            if (!damageSource.is(EpicFightDamageTypeTags.GUARD_PUNCTURE) && !damageSource.is(EpicFightDamageTypeTags.UNBLOCKALBE) &&
                    mobPatch.getEntityState().getLevel() >= 1 && mobPatch.getEntityState().getLevel() <= 2) {
                CEHumanoidPatch ceHumanoidPatch = (CEHumanoidPatch) mobPatch;
                ceHumanoidPatch.playGuardHitSound();
                EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument(
                        (ServerLevel) ceHumanoidPatch.getOriginal().level(), HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO, ceHumanoidPatch.getOriginal(),
                        damageSource.getDirectEntity() == null ? damageSource.getEntity() : damageSource.getDirectEntity()
                );
                return new AttackResult(AttackResult.ResultType.BLOCKED, 0F);
            }
            return attackResult;
        };
    }
}
