package net.shelmarow.combat_evolution.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEKeyMappings {

    public static final KeyMapping EXECUTION = new KeyMapping(
            "key."+ CombatEvolution.MOD_ID + ".execution",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_2,
            "key.categories." + CombatEvolution.MOD_ID
    );

//    public static final List<KeyMapping> CE_KEYS = List.of(
//            EXECUTION
//    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EXECUTION);
    }
}
