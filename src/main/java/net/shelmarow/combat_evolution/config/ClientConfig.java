package net.shelmarow.combat_evolution.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

@OnlyIn(Dist.CLIENT)
public class ClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> HUD_TYPE;
    public static final ForgeConfigSpec.BooleanValue SHOW_TEXT_DISPLAY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("hud");

        HUD_TYPE = builder
                .comment("The execution HUD type used for rendering, e.g. 'combat_evolution:default'")
                .define("hudType", "combat_evolution:default");

        SHOW_TEXT_DISPLAY = builder
                .comment("Whether to show text display in the execution HUD")
                .define("showTextDisplay", true);

        builder.pop();

        CLIENT_SPEC = builder.build();
    }
}
