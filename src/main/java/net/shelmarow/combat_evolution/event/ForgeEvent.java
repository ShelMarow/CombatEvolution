package net.shelmarow.combat_evolution.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEPatchReloadListener;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvent {


    @SubscribeEvent
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            if(event.getPlayer().getServer() != null && !event.getPlayer().getServer().isSingleplayerOwner(event.getPlayer().getGameProfile())) {
                CombatEvolution.CHANNEL.send(PacketDistributor.PLAYER.with(event::getPlayer), new SPCEDataPacket(CEPatchReloadListener.getSize(), CEPatchReloadListener.getTags()));
            }
        }
        else{
            event.getPlayerList().getPlayers().forEach(serverPlayer -> {
                CombatEvolution.CHANNEL.send(PacketDistributor.PLAYER.with(()->serverPlayer), new SPCEDataPacket(CEPatchReloadListener.getSize(), CEPatchReloadListener.getTags()));
            });
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new CEPatchReloadListener());
    }

    @SubscribeEvent
    public static void onStunApply(EntityStunEvent event) {
        LivingEntity original = event.getStunnedEntityPatch().getOriginal();
        StunType stunType = event.getStunType();
        if(original.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get()) && stunType != StunType.NEUTRALIZE){
            event.setCanceled(true);
        }
        else if(original.hasEffect(CEMobEffects.HIGH_STUN_IMMUNITY.get()) &&
                stunType != StunType.NEUTRALIZE && stunType != StunType.FALL){
            event.setCanceled(true);
        }
        else if(original.hasEffect(CEMobEffects.MIDDLE_STUN_IMMUNITY.get()) &&
                stunType != StunType.NEUTRALIZE && stunType != StunType.KNOCKDOWN && stunType != StunType.FALL){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if(target.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get()) ||
                target.hasEffect(CEMobEffects.HIGH_STUN_IMMUNITY.get()) ||
                target.hasEffect(CEMobEffects.MIDDLE_STUN_IMMUNITY.get())){
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
}
