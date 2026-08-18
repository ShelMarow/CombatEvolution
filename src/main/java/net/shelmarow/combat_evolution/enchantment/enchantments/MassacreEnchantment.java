package net.shelmarow.combat_evolution.enchantment.enchantments;

public class MassacreEnchantment {

    public MassacreEnchantment() {
    }

    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    public int getMaxCost(int level) {
        return getMinCost(level) + 30;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return 5;
    }
}
