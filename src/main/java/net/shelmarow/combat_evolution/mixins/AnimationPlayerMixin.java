package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.ai.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = AnimationPlayer.class)
public abstract class AnimationPlayerMixin {

    @Shadow(remap = false)
    protected float elapsedTime;
    @Shadow(remap = false)
    protected float prevElapsedTime;

    @Unique
    private LivingEntityPatch<?> combatEvolution$storeEntityPatch;
    @Unique
    DynamicAnimation combatEvolution$currentPlay;

    @Inject(
            method = "tick",
            at = @At(value = "HEAD"),
            remap = false
    )
    private void tick(LivingEntityPatch<?> entityPatch, CallbackInfo ci){
        combatEvolution$storeEntityPatch = entityPatch;
        combatEvolution$currentPlay = ((AnimationPlayer)(Object) this).getAnimation().get();
        if(entityPatch instanceof CEHumanoidPatch nfiHumanoidPatch){
            CECombatBehaviors.Behavior<?> behavior = BehaviorUtils.getCurrentBehavior(nfiHumanoidPatch);
            if(behavior != null){
                behavior.executeTimeEvent(prevElapsedTime,elapsedTime,nfiHumanoidPatch);
            }
        }
    }

    @ModifyVariable(
            method = "tick",
            at = @At("STORE"),
            name = "playbackSpeed",
            remap = false
    )
    private float modifyPlaybackSpeed(float originalValue) {
        if(combatEvolution$currentPlay instanceof ActionAnimation && combatEvolution$storeEntityPatch instanceof CEHumanoidPatch){
            ILivingEntityData livingEntityData = (ILivingEntityData) combatEvolution$storeEntityPatch;
            return livingEntityData.combat_evolution$getAttackSpeed(combatEvolution$storeEntityPatch.getOriginal());
        }
        return originalValue;
    }



    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/api/animation/property/AnimationProperty$PlaybackSpeedModifier;modify(Lyesman/epicfight/api/animation/types/DynamicAnimation;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;FFF)F",
                    ordinal = 0
            ),
            remap = false
    )
    private float redirectModify(AnimationProperty.PlaybackSpeedModifier instance, DynamicAnimation dynamicAnimation, LivingEntityPatch<?> entityPatch, float playbackSpeed, float prevElapsedTime, float elapsedTime){
        if(dynamicAnimation instanceof ActionAnimation && entityPatch instanceof CEHumanoidPatch){
            return playbackSpeed;
        }
        return instance.modify(dynamicAnimation, entityPatch, playbackSpeed, prevElapsedTime, elapsedTime);
    }
}
