package net.shelmarow.combat_evolution.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.gameassets.animation.MultiPhaseAttackAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

@Mixin(value = LivingEntityPatch.class,remap = false)
public abstract class LivingEntityPatchMixin<T extends LivingEntity> extends HurtableEntityPatch<T> {

    @Inject(
            method = "initAnimator",
            at = @At(value = "HEAD")
    )
    protected void initAnimator(Animator animator, CallbackInfo ci){
        animator.getVariables().putDefaultSharedVariable(MultiPhaseAttackAnimation.CE_PHASE_ATTACK_TRIED);
        animator.getVariables().putDefaultSharedVariable(MultiPhaseAttackAnimation.CE_PHASE_ACTUALLY_HIT);
    }

    @Inject(
            method = "applyStun",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void onApplyStun(StunType stunType, float time, CallbackInfoReturnable<Boolean> cir){
        LivingEntity entity = this.getOriginal();
        if(stunType != StunType.NEUTRALIZE && entity.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get())){
            entity.xxa = 0.0F;
            entity.yya = 0.0F;
            entity.zza = 0.0F;
            entity.setDeltaMovement(0.0F, 0.0F, 0.0F);
            this.cancelKnockback = true;
            cir.setReturnValue(false);
        }
    }
}
