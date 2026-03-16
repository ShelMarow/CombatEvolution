package net.shelmarow.combat_evolution.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.enchantment.enchantments.IntimidateEnchantment;
import net.shelmarow.combat_evolution.enchantment.enchantments.MassacreEnchantment;

public class CEEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, CombatEvolution.MOD_ID);

    public static final RegistryObject<Enchantment> MASSACRE =
            ENCHANTMENTS.register("massacre", MassacreEnchantment::new);

    public static final RegistryObject<Enchantment> INTIMIDATE =
            ENCHANTMENTS.register("intimidate", IntimidateEnchantment::new);
}
