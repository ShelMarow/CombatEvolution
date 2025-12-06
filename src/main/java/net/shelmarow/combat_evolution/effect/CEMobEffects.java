package net.shelmarow.combat_evolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;

public class CEMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CombatEvolution.MOD_ID);

    public static final RegistryObject<MobEffect> FULL_STUN_IMMUNITY =
            EFFECTS.register("full_stun_immunity",() -> new CECommonEffect(MobEffectCategory.BENEFICIAL,0xFFFFFF));

}
