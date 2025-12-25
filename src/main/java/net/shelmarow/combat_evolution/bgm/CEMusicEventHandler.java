package net.shelmarow.combat_evolution.bgm;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CEMusicEventHandler {

    @SubscribeEvent
    public static void onLeaveWorld(ClientPlayerNetworkEvent.LoggingOut event) {
        CEMusicManager.clearAllMusic();
    }

    @SubscribeEvent
    public static void clientTickEvent(TickEvent.PlayerTickEvent event) {
        if(event.player.level().isClientSide() && event.player == Minecraft.getInstance().player) {
            if (event.phase == TickEvent.Phase.START) {
                CEMusicManager.playTick();
            }
        }
    }
}
