package net.shelmarow.combat_evolution.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.shelmarow.combat_evolution.ai.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Mixin(value = AttackAnimation.class)
public abstract class EFAttackAnimation {

    @Shadow(remap = false)
    public abstract int getPhaseOrderByTime(float elapsedTime);

    @Inject(
            method = "getEpicFightDamageSource(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)Lyesman/epicfight/world/damagesource/EpicFightDamageSource;",
            at = @At(value = "RETURN"),
            cancellable = true,
            remap = false
    )
    private void onGetDamageSource(LivingEntityPatch<?> entityPatch, Entity target, AttackAnimation.Phase phase, CallbackInfoReturnable<EpicFightDamageSource> cir){
        if (entityPatch instanceof CEHumanoidPatch) {
            ILivingEntityData entityData = (ILivingEntityData) entityPatch;
            EpicFightDamageSource returnValue = cir.getReturnValue();
            float damage = entityData.combat_evolution$getDamageMultiplier(entityPatch.getOriginal());
            float impact = entityData.combat_evolution$getImpactMultiplier(entityPatch.getOriginal());
            float armorNegation = entityData.combat_evolution$getArmorNegationMultiplier(entityPatch.getOriginal());
            int stunIndex = entityData.combat_evolution$getStunType(entityPatch.getOriginal());
            Set<TagKey<DamageType>> sourceTag = BehaviorUtils.getSourceTagSet(entityPatch);

            if(stunIndex != -1){
                StunType stunType = StunType.values()[stunIndex];
                returnValue.setStunType(stunType);
            }

            returnValue.attachDamageModifier(ValueModifier.multiplier(damage));
            returnValue.attachImpactModifier(ValueModifier.multiplier(impact));
            returnValue.attachArmorNegationModifier(ValueModifier.multiplier(armorNegation));
            if(!sourceTag.isEmpty()) {
                sourceTag.forEach(returnValue::addRuntimeTag);
            }

            cir.setReturnValue(returnValue);
        }
    }

//    @Inject(
//            method = "hurtCollidingEntities",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lyesman/epicfight/api/animation/types/AttackAnimation;spawnHitParticle(Lnet/minecraft/server/level/ServerLevel;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)V"
//            ),
//            locals = LocalCapture.CAPTURE_FAILHARD,
//            remap = false
//    )
//    private void onHurtCollidingEntities(LivingEntityPatch<?> entityPatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state,
//                                         AttackAnimation.Phase phase, CallbackInfo ci, LivingEntity entity, float prevPoseTime, float poseTime, List<Entity> list,
//                                         HitEntityList hitEntities, int maxStrikes, Entity target, LivingEntity trueEntity, AABB aabb, EpicFightDamageSource damagesource,
//                                         int prevInvulTime, AttackResult attackResult){
//        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
//            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
//            if(current != null){
//                int currentPhase = this.getPhaseOrderByTime(elapsedTime);
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
    private void onAttack(LivingEntityPatch<?> entityPatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, AttackAnimation.Phase phase, CallbackInfo ci,
                          LivingEntity entity,float prevPoseTime, float poseTime, List<Entity> list, HitEntityList hitEntities, int maxStrikes, Entity target, LivingEntity trueEntity, AABB aabb,
                          EpicFightDamageSource damagesource, int prevInvulTime, AttackResult attackResult){
        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
            if(current != null){
                int currentPhase = this.getPhaseOrderByTime(elapsedTime);
                current.executeHitEvent(currentPhase,attackResult.resultType,ceHumanoidPatch,target);
            }
        }
    }
}
