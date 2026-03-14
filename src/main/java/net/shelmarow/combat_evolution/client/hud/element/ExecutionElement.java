package net.shelmarow.combat_evolution.client.hud.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.shelmarow.combat_evolution.client.hud.execution.ExecutionHUD;
import net.shelmarow.combat_evolution.client.hud.execution.HUDTypeManager;
import net.shelmarow.combat_evolution.client.hud.execution.types.HUDType;
import net.shelmarow.combat_evolution.config.CEClientConfig;

public class ExecutionElement {
    public double x;
    public double y;
    public int width;
    public int height;
    public boolean selected = false;

    public ExecutionElement(int x, int y, int w, int h){
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    public void renderExample(GuiGraphics pGuiGraphics, float partialTick) {
        PoseStack poseStack = pGuiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        HUDType hudType = HUDTypeManager.getHUDType(CEClientConfig.HUD_TYPE.get());
        ExecutionHUD.drawExecutionIcon(pGuiGraphics, partialTick, hudType, 32);

        poseStack.popPose();
    }

    public boolean isMouseOver(int mouseX, int mouseY){
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
