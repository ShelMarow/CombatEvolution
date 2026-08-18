package net.shelmarow.combat_evolution.bgm;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class CEMusicEventHandler {

    @SubscribeEvent
    public static void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event) {
        CEMusicManager.clearAllMusic();
    }

    @SubscribeEvent
    public static void clientTickEvent(PlayerTickEvent.Pre event) {
        if(event.getEntity().level().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
            CEMusicManager.playTick();
        }
    }
}
