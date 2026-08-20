package net.shelmarow.combat_evolution.event;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.CEPatchReloadListener;
import net.shelmarow.combat_evolution.ai.network.SPCEDataPacket;
import net.shelmarow.combat_evolution.client.shader.ExecutionShaderManager;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.execution.ExecutionMobReloadListener;
import net.shelmarow.combat_evolution.execution.ExecutionTypeReloadListener;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvent {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || Minecraft.getInstance().player == null) return;

        if (!ExecutionShaderManager.isInitialized()) {
            ExecutionShaderManager.init();
        }

        ExecutionShaderManager.tick(event.getRenderTick(), event.getPartialTick());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyInput(InputEvent event){
        LocalPlayer player = Minecraft.getInstance().player;
        if(player != null){
            if(player.hasEffect(CEMobEffects.ON_EXECUTION.get())){
                if(event.isCancelable()){
                    event.setCanceled(true);
                }
            }
        }
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

    @SubscribeEvent
    public static void onStunApply(EntityStunEvent event) {
        LivingEntity original = event.getStunnedEntityPatch().getOriginal();
        StunType stunType = event.getStunType();
        boolean canStun = CEHumanoidPatch.canStun(original, stunType);
        event.setCanceled(!canStun);
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if(target.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get()) ||
                target.hasEffect(CEMobEffects.HIGH_STUN_IMMUNITY.get()) ||
                target.hasEffect(CEMobEffects.MIDDLE_STUN_IMMUNITY.get()) ||
                target.hasEffect(CEMobEffects.NORMAL_STUN_IMMUNITY.get())){
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
