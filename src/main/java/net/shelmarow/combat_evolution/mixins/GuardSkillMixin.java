package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.ai.CEBossEntity;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

@Mixin(value = GuardSkill.class, remap = false)
public class GuardSkillMixin {
    @Inject(
            method = "dealEvent",
            at = @At("HEAD")
    )
    private void ontDealEvent(PlayerPatch<?> playerpatch, TakeDamageEvent.Attack event, boolean advanced, CallbackInfo ci){
        if(event.isParried()){
            LivingEntityPatch<?> livingEntityPatch = EpicFightCapabilities.getEntityPatch(event.getDamageSource().getDirectEntity(),LivingEntityPatch.class);
            if(livingEntityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
                ceHumanoidPatch.onAttackParried(event.getDamageSource(), playerpatch);
            }
        }
    }
}
