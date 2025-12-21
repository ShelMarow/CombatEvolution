package net.shelmarow.combat_evolution.ai.params;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.world.damagesource.StunType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnimationParams {
    private float transitionTime = 0f;
    private boolean shouldChangeSpeed = false;
    private float attackSpeed = 1f;
    private final Map<Integer,PhaseParams> phaseParams = new HashMap<>();

    public AnimationParams transitionTime(float t) {
        this.transitionTime = t;
        return this;
    }

    public boolean shouldChangeSpeed() {
        return shouldChangeSpeed;
    }

    public AnimationParams playSpeed(float attackSpeed) {
        this.attackSpeed = attackSpeed;
        this.shouldChangeSpeed = true;
        return this;
    }

    //新版Phase动画参数设置
    public AnimationParams addPhase(int phase, PhaseParams params) {
        this.phaseParams.put(phase, params);
        return this;
    }

    /*--------------------保留旧版功能调用------------------------*/

    private PhaseParams getOrCreateDefaultPhase() {
        return phaseParams.computeIfAbsent(-1, k -> new PhaseParams());
    }

    public AnimationParams damageMultiplier(float multiplier) {
        getOrCreateDefaultPhase().damageMultiplier(multiplier);
        return this;
    }

    public AnimationParams impactMultiplier(float multiplier) {
        getOrCreateDefaultPhase().impactMultiplier(multiplier);
        return this;
    }

    public AnimationParams armorNegationMultiplier(float multiplier) {
        getOrCreateDefaultPhase().armorNegationMultiplier(multiplier);
        return this;
    }

    public AnimationParams stunType(StunType stunType) {
        getOrCreateDefaultPhase().stunType(stunType);
        return this;
    }

    public AnimationParams damageSource(Set<TagKey<DamageType>> damageSource) {
        getOrCreateDefaultPhase().damageSource(damageSource);
        return this;
    }

    /*---------------------------------------------------------------------------*/

    //Getter
    public float getTransitionTime() {
        return transitionTime;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public Map<Integer,PhaseParams> getPhaseParams() {
        return phaseParams;
    }
}
