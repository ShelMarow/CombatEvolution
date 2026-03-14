package net.shelmarow.combat_evolution.client.hud.execution;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.client.hud.execution.types.HUDType;
import net.shelmarow.combat_evolution.config.CEClientConfig;
import net.shelmarow.combat_evolution.config.screen.HUDConfigScreen;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.key.CEKeyMappings;
import org.joml.Matrix4f;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ExecutionHUD implements IGuiOverlay {
    public static final ExecutionHUD instance = new ExecutionHUD();

    private static HUDType currentHudType;
    private static boolean showExecutionIcon = false;
    private static float timePercent = 1F;
    private static float timePercentO = 1F;


    @SubscribeEvent
    public static void onPlayerClientTick(TickEvent.PlayerTickEvent event) {
        if (!CEClientConfig.ICON_DISPLAY.get()) {
            showExecutionIcon = false;
            return;
        }

        if (event.player.level().isClientSide && event.player == Minecraft.getInstance().player && event.phase == TickEvent.Phase.END) {
            //从配置文件读取当前图标类型
            HUDType hudType = HUDTypeManager.getHUDType(CEClientConfig.HUD_TYPE.get());

            if (hudType != null && (currentHudType == null || !currentHudType.equals(hudType))) {
                currentHudType = hudType;
            }

            if (hudType == null) {
                currentHudType = HUDTypeManager.getHUDType("combat_evolution:default");
            }

            //判断是否需要显示图标
            LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.getEntityPatch(event.player, LocalPlayerPatch.class);
            if (localPlayerPatch != null) {
                LivingEntity target = localPlayerPatch.getTarget();
                LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
                if (targetPatch != null) {
                    AnimationPlayer animationPlayer = targetPatch.getAnimator().getPlayerFor(null);
                    if (animationPlayer != null) {
                        AssetAccessor<? extends StaticAnimation> currentAnimation = animationPlayer.getRealAnimation();
                        //检测可处决的条件
                        if (ExecutionHandler.targetIsInRange(event.player, target, 0, ExecutionHandler.EXECUTION_DISTANCE, 180) &&
                                ExecutionHandler.isTargetGuardBreak(currentAnimation, targetPatch) &&
                                ExecutionHandler.canExecute(event.player, localPlayerPatch, target, targetPatch)) {
                            float totalTime = currentAnimation.get().getTotalTime();
                            float currentTime = animationPlayer.getElapsedTime();
                            timePercentO = timePercent;
                            timePercent = currentTime / totalTime;
                            showExecutionIcon = true;
                            return;
                        }
                    }
                }
            }
            showExecutionIcon = false;
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (currentHudType == null || !showExecutionIcon || Minecraft.getInstance().screen instanceof HUDConfigScreen) {
            return;
        }

        PoseStack poseStack = guiGraphics.pose();

        int renderX = 0;
        int renderY = 0;
        int iconSize = 32;

        HUDAlignment alignment = CEClientConfig.ICON_ALIGNMENT.get();
        int posX = CEClientConfig.ICON_X.get().intValue();
        int posY = CEClientConfig.ICON_Y.get().intValue();

        switch (alignment) {
            case CENTER->{
                renderX = screenWidth / 2 + posX;
                renderY = screenHeight / 2 + posY;
            }
            case CENTER_LEFT -> {
                renderX = posX;
                renderY = screenHeight / 2 + posY;
            }
            case CENTER_RIGHT -> {
                renderX = screenWidth + posX;
                renderY = screenHeight / 2 + posY;
            }
            case TOP -> {
                renderX = screenWidth / 2 + posX;
                renderY = screenHeight + posY;
            }
            case TOP_LEFT -> {
                renderX = posX;
                renderY = screenHeight + posY;
            }
            case TOP_RIGHT-> {
                renderX = screenWidth + posX;
                renderY = screenHeight + posY;
            }
            case BOTTOM -> {
                renderX = screenWidth / 2 + posX;
                renderY = posY;
            }
            case BOTTOM_LEFT -> {
                renderX = posX;
                renderY = posY;
            }
            case BOTTOM_RIGHT -> {
                renderX = screenWidth + posX;
                renderY = posY;
            }
        }

        renderX = Mth.clamp(renderX, 0, screenWidth - iconSize);
        renderY = Mth.clamp(renderY, 0, screenHeight - iconSize);

        if (CEClientConfig.ICON_DISPLAY.get()) {
            poseStack.pushPose();
            poseStack.translate(renderX, renderY, 0);
            poseStack.scale(1, 1, 1);
            drawExecutionIcon(guiGraphics, partialTick, currentHudType, iconSize);
            poseStack.popPose();
        }

        if (CEClientConfig.SHOW_TEXT_DISPLAY.get()) {

            Font font = gui.getFont();

            Component text = Component.translatable("hud.combat_evolution.execution_tooltip", CEKeyMappings.EXECUTION.getTranslatedKeyMessage());

            List<FormattedCharSequence> lines = font.split(text, Integer.MAX_VALUE);

            int textWidth = 0;
            int textHeight = font.lineHeight * lines.size();

            for (FormattedCharSequence line : lines) {
                int width = font.width(line);
                if (width > textWidth) {
                    textWidth = width;
                }
            }

            poseStack.pushPose();
            poseStack.translate(renderX + iconSize / 2F, renderY + iconSize, 0);
            poseStack.scale(0.75F, 0.75F, 0.75F);

            //背景
            guiGraphics.fill(
                    -2 - textWidth / 2,
                    -2,
                    textWidth + 2 - textWidth / 2,
                    textHeight + 2,
                    0XFF000000
            );
            //边框
            guiGraphics.renderOutline(
                    -2 - textWidth / 2,
                    -2,
                    textWidth + 4,
                    textHeight + 4,
                    0XFFFFFFFF
            );
            //文本
            int offsetY = 0;
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(
                        font, line,
                        -textWidth / 2, offsetY,
                        0XFFFFFFFF
                );
                offsetY += font.lineHeight;
            }

            poseStack.popPose();
        }
    }

    public static void drawExecutionIcon(GuiGraphics guiGraphics, float partialTick, HUDType currentHudType, float iconSize) {
        HUDType hudType = currentHudType;

        if (hudType == null) {
            hudType = HUDTypeManager.getHUDType("combat_evolution:default");
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        //绘制背景
        ResourceLocation background = hudType.getBackground();
        if (background != null) {
            RenderSystem.setShaderTexture(0, background);
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            buf.vertex(matrix, 0, 0, 0).uv(0, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, iconSize, 0, 0).uv(1, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, iconSize, 0 + iconSize, 0).uv(1, 1).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, 0, 0 + iconSize, 0).uv(0, 1).color(1, 1, 1, 1).endVertex();

            BufferUploader.drawWithShader(buf.end());
        }

        // 绘制进度外框
        float filled = 1.0F - Mth.lerp(partialTick, timePercentO, timePercent);
        ResourceLocation progress = hudType.getProgress();
        if (progress != null) {
            RenderSystem.setShaderTexture(0, progress);
            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);

            float cx = iconSize / 2f;
            float cy = iconSize / 2f;
            buf.vertex(matrix, cx, cy, 0).uv(0.5F, 0.5F).endVertex();
            float filledAngle = 360.0F * filled;


            int segments = Math.max(3, (int) (filledAngle / 3.0));
            double startRad = Math.toRadians(90);
            double step = Math.toRadians(filledAngle / segments);

            float half = iconSize / 2f;

            for (int i = 0; i <= segments; i++) {
                double angle = startRad - step * i;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float scale = half / Math.max(Math.abs(cos), Math.abs(sin));

                float px = cx + cos * scale;
                float py = cy - sin * scale;

                float u = px / iconSize;
                float v = py / iconSize;

                buf.vertex(matrix, px, py, 0).uv(u, v).endVertex();
            }

            BufferUploader.drawWithShader(buf.end());
        }

        //绘制遮罩
        List<ResourceLocation> overlays = hudType.getOverlays();
        if (!overlays.isEmpty()) {
            int index = Mth.clamp((int) (overlays.size() * filled), 0, overlays.size() - 1);
            RenderSystem.setShaderTexture(0, overlays.get(index));

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            buf.vertex(matrix, 0, 0, 0).uv(0, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, iconSize, 0, 0).uv(1, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, iconSize, iconSize, 0).uv(1, 1).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, 0, iconSize, 0).uv(0, 1).color(1, 1, 1, 1).endVertex();

            BufferUploader.drawWithShader(buf.end());


            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}
