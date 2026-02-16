package net.shelmarow.combat_evolution.example.entity.shelmarow.ai;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.event.HitEvent;
import net.shelmarow.combat_evolution.ai.event.TimeEvent;
import net.shelmarow.combat_evolution.ai.params.AnimationParams;
import net.shelmarow.combat_evolution.ai.params.PhaseParams;
import net.shelmarow.combat_evolution.ai.util.CEParticleUtils;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Set;

public class ShelMarowCombatBehaviors {

    public static final CECombatBehaviors.Builder<MobPatch<?>> COMMON;

    static{
        COMMON = CECombatBehaviors.builder()

                .newGlobalBehavior(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1).maxCooldown(100)
                        .backAfterFinished(false).rootName("global_not_back")

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,6)
                                .animationBehavior(Animations.LONGSWORD_DASH,0)
                        )
                )

                .newGlobalBehavior(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1).maxCooldown(100)
                        .backAfterFinished(true).rootName("global_back")

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,6)
                                .animationBehavior(Animations.SWEEPING_EDGE,0)
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(4).maxCooldown(20)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .canInsertGlobalBehavior(true)
                                .withinDistance(0,6)
                                .animationBehavior(Animations.LONGSWORD_AUTO1,0F)

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .canInsertGlobalBehavior(true,"global_back")
                                        .withinDistance(0,6)
                                        .animationBehavior(Animations.LONGSWORD_AUTO2,0F)

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .withinDistance(0,6)
                                                .waitTime(100)
                                                .animationBehavior(Animations.LONGSWORD_AUTO3,0F)
                                                .addTimeEvent(
                                                        new TimeEvent(0.25F, mobPatch -> {
                                                            mobPatch.playSound(SoundEvents.VILLAGER_AMBIENT,0,0);
                                                        })
                                                )

                                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                        .attackLevel(1,2)
                                                        .withinDistance(0,6)
                                                        .animationBehavior(Animations.BIPED_STEP_BACKWARD,0F)
                                                        .addCooldown(100)
                                                        .setPhase(1)
                                                )
                                        )
                                )
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(2).weight(1).maxCooldown(0)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .phaseContain(1)
                                .animationBehavior(Animations.BATTOJUTSU,0F)
                                .addTimeEvent(
                                        new TimeEvent(mobPatch -> {
                                            if(mobPatch.getTarget() != null){
                                                mobPatch.getOriginal().lookAt(EntityAnchorArgument.Anchor.FEET,mobPatch.getTarget().position());
                                            }
                                        })
                                )
                                .setPhase(0)
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(4).maxCooldown(20)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,6)
                                .animationBehavior(Animations.TACHI_AUTO1,0F)
                                .addExBehavior(mobPatch -> {
                                    mobPatch.playSound(SoundEvents.VILLAGER_NO,0,0);
                                })

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .withinDistance(0,6)
                                        .animationBehavior(Animations.TACHI_AUTO2,0F)
                                        .addHitEvent(
                                                new HitEvent(0, (mobPatch, entity) -> {
                                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(MobEffects.GLOWING,20));
                                                    if(entity instanceof LivingEntity livingEntity){
                                                        livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,3));
                                                    }
                                                })
                                        )

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .interruptedByLevel(3)
                                                .withinDistance(0,6)
                                                .animationBehavior(Animations.TACHI_AUTO3,0F)

                                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                        .canInterruptParent(true)
                                                        .withinDistance(0,6)
                                                        .animationBehavior(Animations.RUSHING_TEMPO3,0F)
                                                )
                                        )

                                        .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                .withinDistance(0,6)
                                                .animationBehavior(Animations.RUSHING_TEMPO3,new AnimationParams()
                                                        .transitionTime(0.25F).playSpeed(1.1F)
                                                        .addPhase(0, new PhaseParams()
                                                                .damageMultiplier(0.5F).impactMultiplier(1.5F).armorNegationMultiplier(2.0F)
                                                                .stunType(StunType.LONG).damageSource(Set.of(EpicFightDamageTypeTags.BYPASS_DODGE))
                                                        )
                                                )
                                                .addExBehavior(mobPatch -> {
                                                    mobPatch.playSound(SoundEvents.ANVIL_LAND,0,0);
                                                    CEParticleUtils.spawnWarningParticle(CEParticles.BYPASS_DODGE_WARNING.get(), mobPatch.getTarget(), new Vec3(0,1.25,0));
                                                })

                                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                                        .withinDistance(0,6)
                                                        .animationBehavior(Animations.RUSHING_TEMPO3,0F)
                                                )
                                        )
                                )
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1).maxCooldown(20)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .weight(2)
                                .withinDistance(0,6)
                                .guard(100)
                                .counterType(CECombatBehaviors.CounterType.NEVER)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .weight(1)
                                .withinDistance(0,6)
                                .guardWithWander(100,0,-0.5F,true)
                                .counterAnimation(Animations.THE_GUILLOTINE,0F)
                                .counterType(CECombatBehaviors.CounterType.RANDOM)
                                .counterChance(0.25)
                                .onCounterStart(mobPatch -> {
                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(),40));
                                })
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .weight(1)
                                .withinDistance(0,6)
                                .guardWithWander(100,0,0.5F,true)
                                .counterAnimation(Animations.THE_GUILLOTINE,0F)
                                .counterType(CECombatBehaviors.CounterType.END)
                                .maxGuardHit(3)
                                .onCounterStart(mobPatch -> {
                                    mobPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(),40));
                                })
                        )
                )

                .newBehaviorRoot(CECombatBehaviors.BehaviorRoot.builder()
                        .priority(1).weight(1).maxCooldown(200)

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .withinDistance(0,6)
                                .wander(100,0,1)
                        )

                        .addFirstBehavior(CECombatBehaviors.Behavior.builder()
                                .interruptedByTime(2.5F,5F)
                                .stopByStun(5)
                                .withinDistance(0,6)
                                .wanderWithAnimation(Animations.PIGLIN_CELEBRATE2,100,0,-1)

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .canInterruptParent(true)
                                        .attackLevel(1,2)
                                        .withinDistance(0,4)
                                        .animationBehavior(Animations.BIPED_STEP_BACKWARD,0F)
                                )

                                .addNextBehavior(CECombatBehaviors.Behavior.builder()
                                        .animationBehavior(Animations.BIPED_ROLL_FORWARD,0F)
                                )
                        )
                )
        ;
    }
}
