package net.shelmarow.combat_evolution.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.shelmarow.combat_evolution.CombatEvolution;

public class CEEnchantments {
    public static final ResourceKey<Enchantment> MASSACRE = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "massacre"));

    public static final ResourceKey<Enchantment> INTIMIDATE = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "intimidate"));
}
