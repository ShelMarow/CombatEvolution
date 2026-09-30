package net.shelmarow.combat_evolution.bossbar.client.types;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.shelmarow.combat_evolution.bossbar.BossData;
import net.shelmarow.combat_evolution.bossbar.client.ClientBossData;
import net.shelmarow.combat_evolution.utils.RLUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class CESoulLikeType extends AbstractBossBarType {

    private static final int BAR_WIDTH = 256;
    private static final int BAR_FRAME_WIDTH = 2;
    private static final int BAR_INNER_WIDTH = BAR_WIDTH - BAR_FRAME_WIDTH * 2;

    private static final int DAMAGE_TEXT = 0xFFFFFFFF;
    private static final int TEXT_GAP = 6;

    protected static long damageDisplayMillis = 2000L;
    protected static long damageBarDelayMillis = 700L;
    protected static long damageBarSmoothMillis = 300L;
    protected static float damageBarMinSpeed = 0.08F;

    private static final Map<UUID, BossBarState> BOSS_BAR_STATES = new HashMap<>();


    public CESoulLikeType() {
        super("[CE:SoulLikeType]", RLUtils.getRL("textures/gui/bossbar/soul_like_bossbar.png"));
    }

    @Override
    public int getPreviewWidth() {
        return 512;
    }

    @Override
    public int getPreviewHeight() {
        return 100;
    }

    @Override
    public void renderPreview(GuiGraphics graphics, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(defaultTexture, 0, 0, 0, 0, 512, 100, 512, 512);
        graphics.blit(defaultTexture, 136, 31, 136, 233, 174, 6, 512, 512);
        graphics.blit(defaultTexture, 136, 31, 136, 132, 112, 6, 512, 512);
        graphics.pose().popPose();
    }

    @Override
    public void render(CustomizeGuiOverlayEvent.BossEventProgress event, BossData bossData) {
        LerpingBossEvent bossEvent = event.getBossEvent();
        UUID bossEventId = bossEvent.getId();

        Font font = Minecraft.getInstance().font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int centerX = event.getX() + 91;
        int barLeft = centerX - BAR_WIDTH / 2;
        int barTop = event.getY() + 2;
        float progress = Mth.clamp(bossEvent.getProgress(), 0.0F, 1.0F);

        float maxHealth = 1.0F;
        float actualProgress = progress;
        boolean hasActualHealth = false;
        CompoundTag customData = bossData.customData;
        if (customData != null && customData.contains("health") && customData.contains("max_health")) {
            maxHealth = Math.max(customData.getFloat("max_health"), 1.0F);
            actualProgress = Mth.clamp(customData.getFloat("health") / maxHealth, 0.0F, 1.0F);
            hasActualHealth = true;
        }

        long now = Util.getMillis();
        BossBarState state = BOSS_BAR_STATES.computeIfAbsent(bossEventId, id -> new BossBarState());
        state.update(progress, actualProgress, maxHealth, hasActualHealth, now);
        BOSS_BAR_STATES.entrySet().removeIf(entry -> !ClientBossData.hasBossData(entry.getKey()));

        int healthWidth = Math.round(BAR_INNER_WIDTH * progress);
        int damageWidth = Math.round(BAR_INNER_WIDTH * state.getDamageProgress());

        String damageText = state.getDamageText(now);
        int textLeft = barLeft + BAR_FRAME_WIDTH + 2;
        int textRight = barLeft + BAR_WIDTH - BAR_FRAME_WIDTH - 2;
        int damageLeft = textRight - font.width(damageText);
        int maxNameWidth = damageText.isEmpty() ? textRight - textLeft : Math.max(0, damageLeft - textLeft - TEXT_GAP);
        String nameText = font.plainSubstrByWidth(bossEvent.getName().getString(), maxNameWidth);

        renderBar(guiGraphics, bossData.bossBarTexture, barLeft, barTop, healthWidth, damageWidth);
        renderBarText(guiGraphics, font, nameText, damageText, textLeft, damageLeft, barTop);

        event.setIncrement(22 + font.lineHeight);
    }

    private static void renderBar(GuiGraphics guiGraphics, @NonNull ResourceLocation bossBarTexture, int barLeft, int barTop, int healthWidth, int damageWidth) {
        int interiorLeft = barLeft + BAR_FRAME_WIDTH;
        int interiorTop = barTop + 1;

        RenderSystem.enableBlend();
        guiGraphics.blit(bossBarTexture, barLeft - 134, barTop - 30, 0,0, 512, 100, 512,512);
        guiGraphics.blit(bossBarTexture, interiorLeft, interiorTop, 136, 233, damageWidth, 6, 512,512);
        guiGraphics.blit(bossBarTexture, interiorLeft, interiorTop, 136, 132, healthWidth, 6, 512,512);
    }


    private static void renderBarText(GuiGraphics guiGraphics, Font font, String nameText, String damageText, int textLeft, int damageLeft, int barTop) {
        int titleY = barTop - font.lineHeight - 1;
        guiGraphics.drawString(font, nameText, textLeft, titleY, 0xFFFFFF, true);
        if (!damageText.isEmpty()) {
            guiGraphics.drawString(font, damageText, damageLeft, titleY, DAMAGE_TEXT, true);
        }
    }

    private static final class BossBarState {
        private float lastProgress;
        private float damageProgress;
        private float damageAmount;
        private long damageBarUntil;
        private long damageTextUntil;
        private long lastUpdate;
        private boolean initialized;
        private boolean hasActualProgress;
        private boolean fallbackDamageInProgress;
        private boolean damageBarActive;

        private void update(float progress, float actualProgress, float maxHealth, boolean hasActualHealth, long now) {
            if (!initialized) {
                lastProgress = progress;
                lastActualProgress = actualProgress;
                damageProgress = progress;
                lastUpdate = now;
                initialized = true;
                hasActualProgress = hasActualHealth;
                return;
            }

            if (damageAmount > 0.0F && now > damageTextUntil) {
                damageAmount = 0.0F;
            }

            boolean tookDamage;
            float damage = 0.0F;
            if (hasActualHealth && hasActualProgress) {
                tookDamage = actualProgress < lastActualProgress - 0.0001F;
                if (tookDamage) {
                    damage = (lastActualProgress - actualProgress) * maxHealth;
                }
            }
            else {
                boolean movingDown = progress < lastProgress - 0.0001F;
                tookDamage = movingDown && !fallbackDamageInProgress;
                fallbackDamageInProgress = movingDown;
                if (tookDamage) {
                    damage = (lastProgress - progress) * maxHealth;
                }
            }

            if (tookDamage && damage > 0.0F) {
                damageAmount += damage;
                damageTextUntil = now + damageDisplayMillis;
                damageProgress = Math.max(damageProgress, lastProgress);

                // Only a hit during the waiting phase restarts the waiting timer.
                if (!damageBarActive || now <= damageBarUntil) {
                    damageBarUntil = now + damageBarDelayMillis;
                }
                damageBarActive = true;
            }

            if (damageBarActive && now > damageBarUntil) {
                float elapsedSeconds = Math.max(0.0F, now - lastUpdate) / 1000.0F;
                float distance = progress - damageProgress;
                float distanceAbs = Math.abs(distance);
                float smoothing = Mth.clamp((now - lastUpdate) / (float) damageBarSmoothMillis, 0.0F, 1.0F);
                float smoothMovement = distanceAbs * smoothing;
                float minimumMovement = damageBarMinSpeed * elapsedSeconds;
                float movement = Math.min(distanceAbs, Math.max(smoothMovement, minimumMovement));
                damageProgress += Math.copySign(movement, distance);
                if (Math.abs(damageProgress - progress) <= 0.001F) {
                    damageProgress = progress;
                    damageBarActive = false;
                }
            }

            lastProgress = progress;
            lastActualProgress = actualProgress;
            hasActualProgress = hasActualHealth;
            lastUpdate = now;
        }

        private float lastActualProgress;

        private float getDamageProgress() {
            return Mth.clamp(damageProgress, 0.0F, 1.0F);
        }

        private String getDamageText(long now) {
            if (damageAmount <= 0.01F || now > damageTextUntil) {
                return "";
            }
            return "-" + Math.max(1, Math.round(damageAmount));
        }
    }
}
