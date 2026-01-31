package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.StaminaStatus;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RenderCEBossBarEvent {

    @SubscribeEvent
    public static void onRenderCustomBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bossEvent = event.getBossEvent();
        UUID bossEventId = bossEvent.getId();

        if(ClientBossData.hasBossData(bossEventId)){
            BossData bossData = ClientBossData.getBossData(bossEventId);
            if (bossData.displayType.equals("[CE:DefaultType]")) {
                event.setCanceled(true);

                ResourceLocation bossBar = bossData.bossBarTexture;

                Font font = Minecraft.getInstance().font;
                GuiGraphics guiGraphics = event.getGuiGraphics();

                int x = event.getX();
                int y = event.getY();
                Component displayName = bossEvent.getName();
                float progress = Mth.clamp(bossEvent.getProgress(), 0, 1);
                float stamina = Mth.clamp(ClientBossData.getStaminaProgress(bossEventId), 0, 1);

                //绘制背景
                //血条 3 + 250 + 3
                guiGraphics.blit(bossBar, x + 91 - 256 / 2, y, 0, 0, 256, 20, 256, 256);
                //耐力 6 + 244 + 6
                guiGraphics.blit(bossBar, x + 91 - 256 / 2, y, 0, 42, 256, 20, 256, 256);

                //绘制血量进度
                guiGraphics.blit(bossBar, x + 91 - 256 / 2 + 6, y, 6, 21, Math.round(244 * progress), 20, 256, 256);
                //绘制耐力进度
                if (bossData.staminaStatus != StaminaStatus.BREAK) {
                    guiGraphics.blit(bossBar, x + 91 - 256 / 2 + 9, y, 9, 63, Math.round(238 * stamina), 20, 256, 256);
                } else {
                    //破防红条
                    guiGraphics.blit(bossBar, x + 91 - 256 / 2, y, 0, 84, 256, 20, 256, 256);
                }

                //绘制名称
                guiGraphics.drawString(font, displayName, x + 91 - font.width(displayName) / 2, y - font.lineHeight + 2, 0xFFFFFF, true);

                event.setIncrement(20 + font.lineHeight);
            }
        }
    }
}
