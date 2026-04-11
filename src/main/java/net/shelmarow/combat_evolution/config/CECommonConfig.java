package net.shelmarow.combat_evolution.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class CECommonConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED_EXECUTION;
    public static final ForgeConfigSpec.DoubleValue EXECUTION_DAMAGE_TO_PLAYER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAMAGE_SOURCE_TO_PLAYER;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXECUTION_ITEM_BLACKLIST;

    public static final ForgeConfigSpec.DoubleValue MASSACRE_ENCHANTMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("common");

        ENABLED_EXECUTION = builder
                .comment("Enable execution")
                .define("enabledExecution", true);

        EXECUTION_DAMAGE_TO_PLAYER = builder
                .comment("Execution damage multiplier to player")
                .defineInRange("executionDamageToPlayer", 1.0, 0.0, Double.MAX_VALUE);

        ENABLE_DAMAGE_SOURCE_TO_PLAYER = builder
                .comment("Enable EX damage source to player(e.g bypass armor, bypass effect)")
                .define("enableDamageSourceToPlayer", true);

        EXECUTION_ITEM_BLACKLIST = builder
                .comment("Execution item blacklist - items in this list will be disabled",
                        "Format: modid:itemname",
                        "Example: 'minecraft:netherite_sword', 'minecraft:diamond_axe'")
                .defineList("executionItemBlacklist", new ArrayList<>(),k-> k instanceof String);

        MASSACRE_ENCHANTMENT = builder
                .comment("The damage multiplier increased by each enchantment level")
                        .defineInRange("massacreEnchantment", 0.16, 0, Double.MAX_VALUE);

        builder.pop();

        COMMON_SPEC = builder.build();
    }

}
