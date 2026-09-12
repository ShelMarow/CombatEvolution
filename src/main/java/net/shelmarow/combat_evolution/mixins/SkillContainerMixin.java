package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.effect.CEMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

@Mixin(value = SkillContainer.class, remap = false)
public class SkillContainerMixin {

    @Inject(
            method = "canUse",
            at = @At("HEAD"),
            cancellable = true
    )
    private void canUse(PlayerPatch<?> executor, SkillCastEvent event, CallbackInfoReturnable<Boolean> cir){
        if(executor.getOriginal().hasEffect(CEMobEffects.ON_EXECUTION.get())){
            cir.setReturnValue(false);
        }
    }
}
