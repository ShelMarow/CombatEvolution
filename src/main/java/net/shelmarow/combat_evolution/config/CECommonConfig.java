package net.shelmarow.combat_evolution.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CECommonConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED_EXECUTION;
    public static final ForgeConfigSpec.DoubleValue MASSACRE_ENCHANTMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("common");

        ENABLED_EXECUTION = builder
                .comment("Enable execution")
                .define("enabledExecution", true);

        MASSACRE_ENCHANTMENT = builder
                .comment("The damage multiplier increased by each enchantment level")
                        .defineInRange("massacreEnchantment", 0.16, 0, Double.MAX_VALUE);

        builder.pop();

        COMMON_SPEC = builder.build();
    }

}
