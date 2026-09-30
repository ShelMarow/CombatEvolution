package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class CEDatapackGuiScaleSlider extends AbstractWidget {
    private final int maxScale;
    private int scale;

    CEDatapackGuiScaleSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        scale = Minecraft.getInstance().options.guiScale().get();
        maxScale = Math.max(6, scale);
        updateMessage();
    }

    private void updateMessage() {
        setMessage(Component.translatable("config.combat_evolution.datapack_gui_scale")
                .append(": ").append(scale == 0
                        ? Component.translatable("config.combat_evolution.datapack_gui_scale_auto")
                        : Component.literal(Integer.toString(scale))));
    }

    private void updateFromMouse(double mouseX) {
        double fraction = (mouseX - getX() - 5) / Math.max(1, width - 10);
        int position = Math.max(0, Math.min(maxScale, (int) Math.round(fraction * maxScale)));
        scale = position == maxScale ? 0 : position + 1;
        updateMessage();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        updateFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        updateFromMouse(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.guiScale().get() == scale) return;
        minecraft.options.guiScale().set(scale);
        minecraft.options.save();
        minecraft.resizeDisplay();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int edge = isHoveredOrFocused() ? 0xFF64D2FF : 0xFF526477;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF273341);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, edge);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, edge);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, edge);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, edge);
        var font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, font.plainSubstrByWidth(getMessage().getString(), Math.max(1, width - 8)),
                getX() + width / 2, getY() + 3, 0xFFF1F5F9);
        int trackLeft = getX() + 5;
        int trackRight = getX() + width - 5;
        graphics.fill(trackLeft, getY() + height - 5, trackRight, getY() + height - 3, 0xFF121B24);
        int position = scale == 0 ? maxScale : scale - 1;
        int knobX = trackLeft + (trackRight - trackLeft) * position / maxScale;
        graphics.fill(trackLeft, getY() + height - 5, knobX, getY() + height - 3, 0xFF64D2FF);
        graphics.fill(knobX - 2, getY() + height - 7, knobX + 2, getY() + height - 1, 0xFFB9EDFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
