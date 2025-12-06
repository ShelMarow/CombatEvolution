package net.shelmarow.combat_evolution.iml;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Set;

public interface ILivingEntityData {

    int combat_evolution$getPhase(LivingEntity entity);
    void combat_evolution$setPhase(LivingEntity entity, int Phase);

    boolean combat_evolution$getCanModifySpeed(LivingEntity entity);
    void combat_evolution$setCanModifySpeed(LivingEntity entity, boolean canModifySpeed);

    float combat_evolution$getAttackSpeed(LivingEntity entity);
    void combat_evolution$setAttackSpeed(LivingEntity entity, float attackSpeed);

    float combat_evolution$getDamageMultiplier(LivingEntity entity);
    void combat_evolution$setDamageMultiplier(LivingEntity entity, float multiplier);

    float combat_evolution$getImpactMultiplier(LivingEntity entity);
    void combat_evolution$setImpactMultiplier(LivingEntity entity, float multiplier);

    float combat_evolution$getArmorNegationMultiplier(LivingEntity entity);
    void combat_evolution$setArmorNegationMultiplier(LivingEntity entity, float multiplier);

    int combat_evolution$getStunType(LivingEntity entity);
    void combat_evolution$setStunType(LivingEntity entity, StunType stunType);
    void combat_evolution$setStunType(LivingEntity entity, int stunType);

//    Set<TagKey<DamageType>> combat_evolution$getDamageSource();
//    void combat_evolution$setDamageSource(Set<TagKey<DamageType>> damageSource);

    float combat_evolution$getStamina(LivingEntity entity);
    void combat_evolution$setStamina(LivingEntity entity, float stamina);

    boolean combat_evolution$isGuard(LivingEntity entity);
    void combat_evolution$setGuard(LivingEntity entity, boolean guard);

    boolean combat_evolution$isInCounter(LivingEntity entity);
    void combat_evolution$setInCounter(LivingEntity entity, boolean counter);

    boolean combat_evolution$isWander(LivingEntity entity);
    void combat_evolution$setWander(LivingEntity entity, boolean wander);

    StaminaStatus combat_evolution$getStaminaStatus(LivingEntity entity);
    void combat_evolution$setStaminaStatus(LivingEntity entity, StaminaStatus staminaStatus);
}
