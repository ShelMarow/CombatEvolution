package net.shelmarow.combat_evolution.mixins;

import com.merlin204.avalon.epicfight.animations.AvalonAttackAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

import java.util.Arrays;
import java.util.List;

@Mixin(value = AvalonAttackAnimation.class)
public abstract class AvalonAttackAnimationMixin extends BasicAttackAnimation{

    public AvalonAttackAnimationMixin(float transitionTime, float antic, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(transitionTime, antic, contact, recovery, collider, colliderJoint, accessor, armature);
    }


    @Unique
    private int combatEvolution$getCurrentPhaseOrder(AttackAnimation.Phase[] phases, AttackAnimation.Phase phase) {
        return List.of(phases).indexOf(phase);
    }

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
            if(current != null && current.shouldExecuteHitEvent()){
                int currentPhase = combatEvolution$getCurrentPhaseOrder(this.phases, phase);
                current.executeHitEvent(currentPhase,attackResult.resultType,ceHumanoidPatch,target);
            }
        }
    }
}
