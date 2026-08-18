package net.shelmarow.combat_evolution.enchantment.enchantments;

public class IntimidateEnchantment {

    public IntimidateEnchantment() {
    }

    public int getMinCost(int level) {
        return 20;
    }

    public int getMaxCost(int level) {
        return 60;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 1;
    }
}
