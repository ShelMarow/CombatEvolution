package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = StaticAnimation.class,remap = false)
public class StaticAnimationsMixin {

    @Inject(
            method = "end",
            at = @At(value = "HEAD")
    )
    public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd, CallbackInfo ci){
        if(entitypatch instanceof CEHumanoidPatch ceHumanoidPatch){

            //动画结束时重置属性
            if(ceHumanoidPatch instanceof ILivingEntityData livingEntityData) {
                livingEntityData.combat_evolution$setCanModifySpeed(false);
                livingEntityData.combat_evolution$setAttackSpeed(1F);
            }

            //重置时间戳事件和命中事件等等
            CECombatBehaviors.Behavior<?> behavior = BehaviorUtils.getCurrentBehavior(ceHumanoidPatch);
            if(behavior != null){
                behavior.resetTimeEventAvailable();
                behavior.setCanApplyPhaseParam(false);
                behavior.setShouldExecuteTimeEvent(false);
                behavior.setShouldExecuteHitEvent(false);
            }
        }
    }
}
