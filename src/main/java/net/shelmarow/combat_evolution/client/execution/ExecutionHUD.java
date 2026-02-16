package net.shelmarow.combat_evolution.client.execution;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.client.execution.types.HUDType;
import net.shelmarow.combat_evolution.config.CEClientConfig;
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

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,value = Dist.CLIENT)
public class ExecutionHUD {

    private static final Minecraft mc = Minecraft.getInstance();
    private static HUDType currentHudType;

    private static boolean showExecutionIcon = false;
    private static float timePercent = 1F;

    @SubscribeEvent
    public static void onPlayerClientTick(TickEvent.PlayerTickEvent event) {
        if(!CEClientConfig.ICON_DISPLAY.get()){
            return;
        }
        if(event.player.level().isClientSide && event.player == mc.player && event.phase == TickEvent.Phase.END){
            //从配置文件读取当前图标类型
            HUDType hudType = HUDTypeManager.getHUDType(CEClientConfig.HUD_TYPE.get());

            if(hudType != null && (currentHudType == null || !currentHudType.equals(hudType))){
                currentHudType = hudType;
            }
            if(hudType == null){
                currentHudType = HUDTypeManager.getHUDType("default");
            }

            //判断是否需要显示图标
            LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.getEntityPatch(mc.player,LocalPlayerPatch.class);
            if(localPlayerPatch != null){
                LivingEntity target = localPlayerPatch.getTarget();
                LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target,LivingEntityPatch.class);
                if(targetPatch != null){
                    AnimationPlayer animationPlayer =  targetPatch.getAnimator().getPlayerFor(null);
                    if (animationPlayer != null) {
                        AssetAccessor<? extends StaticAnimation> currentAnimation = animationPlayer.getRealAnimation();
                        //检测可处决的条件
                        if (ExecutionHandler.isTargetGuardBreak(currentAnimation, targetPatch) && ExecutionHandler.canExecute(mc.player, localPlayerPatch, target, targetPatch)) {
                            //检测是否处于破防状态
                            float totalTime = currentAnimation.get().getTotalTime();
                            float currentTime = animationPlayer.getElapsedTime();
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

    @SubscribeEvent
    public static void OnRenderGUI(RenderGuiEvent.Pre event){
        if(mc.level == null || mc.player == null || currentHudType == null || !showExecutionIcon) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        PoseStack poseStack = guiGraphics.pose();

        int renderX = event.getWindow().getGuiScaledWidth() / 2 + 40;
        int renderY = event.getWindow().getGuiScaledHeight() / 2 + 40;

        if(CEClientConfig.ICON_DISPLAY.get()) {
            poseStack.pushPose();
            poseStack.translate(renderX, renderY, 0);
            poseStack.scale(1, 1, 1);
            drawExecutionIcon(event.getGuiGraphics(), -16, -32, 32);
            poseStack.popPose();
        }

        if(CEClientConfig.SHOW_TEXT_DISPLAY.get()) {

            Component text = Component.translatable("hud.combat_evolution.execution_tooltip", CEKeyMappings.EXECUTION.getTranslatedKeyMessage());

            List<FormattedCharSequence> lines = mc.font.split(text, Integer.MAX_VALUE);

            int textWidth = 0;
            int textHeight = mc.font.lineHeight * lines.size();

            for (FormattedCharSequence line : lines) {
                int width = mc.font.width(line);
                if (width > textWidth) {
                    textWidth = width;
                }
            }

            poseStack.pushPose();
            poseStack.translate(renderX, renderY, 0);
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
                        mc.font, line,
                        -textWidth / 2, offsetY,
                        0XFFFFFFFF
                );
                offsetY += mc.font.lineHeight;
            }

            poseStack.popPose();
        }
    }

    public static void drawExecutionIcon(GuiGraphics guiGraphics, float x, float y, float iconSize) {

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();

        //绘制背景
        ResourceLocation background = currentHudType.getBackground();
        if(background != null) {
            RenderSystem.setShaderTexture(0, background);
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            buf.vertex(matrix, x, y, 0).uv(0, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix,x + iconSize, y, 0).uv(1, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, x + iconSize, y + iconSize, 0).uv(1, 1).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, x, y + iconSize, 0).uv(0, 1).color(1, 1, 1, 1).endVertex();

            BufferUploader.drawWithShader(buf.end());
        }

        // 绘制进度外框
        float filled = 1.0F - timePercent;
        ResourceLocation progress = currentHudType.getProgress();
        if(progress != null) {
            RenderSystem.setShaderTexture(0, progress);
            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);

            float cx = x + iconSize / 2f;
            float cy = y + iconSize / 2f;
            buf.vertex(matrix, cx, cy, 0).uv(0.5F, 0.5F).endVertex();
            float filledAngle = 360.0F * filled;


            //根据角度动态调整段数（每3°一段，最少3段）
            int segments = Math.max(3, (int)(filledAngle / 3.0));
            double startRad = Math.toRadians(90);
            double step = Math.toRadians(filledAngle / segments);

            float half = iconSize / 2f;

            for (int i = 0; i <= segments; i++) {
                double angle = startRad - step * i;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                // 计算扇形顶点落在正方形边界上的缩放半径
                float scale = half / Math.max(Math.abs(cos), Math.abs(sin));

                float px = cx + cos * scale;
                float py = cy - sin * scale;

                float u = (px - x) / iconSize;
                float v = (py - y) / iconSize;

                buf.vertex(matrix, px, py, 0).uv(u, v).endVertex();
            }

            BufferUploader.drawWithShader(buf.end());
        }

        //绘制遮罩
        List<ResourceLocation> overlays = currentHudType.getOverlays();
        if(!overlays.isEmpty()){
            int index = Mth.clamp((int)(overlays.size() * filled),0,overlays.size() - 1);
            RenderSystem.setShaderTexture(0, overlays.get(index));

            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            buf.vertex(matrix, x, y, 0).uv(0, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix,x + iconSize, y, 0).uv(1, 0).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, x + iconSize, y + iconSize, 0).uv(1, 1).color(1, 1, 1, 1).endVertex();
            buf.vertex(matrix, x, y + iconSize, 0).uv(0, 1).color(1, 1, 1, 1).endVertex();

            BufferUploader.drawWithShader(buf.end());


            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}
