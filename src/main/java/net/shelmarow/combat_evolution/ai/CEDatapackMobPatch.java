package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
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
import net.shelmarow.combat_evolution.ai.iml.CustomExecuteEntity;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.bgm.network.CEMusicNetworkHandler;
import net.shelmarow.combat_evolution.bgm.network.CEMusicPacket;
import net.shelmarow.combat_evolution.bossbar.CEBossEvent;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class CEDatapackMobPatch extends CEHumanoidPatch<Mob> implements CustomExecuteEntity {
    private final CEPatchReloadListener.CEDatapackMobPatchProvider provider;
    protected final CEBossEvent ceBossEvent = new CEBossEvent(Component.empty());
    private boolean shouldPlayBGM = false;
    private final UUID bgmUUID = UUID.randomUUID();
    private CEMusicPacket music;
    private float bossBarHealth = Float.NaN;
    private float bossBarMaxHealth = Float.NaN;

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
            music = new CEMusicPacket(
                    true, bgmUUID,
                    provider.bgm, SoundSource.RECORDS,
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
        ceBossEvent.setDisplayType(provider.bossBarType);
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

    @Override
    public boolean shouldDisplayHealthBar(){
        return !ceBossEvent.isVisible();
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
                updateBossBarHealth();
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

    private void updateBossBarHealth() {
        float maxHealth = Math.max(original.getMaxHealth(), 1.0F);
        float health = Math.max(original.getHealth(), 0.0F);
        ceBossEvent.setProgress(Mth.clamp(health / maxHealth, 0.0F, 1.0F));
        if (Float.compare(bossBarHealth, health) != 0 || Float.compare(bossBarMaxHealth, maxHealth) != 0) {
            CompoundTag customData = ceBossEvent.getCustomData();
            customData.putFloat("health", health);
            customData.putFloat("max_health", maxHealth);
            ceBossEvent.updateCustomData(customData);
            bossBarHealth = health;
            bossBarMaxHealth = maxHealth;
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
        if (ceBossEvent.isVisible()) updateBossBarHealth();
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

    @Override
    public boolean canBeExecuted(LivingEntityPatch<?> executorPatch) {
        return true;
    }

    @Override
    public boolean canUseCustomType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType) {
        return false;
    }

    @Override
    public ExecutionTypeManager.Type getExecutionType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType) {
        return originalType;
    }

    @Override
    public boolean canBeAssassinate(LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch) {
        if(provider.canBeAssassinate != null){
            boolean b = provider.canBeAssassinate;
            return b && CustomExecuteEntity.super.canBeAssassinate(executorPatch, targetPatch);
        }
        return CustomExecuteEntity.super.canBeAssassinate(executorPatch, targetPatch);
    }
}
