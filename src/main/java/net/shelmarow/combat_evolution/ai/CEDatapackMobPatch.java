package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Map;

public class CEDatapackMobPatch extends CEHumanoidPatch{
    private final CEPatchReloadListener.CEDatapackMobPatchProvider provider;

    public CEDatapackMobPatch(CEPatchReloadListener.CEDatapackMobPatchProvider provider) {
        super(provider.faction);
        this.provider = provider;

        this.breakTime = provider.breakTime;
        this.recoverTime = provider.recoverTime;
        this.staminaRegenDelay = provider.staminaRegenDelay;

        this.weaponLivingMotions.putAll(provider.weaponLivingMotions);
        this.guardHitMotions.putAll(provider.guardHitMotions);
        this.weaponAttackMotions.putAll(provider.weaponAttackMotions);
    }

    @Override
    protected void setWeaponMotions() {

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
    public void onJoinWorld(PathfinderMob entity, EntityJoinLevelEvent event) {
        super.onJoinWorld(entity, event);
        putAndSetCustomAttributes();
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
    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> stunAnimations = provider.stunAnimations;
        return stunAnimations.get(stunType);
    }


    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        float scale = provider.scale;
        return super.getModelMatrix(partialTicks).scale(scale, scale, scale);
    }
}
