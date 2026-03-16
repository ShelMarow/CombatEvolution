package net.shelmarow.combat_evolution.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.enchantment.CEEnchantments;

public class CECreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CombatEvolution.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CE_TAB = CREATIVE_TAB.register("combat_evolution_items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.combat_evolution.items"))
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .displayItems((params, output) -> {

                        ItemStack massacreBook = new ItemStack(Items.ENCHANTED_BOOK);
                        EnchantedBookItem.addEnchantment(massacreBook, new EnchantmentInstance(CEEnchantments.MASSACRE.get(), 5));
                        output.accept(massacreBook);

                        ItemStack intimidateBook = new ItemStack(Items.ENCHANTED_BOOK);
                        EnchantedBookItem.addEnchantment(intimidateBook, new EnchantmentInstance(CEEnchantments.INTIMIDATE.get(), 1));
                        output.accept(intimidateBook);

                    }).build());
}
