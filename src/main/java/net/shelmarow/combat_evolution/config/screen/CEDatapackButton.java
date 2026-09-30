package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class CEDatapackButton extends AbstractWidget {
    private Runnable action;
    private boolean primary;
    private boolean sectionHeader;
    private boolean danger;
    private Component customTooltip;

    CEDatapackButton(int x, int y, int width, int height, Component label, boolean primary, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
        this.primary = primary;
        this.danger = label.getString().equalsIgnoreCase("x");
    }

    void setAction(Runnable action) {
        this.action = action;
    }

    void setPrimary(boolean primary) {
        this.primary = primary;
    }

    void setDanger(boolean danger) {
        this.danger = danger;
    }

    void setSectionHeader(boolean sectionHeader) {
        this.sectionHeader = sectionHeader;
    }

    void setCustomTooltip(Component customTooltip) {
        this.customTooltip = customTooltip;
    }

    Component getCustomTooltip() {
        return customTooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        if (sectionHeader) {
            if (hovered) {
                graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x6636A9D2);
                graphics.fill(getX(), getY(), getX() + 2, getY() + height, 0xFF64D2FF);
                graphics.fill(getX() + 2, getY() + height - 1, getX() + width, getY() + height, 0xFF64D2FF);
            }
            String label = this.getMessage().getString();
            var font = Minecraft.getInstance().font;
            int color = active ? (hovered ? 0xFF8BE2FF : 0xFFE5EDF5) : 0xFF8793A0;
            graphics.drawString(font, label, getX() + 8, getY() + (height - 8) / 2, color);
            return;
        }
        int fill = danger ? (hovered ? 0xFF803A42 : 0xFF603039) : !active ? 0xFF202832 : primary
                ? (hovered ? 0xFF28658A : 0xFF214D69)
                : (hovered ? 0xFF374657 : 0xFF273341);
        int edge = danger ? (hovered ? 0xFFFF7B85 : 0xFFB95E68)
                : !active ? 0xFF34404C : primary ? 0xFF64D2FF : 0xFF526477;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, edge);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, edge);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, edge);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, edge);
        String label = this.getMessage().getString();
        var font = Minecraft.getInstance().font;
        String visible = font.plainSubstrByWidth(label, Math.max(1, width - 10));
        graphics.drawCenteredString(font, visible, getX() + width / 2, getY() + (height - 8) / 2,
                danger ? 0xFFFFE5E7 : active ? 0xFFF1F5F9 : 0xFF8793A0);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (active) action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
