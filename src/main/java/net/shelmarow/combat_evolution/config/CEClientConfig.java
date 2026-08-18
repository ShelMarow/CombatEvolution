package net.shelmarow.combat_evolution.config;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.shelmarow.combat_evolution.client.hud.execution.HUDAlignment;

@OnlyIn(Dist.CLIENT)
public class CEClientConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue PLAY_CE_MUSIC;
    public static final ModConfigSpec.ConfigValue<String> HUD_TYPE;
    public static final ModConfigSpec.BooleanValue ICON_DISPLAY;
    public static final ModConfigSpec.BooleanValue SHOW_TEXT_DISPLAY;

    public static final ModConfigSpec.EnumValue<HUDAlignment> ICON_ALIGNMENT;
    public static final ModConfigSpec.DoubleValue ICON_X;
    public static final ModConfigSpec.DoubleValue ICON_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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

        ICON_ALIGNMENT = builder
                .comment("Icon Alignment")
                .defineEnum("iconAlignment", HUDAlignment.CENTER);

        ICON_X = builder
                .comment("Position X of Execution Icon")
                        .defineInRange("iconX", 30D, -Double.MAX_VALUE, Double.MAX_VALUE);

        ICON_Y = builder
                .comment("Position Y of Execution Icon")
                .defineInRange("iconY", 30D, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.pop();

        CLIENT_SPEC = builder.build();
    }
}
