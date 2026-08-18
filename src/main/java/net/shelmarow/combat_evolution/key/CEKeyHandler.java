package net.shelmarow.combat_evolution.key;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;
import net.shelmarow.combat_evolution.network.CENetworkHandler;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class CEKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if(CEKeyMappings.EXECUTION.consumeClick()) {
                LivingEntity entity = ExecutionHandler.getEntityLookedAt(mc.player, ExecutionHandler.EXECUTION_DISTANCE);
                if(entity != null) {
                    CENetworkHandler.sendToServer(new C2STryExecutionPacket());
                }
            }
        }
    }
}
