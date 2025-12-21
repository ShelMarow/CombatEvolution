package net.shelmarow.combat_evolution.mixins;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.params.PhaseParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;

import java.util.*;

@Mixin(value = AttackAnimation.class)
public abstract class EFAttackAnimation extends StaticAnimation {

    @Shadow(remap = false)
    @Final
    public AttackAnimation.Phase[] phases;

    @Unique
    private int combatEvolution$getCurrentPhaseOrder(AttackAnimation.Phase[] phases, AttackAnimation.Phase phase) {
        return Arrays.stream(phases).toList().indexOf(phase);
    }

    @Inject(
            method = "getEpicFightDamageSource(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)Lyesman/epicfight/world/damagesource/EpicFightDamageSource;",
            at = @At(value = "RETURN"),
            cancellable = true,
            remap = false
    )
    private void onGetDamageSource(LivingEntityPatch<?> entityPatch, Entity target, AttackAnimation.Phase phase, CallbackInfoReturnable<EpicFightDamageSource> cir){
        if (entityPatch instanceof CEHumanoidPatch) {

            Map<Integer, PhaseParams> phaseParamsMap = BehaviorUtils.getPhaseParams(entityPatch);

            if (!phaseParamsMap.isEmpty()) {
                //获取当前Phase可用的参数
                int currentPhase = this.combatEvolution$getCurrentPhaseOrder(this.phases, phase);
                PhaseParams params = phaseParamsMap.containsKey(currentPhase) ? phaseParamsMap.get(currentPhase) : phaseParamsMap.get(-1);

                int stunIndex = params.getStunType();
                float damage = params.getDamageMultiplier();
                float impact = params.getImpactMultiplier();
                float armorNegation = params.getArmorNegationMultiplier();
                Set<TagKey<DamageType>> sourceTag = params.getDamageSource();

                EpicFightDamageSource returnValue = cir.getReturnValue();

                if (stunIndex != -1) {
                    StunType stunType = StunType.values()[stunIndex];
                    returnValue.setStunType(stunType);
                }

                returnValue.attachDamageModifier(ValueModifier.multiplier(damage));
                returnValue.attachImpactModifier(ValueModifier.multiplier(impact));
                returnValue.attachArmorNegationModifier(ValueModifier.multiplier(armorNegation));
                sourceTag.forEach(returnValue::addRuntimeTag);

                cir.setReturnValue(returnValue);
            }
        }
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
    private void onAttack(LivingEntityPatch<?> entityPatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, AttackAnimation.Phase phase, CallbackInfo ci,
                          LivingEntity entity,float prevPoseTime, float poseTime, List<Entity> list, HitEntityList hitEntities, int maxStrikes, Entity target, LivingEntity trueEntity, AABB aabb,
                          EpicFightDamageSource damagesource, int prevInvulTime, AttackResult attackResult){
        if(entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
            CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(entityPatch);
            if(current != null && current.shouldExecuteHitEvent()){
                int currentPhase = this.combatEvolution$getCurrentPhaseOrder(this.phases,phase);
                current.executeHitEvent(currentPhase,attackResult.resultType,ceHumanoidPatch,target);
            }
        }
    }
}
