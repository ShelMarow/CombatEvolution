package net.shelmarow.combat_evolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.registry.entries.EpicFightMobEffects;

public class CEStunImmunityEffect extends MobEffect {

    protected CEStunImmunityEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity pLivingEntity, int pAmplifier) {
        pLivingEntity.addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY,2, 256, false, false, false));
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }
}
