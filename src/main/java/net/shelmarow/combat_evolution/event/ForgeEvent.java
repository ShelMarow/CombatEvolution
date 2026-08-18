package net.shelmarow.combat_evolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEPatchReloadListener;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.client.shader.ExecutionShaderManager;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.execution.ExecutionMobReloadListener;
import net.shelmarow.combat_evolution.execution.ExecutionTypeReloadListener;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import yesman.epicfight.api.event.types.entity.StunnedEvent;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ForgeEvent {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || Minecraft.getInstance().player == null) return;

        if (!ExecutionShaderManager.isInitialized()) {
            ExecutionShaderManager.init();
        }

        ExecutionShaderManager.tick(event.getRenderTick(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            if(event.getPlayer().getServer() != null && !event.getPlayer().getServer().isSingleplayerOwner(event.getPlayer().getGameProfile())) {
                CENetworkHandler.sendToPlayer(event.getPlayer(), new SPCEDataPacket(CEPatchReloadListener.getSize(), CEPatchReloadListener.getTags()));
            }
        }
        else{
            event.getPlayerList().getPlayers().forEach(serverPlayer -> {
                CENetworkHandler.sendToPlayer(serverPlayer, new SPCEDataPacket(CEPatchReloadListener.getSize(), CEPatchReloadListener.getTags()));
            });
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new CEPatchReloadListener());
        event.addListener(new ExecutionTypeReloadListener());
        event.addListener(new ExecutionMobReloadListener());
    }

    public static void onStunApply(StunnedEvent event) {
        LivingEntity original = event.getEntityPatch().getOriginal();
        StunType stunType = event.getStunType();
        if(original.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY)){
            event.cancel();
        }
        else if(original.hasEffect(CEMobEffects.HIGH_STUN_IMMUNITY) && stunType != StunType.NEUTRALIZE){
            event.cancel();
        }
        else if(original.hasEffect(CEMobEffects.MIDDLE_STUN_IMMUNITY) &&
                stunType != StunType.NEUTRALIZE && stunType != StunType.FALL){
            event.cancel();
        }
        else if(original.hasEffect(CEMobEffects.NORMAL_STUN_IMMUNITY) &&
                stunType != StunType.NEUTRALIZE && stunType != StunType.KNOCKDOWN && stunType != StunType.FALL){
            event.cancel();
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if(target.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY) ||
                target.hasEffect(CEMobEffects.HIGH_STUN_IMMUNITY) ||
                target.hasEffect(CEMobEffects.MIDDLE_STUN_IMMUNITY) ||
                target.hasEffect(CEMobEffects.NORMAL_STUN_IMMUNITY)){
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event){
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if(attacker instanceof LivingEntity living && source instanceof EpicFightDamageSource epicFightDamageSource){
            if(living.hasEffect(CEMobEffects.BYPASS_DODGE_EFFECT)){
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.BYPASS_DODGE);
            }
            if(living.hasEffect(CEMobEffects.BYPASS_GUARD_EFFECT)){
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.UNBLOCKALBE);
                epicFightDamageSource.addRuntimeTag(EpicFightDamageTypeTags.GUARD_PUNCTURE);
            }
        }
    }
}
