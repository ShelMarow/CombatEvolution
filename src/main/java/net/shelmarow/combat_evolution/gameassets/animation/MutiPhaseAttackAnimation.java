package net.shelmarow.combat_evolution.gameassets.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.entity.eventlistener.AttackPhaseEndEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.*;

public class MutiPhaseAttackAnimation extends AttackAnimation {
    //碰撞箱命中的实体
    public static final AnimationVariables.SharedAnimationVariableKey<Map<Phase, List<Entity>>> CE_PHASE_ATTACK_TRIED =
            AnimationVariables.shared((animator) -> new HashMap<>(), false);
    //造成伤害的实体
    public static final AnimationVariables.SharedAnimationVariableKey<Map<Phase, List<LivingEntity>>> CE_PHASE_ACTUALLY_HIT =
            AnimationVariables.shared((animator) -> new HashMap<>(), false);


    public MutiPhaseAttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, antic, preDelay, contact, recovery, collider, colliderJoint, accessor, armature);
    }

    public MutiPhaseAttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, antic, preDelay, contact, recovery, hand, collider, colliderJoint, accessor, armature);
    }

    public MutiPhaseAttackAnimation(float transitionTime, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature, Phase... phases) {
        super(transitionTime, accessor, armature, phases);
    }

    public MutiPhaseAttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, String path, AssetAccessor<? extends Armature> armature) {
        super(convertTime, antic, preDelay, contact, recovery, hand, collider, colliderJoint, path, armature);
    }

    public MutiPhaseAttackAnimation(float convertTime, String path, AssetAccessor<? extends Armature> armature, Phase... phases) {
        super(convertTime, path, armature, phases);
    }


    protected void attackTick(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> animation) {
        AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(this.getAccessor());
        float prevElapsedTime = player.getPrevElapsedTime();
        float elapsedTime = player.getElapsedTime();
        EntityState prevState = animation.get().getState(entitypatch, prevElapsedTime);
        EntityState state = animation.get().getState(entitypatch, elapsedTime);

        for (Phase attackPhase : this.phases){
            if (animation.get().isLinkAnimation()) break;
            boolean preAttack = prevElapsedTime >= attackPhase.preDelay && prevElapsedTime <= attackPhase.contact;
            boolean currentAttack = elapsedTime >= attackPhase.preDelay && elapsedTime <= attackPhase.contact;

            if(preAttack || currentAttack) {
                if (!preAttack) {
                    entitypatch.playSound(this.getSwingSound(entitypatch, attackPhase), 0.0F, 0.0F);
                    entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ATTACK_TRIED).remove(attackPhase);
                    entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ACTUALLY_HIT).remove(attackPhase);
                }

                this.hurtCollidingEntities(entitypatch, prevElapsedTime, elapsedTime, prevState, state, attackPhase);

                if ((elapsedTime > attackPhase.contact || elapsedTime >= this.getTotalTime()) && entitypatch instanceof ServerPlayerPatch playerpatch) {
                    playerpatch.getEventListener().triggerEvents(PlayerEventListener.EventType.ATTACK_PHASE_END_EVENT, new AttackPhaseEndEvent(playerpatch, this.getAccessor(), attackPhase, List.of(this.phases).indexOf(attackPhase)));
                }
            }
        }
    }


    protected void hurtCollidingEntities(LivingEntityPatch<?> entitypatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, Phase phase) {
        LivingEntity entity = entitypatch.getOriginal();

        boolean preAttack = prevElapsedTime >= phase.preDelay && prevElapsedTime <= phase.contact;
        boolean currentAttack = elapsedTime >= phase.preDelay && elapsedTime <= phase.contact;

        float prevPoseTime = preAttack ? prevElapsedTime : phase.preDelay;
        float poseTime =currentAttack ? elapsedTime : phase.contact;
        List<Entity> list = phase.getCollidingEntities(entitypatch, this, prevPoseTime, poseTime, this.getPlaySpeed(entitypatch, this));

        if (!list.isEmpty()) {
            HitEntityList hitEntities = new HitEntityList(entitypatch, list, phase.getProperty(AnimationProperty.AttackPhaseProperty.HIT_PRIORITY).orElse(HitEntityList.Priority.DISTANCE));
            int maxStrikes = this.getMaxStrikes(entitypatch, phase);

            List<LivingEntity> attackTriedEntities = entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ACTUALLY_HIT).computeIfAbsent(phase, k -> new ArrayList<>());
            while (attackTriedEntities.size() < maxStrikes && hitEntities.next()) {
                Entity target = hitEntities.getEntity();
                LivingEntity trueEntity = this.getTrueEntity(target);

                boolean containsTarget = entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ATTACK_TRIED).getOrDefault(phase, new ArrayList<>()).contains(trueEntity);
                if (trueEntity != null && trueEntity.isAlive() && !containsTarget && !entitypatch.isTargetInvulnerable(target)) {
                    if (target instanceof LivingEntity || target instanceof PartEntity) {
                        AABB aabb = target.getBoundingBox();

                        if (MathUtils.canBeSeen(target, entity, target.position().distanceTo(entity.getEyePosition()) + aabb.getCenter().distanceTo(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)))) {
                            EpicFightDamageSource damagesource = this.getEpicFightDamageSource(entitypatch, target, phase);
                            int prevInvulTime = target.invulnerableTime;
                            target.invulnerableTime = 0;

                            AttackResult attackResult = entitypatch.attack(damagesource, target, phase.hand);
                            target.invulnerableTime = prevInvulTime;

                            if (attackResult.resultType.dealtDamage()) {
                                SoundEvent hitSound = this.getHitSound(entitypatch, phase);

                                if (hitSound != null) {
                                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), this.getHitSound(entitypatch, phase), target.getSoundSource(), 1.0F, 1.0F);
                                }

                                this.spawnHitParticle((ServerLevel)target.level(), entitypatch, target, phase);
                            }

                            entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ATTACK_TRIED).computeIfAbsent(phase, k -> new ArrayList<>()).add(trueEntity);

                            if (attackResult.resultType.shouldCount()) {
                                entitypatch.getAnimator().getVariables().getOrDefaultSharedVariable(CE_PHASE_ACTUALLY_HIT).computeIfAbsent(phase, k -> new ArrayList<>()).add(trueEntity);
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderDebugging(PoseStack poseStack, MultiBufferSource buffer, LivingEntityPatch<?> entitypatch, float playbackTime, float partialTicks) {
        AnimationPlayer animPlayer = entitypatch.getAnimator().getPlayerFor(this.getAccessor());
        float prevElapsedTime = animPlayer.getPrevElapsedTime();
        float elapsedTime = animPlayer.getElapsedTime();

        List<Phase> reversedPhases = new ArrayList<>(List.of(this.phases));
        Collections.reverse(reversedPhases);
        for (Phase phase : reversedPhases){
            if(prevElapsedTime < phase.start || elapsedTime < phase.start || prevElapsedTime > phase.end || elapsedTime > phase.end) continue;
            for (Pair<Joint, Collider> colliderInfo : phase.colliders) {
                Collider collider = colliderInfo.getSecond();

                if (collider == null) {
                    collider = entitypatch.getColliderMatching(phase.hand);
                }

                Armature armature = entitypatch.getArmature();
                boolean preAttack = prevElapsedTime >= phase.preDelay && prevElapsedTime <= phase.contact;
                boolean currentAttack = elapsedTime >= phase.preDelay && elapsedTime <= phase.contact;
                boolean attacking = preAttack || currentAttack;

                Pose prevPose;
                Pose currentPose;

                if (colliderInfo.getFirst().getName().equals(armature.rootJoint.getName())) {
                    prevPose = new Pose();
                    currentPose = new Pose();
                    prevPose.putJointData("Root", JointTransform.empty());
                    currentPose.putJointData("Root", JointTransform.empty());
                    modifyPose(this, prevPose, entitypatch, prevElapsedTime, 0.0F);
                    modifyPose(this, currentPose, entitypatch, elapsedTime, 1.0F);
                } else {
                    prevPose = getPoseByTime(entitypatch, prevElapsedTime, 0.0F);
                    currentPose = getPoseByTime(entitypatch, elapsedTime, 1.0F);
                }

                collider.drawInternal(poseStack, buffer.getBuffer(collider.getRenderType()), armature, colliderInfo.getFirst(), prevPose, currentPose, partialTicks, attacking ? 0xFFFF0000 : -1);

            }
        }
    }
}
