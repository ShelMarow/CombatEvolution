package net.shelmarow.combat_evolution.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.registry.entries.EpicFightAttributes;

public class CEMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CombatEvolution.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FULL_STUN_IMMUNITY =
            EFFECTS.register("full_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> HIGH_STUN_IMMUNITY =
            EFFECTS.register("high_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> MIDDLE_STUN_IMMUNITY =
            EFFECTS.register("middle_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> NORMAL_STUN_IMMUNITY =
            EFFECTS.register("normal_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> BYPASS_DODGE_EFFECT =
            EFFECTS.register("bypass_dodge_effect",() -> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> BYPASS_GUARD_EFFECT =
            EFFECTS.register("bypass_guard_effect",() -> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final DeferredHolder<MobEffect, MobEffect> IMPACT_BOOST =
            EFFECTS.register("impact_boost",()-> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF)
                    .addAttributeModifier(EpicFightAttributes.IMPACT, ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "impact_boost"), 0.5, AttributeModifier.Operation.ADD_VALUE)
                    .addAttributeModifier(EpicFightAttributes.OFFHAND_IMPACT, ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "offhand_impact_boost"), 0.5, AttributeModifier.Operation.ADD_VALUE)
            );

}
