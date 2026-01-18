package net.shelmarow.combat_evolution.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CECommonConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED_EXECUTION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("common");

        ENABLED_EXECUTION = builder
                .comment("Enable execution")
                .define("enabledExecution", true);

        builder.pop();

        COMMON_SPEC = builder.build();
    }

}
