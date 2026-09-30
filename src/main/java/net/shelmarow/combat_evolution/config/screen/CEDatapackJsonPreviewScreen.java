package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class CEDatapackJsonPreviewScreen extends Screen {
    private static final int LINE_HEIGHT = 10;
    private final Screen parent;
    private final String json;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int panelLeft, panelTop, panelWidth, panelBottom, viewportTop, viewportBottom, scroll;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();

    CEDatapackJsonPreviewScreen(Screen parent, String json) {
        super(Component.translatable("config.combat_evolution.datapack_json_preview"));
        this.parent = parent;
        this.json = json;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(760, width - 12));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(8, Math.max(0, height / 12));
        panelBottom = Math.max(panelTop + 1, height - 8);
        viewportTop = panelTop + 34;
        viewportBottom = Math.max(viewportTop, panelBottom - 38);
        lines.clear();
        int textWidth = Math.max(1, panelWidth - 40);
        for (String sourceLine : json.split("\\R", -1)) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal(sourceLine), textWidth);
            if (wrapped.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
            else lines.addAll(wrapped);
        }
        addRenderableWidget(new CEDatapackButton(panelLeft + panelWidth - 108, panelBottom - 28, 96, 20,
                Component.translatable("config.combat_evolution.back"), false, this::onClose));
        clampScroll();
    }

    private int visibleLines() {
        return Math.max(1, (viewportBottom - viewportTop) / LINE_HEIGHT);
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - visibleLines())));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visibleLines()),
                    scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, panelLeft + panelWidth - 10,
                viewportTop, viewportBottom, lines.size(), visibleLines(), scroll, value -> scroll = value)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.drag(mouseY, button, value -> scroll = value)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.release(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xE6161D27);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF3A4656);
        graphics.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, 0xFFF1F5F9);
        graphics.fill(panelLeft + 8, viewportTop, panelLeft + panelWidth - 8, viewportBottom, 0xA91D2632);
        graphics.enableScissor(panelLeft + 10, viewportTop, panelLeft + panelWidth - 16, viewportBottom);
        int end = Math.min(lines.size(), scroll + visibleLines() + 1);
        for (int index = scroll; index < end; index++) {
            int y = viewportTop + (index - scroll) * LINE_HEIGHT + 3;
            graphics.drawString(font, lines.get(index), panelLeft + 16, y, 0xFFE5EDF5);
        }
        graphics.disableScissor();
        CEDatapackViewport.drawDividers(graphics, panelLeft + 8, panelLeft + panelWidth - 8,
                viewportTop, viewportBottom);
        scrollbar.render(graphics, panelLeft + panelWidth - 10, viewportTop, viewportBottom,
                lines.size(), visibleLines(), scroll);
        graphics.fill(panelLeft + 10, panelBottom - 36, panelLeft + panelWidth - 10, panelBottom - 35, 0xFF46576B);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
