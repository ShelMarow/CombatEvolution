package net.shelmarow.combat_evolution.ai;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.world.damagesource.StunType;

import java.util.HashSet;
import java.util.Set;

public class AnimationParams {
    private float transitionTime = 0f;
    private boolean shouldChangeSpeed = false;
    private float attackSpeed = 1f;
    private float damageMultiplier = 1f;
    private float impactMultiplier = 1f;
    private float armorNegationMultiplier = 1f;
    private int stunType = -1;
    private final Set<TagKey<DamageType>> damageSource = new HashSet<>();

    public AnimationParams transitionTime(float t) {
        this.transitionTime = t;
        return this;
    }

    public void setShouldChangeSpeed(boolean shouldChangeSpeed) {
        this.shouldChangeSpeed = shouldChangeSpeed;
    }

    public boolean shouldChangeSpeed() {
        return shouldChangeSpeed;
    }

    public AnimationParams playSpeed(float attackSpeed) {
        this.attackSpeed = attackSpeed;
        setShouldChangeSpeed(true);
        return this;
    }

    public AnimationParams damageMultiplier(float multiplier) {
        this.damageMultiplier = multiplier;
        return this;
    }

    public AnimationParams impactMultiplier(float multiplier) {
        this.impactMultiplier = multiplier;
        return this;
    }

    public AnimationParams armorNegationMultiplier(float multiplier) {
        armorNegationMultiplier = multiplier;
        return this;
    }

    public AnimationParams stunType(StunType stunType) {
        this.stunType = stunType.ordinal();
        return this;
    }

    public AnimationParams damageSource(Set<TagKey<DamageType>> damageSource) {
        this.damageSource.addAll(damageSource);
        return this;
    }

    public float getTransitionTime() {
        return transitionTime;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public float getImpactMultiplier() {
        return impactMultiplier;
    }

    public float getArmorNegationMultiplier() {
        return armorNegationMultiplier;
    }

    public int getStunType() {
        return stunType;
    }

    public Set<TagKey<DamageType>> getDamageSource() {
        return damageSource;
    }
}
