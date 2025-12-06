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
import java.util.Set;

@Mixin(value = AttackAnimation.class)
public class EFAttackAnimation {

    @Shadow(remap = false)
    protected void spawnHitParticle(ServerLevel world, LivingEntityPatch<?> attacker, Entity hit, AttackAnimation.Phase phase){}

//    @Inject(
//            method = "getPlaySpeed",
//            at = @At("HEAD"),
//            cancellable = true,
//            remap = false
//    )
//    private void onGetPlaySpeed(LivingEntityPatch<?> entityPatch, DynamicAnimation animation,CallbackInfoReturnable<Float> cir) {
//        if(entityPatch instanceof CEHumanoidPatch) {
//            ILivingEntityData livingEntityData = (ILivingEntityData) entityPatch;
//            cir.setReturnValue(livingEntityData.combat_evolution$getAttackSpeed(entityPatch.getOriginal()));
//        }
//    }

    @Inject(
            method = "getEpicFightDamageSource(Lnet/minecraft/world/damagesource/DamageSource;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)Lyesman/epicfight/world/damagesource/EpicFightDamageSource;",
            at = @At(value = "RETURN"),
            cancellable = true,
            remap = false
    )
    private void onGetDamageSource(DamageSource originalSource, LivingEntityPatch<?> entitypatch, Entity target, AttackAnimation.Phase phase, CallbackInfoReturnable<EpicFightDamageSource> cir){
        if (entitypatch instanceof CEHumanoidPatch) {
            ILivingEntityData entityData = (ILivingEntityData) entitypatch;
            EpicFightDamageSource returnValue = cir.getReturnValue();
            float damage = entityData.combat_evolution$getDamageMultiplier(entitypatch.getOriginal());
            float impact = entityData.combat_evolution$getImpactMultiplier(entitypatch.getOriginal());
            float armorNegation = entityData.combat_evolution$getArmorNegationMultiplier(entitypatch.getOriginal());
            int stunIndex = entityData.combat_evolution$getStunType(entitypatch.getOriginal());
            Set<TagKey<DamageType>> sourceTag = entityData.combat_evolution$getDamageSource();

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


    @Redirect(
            method = "hurtCollidingEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/api/animation/types/AttackAnimation;spawnHitParticle(Lnet/minecraft/server/level/ServerLevel;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)V"
            ),
            remap = false
    )
    private void onHurtCollidingEntities(AttackAnimation instance, ServerLevel world, LivingEntityPatch<?> entityPatch, Entity target, AttackAnimation.Phase phase){
        spawnHitParticle((ServerLevel)target.level(), entityPatch, target, phase);
        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
            if(current != null){
                current.executeHitEvent(ceHumanoidPatch,target);
            }
        }
    }

}
