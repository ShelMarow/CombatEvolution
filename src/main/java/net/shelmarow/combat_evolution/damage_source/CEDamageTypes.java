package net.shelmarow.combat_evolution.damage_source;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.shelmarow.combat_evolution.CombatEvolution;

public class CEDamageTypes {

    public static final ResourceKey<DamageType> EXECUTION =
            ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "execution"));

}
