package net.shelmarow.combat_evolution.mixins;

import com.merlin204.avalon.epicfight.animations.AvalonAttackAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

import java.util.List;

@Mixin(value = AvalonAttackAnimation.class)
public abstract class AvalonAttackAnimationMixin extends BasicAttackAnimation{

    public AvalonAttackAnimationMixin(float transitionTime, float antic, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, antic, contact, recovery, collider, colliderJoint, accessor, armature);
    }


//    @Inject(
//            method = "getPlaySpeed",
//            at = @At("HEAD"),
//            cancellable = true,
//            remap = false
//    )
//    private void onGetPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation, CallbackInfoReturnable<Float> cir) {
//        if(entityPatch instanceof CEHumanoidPatch) {
//            ILivingEntityData livingEntityData = (ILivingEntityData) entityPatch;
//            cir.setReturnValue(livingEntityData.combat_evolution$getAttackSpeed(entityPatch.getOriginal()));
//        }
//    }

//    @Inject(
//            method = "getEpicFightDamageSource(Lnet/minecraft/world/damagesource/DamageSource;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)Lyesman/epicfight/world/damagesource/EpicFightDamageSource;",
//            at = @At(value = "RETURN"),
//            cancellable = true,
//            remap = false
//    )
//    private void onGetDamageSource(DamageSource originalSource, LivingEntityPatch<?> entitypatch, Entity target, AttackAnimation.Phase phase, CallbackInfoReturnable<EpicFightDamageSource> cir){
//        if (entitypatch instanceof CEHumanoidPatch) {
//            ILivingEntityData entityData = (ILivingEntityData) entitypatch;
//            EpicFightDamageSource returnValue = cir.getReturnValue();
//            float damage = entityData.combat_evolution$getDamageMultiplier(entitypatch.getOriginal());
//            float impact = entityData.combat_evolution$getImpactMultiplier(entitypatch.getOriginal());
//            float armorNegation = entityData.combat_evolution$getArmorNegationMultiplier(entitypatch.getOriginal());
//            int stunIndex = entityData.combat_evolution$getStunType(entitypatch.getOriginal());
//            Set<TagKey<DamageType>> sourceTag = entityData.combat_evolution$getDamageSource();
//
//            if(stunIndex != -1){
//                StunType stunType = StunType.values()[stunIndex];
//                returnValue.setStunType(stunType);
//            }
//
//            returnValue.attachDamageModifier(ValueModifier.multiplier(damage));
//            returnValue.attachImpactModifier(ValueModifier.multiplier(impact));
//            returnValue.attachArmorNegationModifier(ValueModifier.multiplier(armorNegation));
//            if(!sourceTag.isEmpty()) {
//                sourceTag.forEach(returnValue::addRuntimeTag);
//            }
//
//            cir.setReturnValue(returnValue);
//        }
//    }
//    @Redirect(
//            method = "hurtCollidingEntities",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lcom/merlin204/avalon/epicfight/animations/AvalonAttackAnimation;spawnHitParticle(Lnet/minecraft/server/level/ServerLevel;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)V"
//            ),
//            remap = false
//    )
//    private void onHurtCollidingEntities(AvalonAttackAnimation instance, ServerLevel level, LivingEntityPatch<?> entityPatch, Entity target, AttackAnimation.Phase phase){
//        spawnHitParticle((ServerLevel)target.level(), entityPatch, target, phase);
//        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
//            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
//            if(current != null){
//                int currentPhase = instance.getPhaseOrderByTime(Objects.requireNonNull(entityPatch.getAnimator().getPlayerFor(null)).getElapsedTime());
//                current.executeHitEvent(currentPhase,ceHumanoidPatch,target);
//            }
//        }
//    }


    @Inject(
            method = "hurtCollidingEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    ordinal = 0
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false
    )
    private void onAttack(LivingEntityPatch<?> entityPatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, Phase phase, CallbackInfo ci,
                          LivingEntity entity, float phasePrevTime, float phaseCurrentTime, float phasePreDelay, float phaseContact, List list, HitEntityList hitEntities,
                          int maxStrikes, Entity target, LivingEntity trueEntity, boolean canAttack, EpicFightDamageSource damagesource, int prevInvulTime, AttackResult attackResult){
        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
            if(current != null){
                int currentPhase = this.getPhaseOrderByTime(elapsedTime);
                current.executeHitEvent(currentPhase,attackResult.resultType,ceHumanoidPatch,target);
            }
        }
    }
}
