package net.shelmarow.combat_evolution.ai.params;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.world.damagesource.StunType;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public class PhaseParams {
    protected float damageMultiplier = 1f;
    protected float impactMultiplier = 1f;
    protected float armorNegationMultiplier = 1f;
    protected int stunType = -1;
    protected final Set<TagKey<DamageType>> damageSource = new HashSet<>();

    public PhaseParams damageMultiplier(float damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
        return this;
    }

    public PhaseParams impactMultiplier(float impactMultiplier) {
        this.impactMultiplier = impactMultiplier;
        return this;
    }

    public PhaseParams damageSource(Set<TagKey<DamageType>> tag) {
        this.damageSource.addAll(tag);
        return this;
    }

    public PhaseParams armorNegationMultiplier(float armorNegationMultiplier) {
        this.armorNegationMultiplier = armorNegationMultiplier;
        return this;
    }

    public PhaseParams stunType(StunType stunType) {
        this.stunType = stunType.ordinal();
        return this;
    }

    public Set<TagKey<DamageType>> getDamageSource() {
        return damageSource;
    }

    public int getStunType() {
        return stunType;
    }

    public float getArmorNegationMultiplier() {
        return armorNegationMultiplier;
    }

    public float getImpactMultiplier() {
        return impactMultiplier;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }
}
