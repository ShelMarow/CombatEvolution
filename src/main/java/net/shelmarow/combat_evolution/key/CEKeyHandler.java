package net.shelmarow.combat_evolution.key;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CEKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && mc.player != null) {
            if(CEKeyMappings.EXECUTION.consumeClick()) {
                LivingEntity entity = ExecutionHandler.getEntityLookedAt(mc.player, ExecutionHandler.EXECUTION_DISTANCE);
                if(entity != null) {
                    CombatEvolution.CHANNEL.sendToServer(new C2STryExecutionPacket());
                }
            }
        }
    }
}
