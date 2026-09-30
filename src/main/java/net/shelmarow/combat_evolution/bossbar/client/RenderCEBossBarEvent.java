package net.shelmarow.combat_evolution.bossbar.client;

import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.BossData;
import net.shelmarow.combat_evolution.bossbar.client.types.AbstractBossBarType;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RenderCEBossBarEvent {

    @SubscribeEvent
    public static void onRenderCustomBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bossEvent = event.getBossEvent();
        UUID bossEventId = bossEvent.getId();

        if(ClientBossData.hasBossData(bossEventId)){
            BossData bossData = ClientBossData.getBossData(bossEventId);
            List<AbstractBossBarType> barTypes = BossBarTypeManager.getInstance().getBossBarTypes();
            for(AbstractBossBarType barType : barTypes){
                if(barType.getName().equals(bossData.displayType)){
                    event.setCanceled(true);
                    barType.render(event, bossData);
                }
            }
        }
    }
}
