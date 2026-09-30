package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;

final class CEDatapackViewport {
    private CEDatapackViewport() { }

    static void drawDividers(GuiGraphics graphics, int left, int right, int top, int bottom) {
        if (right <= left || bottom <= top) return;
        graphics.fill(left, top - 1, right, top, 0xFF52687C);
        graphics.fill(left, bottom, right, bottom + 1, 0xFF52687C);
    }
}
