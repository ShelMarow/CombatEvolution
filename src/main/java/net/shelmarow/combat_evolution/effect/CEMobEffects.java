package net.shelmarow.combat_evolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public class CEMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CombatEvolution.MOD_ID);

    public static final RegistryObject<MobEffect> FULL_STUN_IMMUNITY =
            EFFECTS.register("full_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final RegistryObject<MobEffect> HIGH_STUN_IMMUNITY =
            EFFECTS.register("high_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final RegistryObject<MobEffect> MIDDLE_STUN_IMMUNITY =
            EFFECTS.register("middle_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

//    public static final RegistryObject<MobEffect> NORMAL_STUN_IMMUNITY =
//            EFFECTS.register("normal_stun_immunity",() -> new CEStunImmunityEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final RegistryObject<MobEffect> BYPASS_DODGE_EFFECT =
            EFFECTS.register("bypass_dodge_effect",() -> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final RegistryObject<MobEffect> BYPASS_GUARD_EFFECT =
            EFFECTS.register("bypass_guard_effect",() -> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

    public static final RegistryObject<MobEffect> IMPACT_BOOST =
            EFFECTS.register("impact_boost",()-> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF)
                    .addAttributeModifier(EpicFightAttributes.IMPACT.get(), "96bd5bc6-37b0-46b5-a0ce-b7a914d43c2a", 0.5, AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(EpicFightAttributes.OFFHAND_IMPACT.get(), "96bd5bc6-37b0-46b5-a0ce-b7a914d43c2b", 0.5, AttributeModifier.Operation.ADDITION)
            );

}
