package net.shelmarow.combat_evolution.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

@OnlyIn(Dist.CLIENT)
public class CEClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue PLAY_CE_MUSIC;
    public static final ForgeConfigSpec.ConfigValue<String> HUD_TYPE;
    public static final ForgeConfigSpec.BooleanValue ICON_DISPLAY;
    public static final ForgeConfigSpec.BooleanValue SHOW_TEXT_DISPLAY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("music");

        PLAY_CE_MUSIC = builder
                .comment("Should CE Music Play (Such as Boss BGM)")
                .define("playCEMusic", true);

        builder.pop();

        builder.push("hud");

        HUD_TYPE = builder
                .comment("The execution HUD type used for rendering, e.g. 'combat_evolution:default'")
                .define("hudType", "combat_evolution:default");

        ICON_DISPLAY = builder
                .comment("Whether to show the icon in the execution HUD")
                .define("iconDisplay", true);

        SHOW_TEXT_DISPLAY = builder
                .comment("Whether to show text display in the execution HUD")
                .define("showTextDisplay", true);

        builder.pop();

        CLIENT_SPEC = builder.build();
    }
}
