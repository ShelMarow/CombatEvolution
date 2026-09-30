package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;

final class CEDatapackScrollbar {
    private boolean dragging;
    private int top, travel, maxOffset, grabOffset;

    boolean press(double mouseX, double mouseY, int button, int x, int top, int bottom,
                  int total, int visible, int offset, IntConsumer setOffset) {
        if (button != 0 || total <= visible || visible <= 0 || bottom <= top
                || mouseX < x - 4 || mouseX > x + 7 || mouseY < top || mouseY >= bottom) return false;
        int thumbHeight = thumbHeight(top, bottom, total, visible);
        int thumbTop = top + (bottom - top - thumbHeight) * offset / (total - visible);
        this.top = top;
        this.travel = bottom - top - thumbHeight;
        this.maxOffset = total - visible;
        grabOffset = mouseY >= thumbTop && mouseY < thumbTop + thumbHeight
                ? (int) mouseY - thumbTop : thumbHeight / 2;
        dragging = true;
        update(mouseY, setOffset);
        return true;
    }

    boolean drag(double mouseY, int button, IntConsumer setOffset) {
        if (!dragging || button != 0) return false;
        update(mouseY, setOffset);
        return true;
    }

    boolean release(int button) {
        if (!dragging || button != 0) return false;
        dragging = false;
        return true;
    }

    void render(GuiGraphics graphics, int x, int top, int bottom, int total, int visible, int offset) {
        if (total <= visible || visible <= 0 || bottom <= top) return;
        int thumbHeight = thumbHeight(top, bottom, total, visible);
        int thumbTop = top + (bottom - top - thumbHeight) * offset / (total - visible);
        graphics.fill(x, top, x + 4, bottom, 0xFF1B2530);
        graphics.fill(x, thumbTop, x + 4, thumbTop + thumbHeight, 0xFF64D2FF);
    }

    private void update(double mouseY, IntConsumer setOffset) {
        if (travel <= 0) return;
        int position = Math.max(0, Math.min(travel, (int) mouseY - grabOffset - top));
        setOffset.accept(position * maxOffset / travel);
    }

    private static int thumbHeight(int top, int bottom, int total, int visible) {
        int height = bottom - top;
        return Math.min(height, Math.max(14, height * visible / total));
    }
}
