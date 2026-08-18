package net.shelmarow.combat_evolution.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class CECommonConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED_EXECUTION;
    public static final ModConfigSpec.DoubleValue EXECUTION_DAMAGE_TO_PLAYER;
    public static final ModConfigSpec.BooleanValue ENABLE_DAMAGE_SOURCE_TO_PLAYER;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXECUTION_ITEM_BLACKLIST;

    public static final ModConfigSpec.DoubleValue MASSACRE_ENCHANTMENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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
