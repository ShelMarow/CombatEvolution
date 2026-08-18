package net.shelmarow.combat_evolution.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CEKeyMappings {

    public static final KeyMapping EXECUTION = new KeyMapping(
            "key."+ CombatEvolution.MOD_ID + ".execution",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_2,
            "key.categories." + CombatEvolution.MOD_ID
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(EXECUTION);
    }
}
