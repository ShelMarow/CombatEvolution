package net.shelmarow.combat_evolution.tickTask;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class TickTaskHandler {

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Pre event) {
        TickTaskManager.tickAll();
    }

}
