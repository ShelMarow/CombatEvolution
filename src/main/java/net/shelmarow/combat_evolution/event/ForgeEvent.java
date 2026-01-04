package net.shelmarow.combat_evolution.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.iml.IDamageSourceData;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvent {

    @SubscribeEvent
    public static void onStunApply(EntityStunEvent event) {
        LivingEntity original = event.getStunnedEntityPatch().getOriginal();
        StunType stunType = event.getStunType();
        if(stunType != StunType.NEUTRALIZE && original.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get())){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if(target.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get())){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event){
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if(attacker instanceof LivingEntity living && source instanceof EpicFightDamageSource epicFightDamageSource){
            if(living.hasEffect(CEMobEffects.BYPASS_DODGE_EFFECT.get())){
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.BYPASS_DODGE);
            }
            if(living.hasEffect(CEMobEffects.BYPASS_GUARD_EFFECT.get())){
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.UNBLOCKALBE);
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.GUARD_PUNCTURE);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event){
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity target = event.getEntity();

        if(source instanceof IDamageSourceData damageSourceData){
            CEHumanoidPatch ceHumanoidPatch = EpicFightCapabilities.getEntityPatch(attacker, CEHumanoidPatch.class);
            if(ceHumanoidPatch != null){
                CECombatBehaviors.Behavior<?> current = BehaviorUtils.getCurrentBehavior(ceHumanoidPatch);
                if(current != null && current.shouldExecuteHitEvent()){
                    current.executeHitEvent(damageSourceData.getSourcePhaseIndex(), ceHumanoidPatch, target);
                }
            }
        }
    }
}
