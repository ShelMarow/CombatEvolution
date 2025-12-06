package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.ai.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Set;

@Mixin(value = StaticAnimation.class,remap = false)
public class StaticAnimationsMixin {
    @Shadow
    protected AnimationManager.AnimationAccessor<? extends StaticAnimation> accessor;

    @Inject(
            method = "end",
            at = @At(value = "HEAD")
    )
    public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd, CallbackInfo ci){
        if(entitypatch instanceof CEHumanoidPatch ceHumanoidPatch){

            //动画结束时重置属性
            if(ceHumanoidPatch instanceof ILivingEntityData livingEntityData) {
                livingEntityData.combat_evolution$setCanModifySpeed(entitypatch.getOriginal(), false);
                livingEntityData.combat_evolution$setAttackSpeed(entitypatch.getOriginal(), 1F);
                livingEntityData.combat_evolution$setDamageMultiplier(entitypatch.getOriginal(), 1F);
                livingEntityData.combat_evolution$setImpactMultiplier(entitypatch.getOriginal(), 1F);
                livingEntityData.combat_evolution$setArmorNegationMultiplier(entitypatch.getOriginal(),1F);
                livingEntityData.combat_evolution$setStunType(entitypatch.getOriginal(), -1);
                if((StaticAnimation)(Object)this instanceof AttackAnimation) {
                    livingEntityData.combat_evolution$setDamageSource(Set.of());
                }
            }

            //重置时间戳事件和命中事件
            CECombatBehaviors.Behavior<?> behavior = BehaviorUtils.getCurrentBehavior(ceHumanoidPatch);
            if(behavior != null){
                behavior.resetTimeEventAvailable();
                behavior.setShouldExecuteTimeEvent(false);
                behavior.setShouldExecuteHitEvent(false);
            }
        }
    }
}
