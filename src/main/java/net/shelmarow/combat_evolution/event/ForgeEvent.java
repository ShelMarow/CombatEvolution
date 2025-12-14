package net.shelmarow.combat_evolution.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
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
}
