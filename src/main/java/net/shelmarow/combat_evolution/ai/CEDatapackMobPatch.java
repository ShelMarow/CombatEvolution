package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.bgm.CEMusic;
import net.shelmarow.combat_evolution.bgm.network.CEMusicNetworkHandler;
import net.shelmarow.combat_evolution.bossbar.CEBossEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.StunType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CEDatapackMobPatch extends CEHumanoidPatch<Mob>{
    private final CEPatchReloadListener.CEDatapackMobPatchProvider provider;
    protected final CEBossEvent ceBossEvent = new CEBossEvent(Component.empty());
    private boolean shouldPlayBGM = false;
    private final UUID bgmUUID = UUID.randomUUID();
    private CEMusic music;

    public CEDatapackMobPatch(CEPatchReloadListener.CEDatapackMobPatchProvider provider) {
        super(provider.faction);
        this.provider = provider;
        this.ceBossEvent.setVisible(false);

        this.chasingSpeed = provider.chasingSpeed;

        this.breakTime = provider.breakTime;
        this.recoverTime = provider.recoverTime;
        this.staminaRegenDelay = provider.staminaRegenDelay;

        this.weaponLivingMotions.putAll(provider.weaponLivingMotions);
        this.guardHitMotions.putAll(provider.guardHitMotions);
        for (Map.Entry<WeaponCategory, Map<Style, Supplier<CECombatBehaviors.Builder<MobPatch<?>>>>> entry : provider.weaponAttackMotions.entrySet()){
            Map<Style, CECombatBehaviors.Builder<MobPatch<?>>> newInner = new HashMap<>();

            for (Map.Entry<Style, Supplier<CECombatBehaviors.Builder<MobPatch<?>>>> inner : entry.getValue().entrySet()) {
                newInner.put(inner.getKey(), inner.getValue().get());
            }

            this.weaponAttackMotions.put(entry.getKey(), newInner);
        }

        if(provider.playBGM && provider.bgm != null){
            music = new CEMusic(
                    provider.bgm, SoundSource.RECORDS, bgmUUID,
                    provider.bgmVolume, provider.bgmDuration,
                    provider.bgmLoop, true,
                    provider.bgmFadeIn,provider.bgmFadeOut
            );
        }
    }

    @Override
    protected void setWeaponMotions() {
    }

    @Override
    public void onAddedToWorld(){
        initBossBar();
        putAndSetCustomAttributes();
        super.onAddedToWorld();
    }

    private void initBossBar() {
        ceBossEvent.setVisible(provider.enableBossBar);
        if(!provider.bossBarName.equals("[CE:EMPTY_NAME]")){
            ceBossEvent.setName(Component.translatable(provider.bossBarName));
        }
        else {
            ceBossEvent.setName(original.getDisplayName());
        }
        if(provider.bossBarTexture != null){
            ceBossEvent.setBossBarTexture(provider.bossBarTexture);
        }
    }

    public void putAndSetCustomAttributes() {
        Map<Attribute, AttributeInstance> newMap = Maps.newHashMap();
        AttributeSupplier.Builder builder = AttributeSupplier.builder();

        for (Attribute attribute : this.provider.attributeMap.keySet()) {
            builder.add(attribute);
        }

        AttributeSupplier supplier = builder.build();
        newMap.putAll(supplier.instances);
        newMap.putAll(original.getAttributes().supplier.instances);
        original.getAttributes().supplier.instances = ImmutableMap.copyOf(newMap);

        for (Map.Entry<Attribute, Double> entrySet : provider.attributeMap.entrySet()) {
            AttributeInstance instance = this.original.getAttribute(entrySet.getKey());
            if(instance != null){
                instance.setBaseValue(entrySet.getValue());
            }
        }
    }

    @Override
    public void tick(LivingEvent.LivingTickEvent event) {
        super.tick(event);

        if(!isLogicalClient()){
            if(ceBossEvent.isVisible()){
                ceBossEvent.setProgress(Mth.clamp(original.getHealth() / original.getMaxHealth(),0,1));
                ceBossEvent.setStaminaStatus(CEPatchUtils.getStaminaStatus(this));
                ceBossEvent.setStamina(CEPatchUtils.getStaminaPercent(this));
            }

            if(music != null){
                boolean hasTarget = getTarget() != null;
                if(!shouldPlayBGM && hasTarget){
                    shouldPlayBGM = true;
                    for (ServerPlayer serverPlayer : ceBossEvent.getPlayers()){
                        CEMusicNetworkHandler.sendRequestPlayPacket(serverPlayer, music);
                    }
                }
                else if(shouldPlayBGM && !hasTarget){
                    shouldPlayBGM = false;
                    for (ServerPlayer serverPlayer : ceBossEvent.getPlayers()) {
                        CEMusicNetworkHandler.sendRemoveMusicPacket(serverPlayer, bgmUUID, false);
                    }
                }
            }

        }
    }

    @Override
    public float getGuardHitImpactPercent(DamageSource damageSource){
        return provider.guardHitImpact;
    }

    @Override
    public float getHurtImpactPercent(DamageSource damageSource){
        return provider.hurtImpact;
    }

    @Override
    public void onAttackParried(DamageSource damageSource, LivingEntityPatch<?> blocker) {
        super.onAttackParried(damageSource, blocker);
        dealStaminaDamage(null, provider.beParriedDamage);
    }

    @Override
    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> stunAnimations = provider.stunAnimations;
        return stunAnimations.get(stunType);
    }


    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        float scale = provider.scale;
        return super.getModelMatrix(partialTicks).scale(scale, scale, scale);
    }

    @Override
    public void onDeath(LivingDeathEvent event) {
        super.onDeath(event);
        if(!isLogicalClient() && music != null){
            for (ServerPlayer serverPlayer : ceBossEvent.getPlayers()) {
                CEMusicNetworkHandler.sendRemoveMusicPacket(serverPlayer, bgmUUID, false);
            }
        }
    }

    @Override
    public void onStartTracking(ServerPlayer serverPlayer) {
        super.onStartTracking(serverPlayer);
        ceBossEvent.addPlayer(serverPlayer);
        if(music != null && shouldPlayBGM){
            CEMusicNetworkHandler.sendRequestPlayPacket(serverPlayer, music);
        }
    }

    @Override
    public void onStopTracking(ServerPlayer serverPlayer) {
        super.onStopTracking(serverPlayer);
        ceBossEvent.removePlayer(serverPlayer);
        if(music != null){
            CEMusicNetworkHandler.sendRemoveMusicPacket(serverPlayer, bgmUUID, false);
        }
    }
}
