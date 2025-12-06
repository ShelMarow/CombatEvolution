package net.shelmarow.combat_evolution.tickTask;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TickTaskHandler {

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            TickTaskManager.tickAll();
        }
    }

}