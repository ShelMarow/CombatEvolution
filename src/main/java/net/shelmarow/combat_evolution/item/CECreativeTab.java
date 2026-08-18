package net.shelmarow.combat_evolution.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.enchantment.CEEnchantments;

public class CECreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CombatEvolution.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CE_TAB = CREATIVE_TAB.register("combat_evolution_items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.combat_evolution.items"))
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .displayItems((params, output) -> {

                        ItemStack massacreBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(params.holders().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(CEEnchantments.MASSACRE), 5));
                        output.accept(massacreBook);

                        ItemStack intimidateBook = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(params.holders().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(CEEnchantments.INTIMIDATE), 1));
                        output.accept(intimidateBook);

                    }).build());
}
