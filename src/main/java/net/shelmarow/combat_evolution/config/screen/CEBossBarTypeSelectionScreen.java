package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.bossbar.client.BossBarTypeManager;
import net.shelmarow.combat_evolution.bossbar.client.types.AbstractBossBarType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class CEBossBarTypeSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 25;
    private final Screen parent;
    private final List<AbstractBossBarType> types;
    private final Consumer<String> onSelect;
    private AbstractBossBarType selected;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();
    private int left, top, panelWidth, bottom, listLeft, listRight, listTop, listBottom;
    private int previewLeft, previewRight, scroll;

    CEBossBarTypeSelectionScreen(Screen parent, String current, Consumer<String> onSelect) {
        super(Component.translatable("config.combat_evolution.datapack_field.ceBossBar_bossBarType"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.types = BossBarTypeManager.getInstance().getBossBarTypes().stream()
                .sorted(Comparator.comparing(AbstractBossBarType::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        this.selected = types.stream().filter(type -> type.getName().equals(current)).findFirst()
                .orElse(types.isEmpty() ? null : types.get(0));
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(800, width - 12));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);

        int contentLeft = left + 12;
        int contentRight = left + panelWidth - 12;
        int contentWidth = Math.max(1, contentRight - contentLeft);
        int listWidth = Math.min(contentWidth, Math.max(72, contentWidth / 4));
        listLeft = contentLeft;
        listRight = listLeft + listWidth;
        previewLeft = listRight + 12;
        previewRight = contentRight;
        listTop = Math.min(top + 54, Math.max(top, bottom - 40));
        listBottom = Math.max(listTop, bottom - 40);

        int buttonWidth = Math.min(112, Math.max(64, (panelWidth - 44) / 5));
        int buttonY = bottom - 28;
        addRenderableWidget(new CEDatapackButton(left + panelWidth - 16 - buttonWidth * 2 - 8,
                buttonY, buttonWidth, 20, Component.translatable("config.combat_evolution.cancel"),
                false, this::onClose));
        CEDatapackButton choose = new CEDatapackButton(left + panelWidth - 16 - buttonWidth,
                buttonY, buttonWidth, 20, Component.translatable("config.combat_evolution.select"),
                true, this::applySelection);
        choose.active = selected != null;
        addRenderableWidget(choose);
    }

    private int visibleRows() {
        return Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
    }

    private void applySelection() {
        if (selected == null || minecraft == null) return;
        onSelect.accept(selected.getName());
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom) {
            scroll = Math.max(0, Math.min(Math.max(0, types.size() - visibleRows()),
                    scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, listRight - 5, listTop, listBottom,
                types.size(), visibleRows(), scroll, value -> scroll = value)) return true;
        if (button == 0 && mouseX >= listLeft && mouseX < listRight
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) ((mouseY - listTop) / ROW_HEIGHT);
            if (index >= 0 && index < types.size()) {
                selected = types.get(index);
                return true;
            }
        }
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
        drawPanel(graphics, left, top, panelWidth, bottom - top, 0xE6161D27, 0xFF3A4656);
        graphics.drawCenteredString(font, title, width / 2, top + 11, 0xFFF1F5F9);

        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_field.ceBossBar_bossBarType"),
                listLeft, top + 34, 0xFF8BE2FF);
        graphics.drawString(font, Component.translatable("config.combat_evolution.boss_bar_type_preview"),
                previewLeft, top + 34, 0xFF8BE2FF);

        graphics.fill(listLeft, listTop, listRight, listBottom, 0xA91D2632);
        graphics.fill(listLeft, listTop, listRight, listTop + 1, 0xFF405165);
        graphics.fill(previewLeft, listTop, previewRight, listBottom, 0xA91D2632);
        graphics.renderOutline(listLeft, listTop, Math.max(1, listRight - listLeft),
                Math.max(1, listBottom - listTop), 0xFF405165);
        graphics.renderOutline(previewLeft, listTop, Math.max(1, previewRight - previewLeft),
                Math.max(1, listBottom - listTop), 0xFF405165);

        graphics.enableScissor(listLeft + 1, listTop + 1, Math.max(listLeft + 1, listRight - 6), listBottom - 1);
        int rows = visibleRows();
        for (int row = 0; row < rows && scroll + row < types.size(); row++) {
            AbstractBossBarType type = types.get(scroll + row);
            int y = listTop + row * ROW_HEIGHT;
            boolean active = type == selected;
            boolean hovered = mouseX >= listLeft && mouseX < listRight && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(listLeft + 1, y, listRight - 6, y + ROW_HEIGHT - 1,
                    active ? 0xFF214D69 : hovered ? 0xFF34485C : 0xA91D2632);
            graphics.drawString(font, font.plainSubstrByWidth(type.getName(),
                            Math.max(1, listRight - listLeft - 20)),
                    listLeft + 7, y + 8, 0xFFE7ECF2);
        }
        graphics.disableScissor();
        scrollbar.render(graphics, listRight - 5, listTop, listBottom,
                types.size(), visibleRows(), scroll);

        if (selected == null) {
            graphics.drawCenteredString(font,
                    Component.translatable("config.combat_evolution.boss_bar_type_empty"),
                    (previewLeft + previewRight) / 2, (listTop + listBottom) / 2, 0xFFAAB4C2);
        } else {
            graphics.drawString(font, selected.getName(), previewLeft + 14, listTop + 14, 0xFFE7ECF2);
            graphics.drawString(font, selected.getDefaultTexture().toString(), previewLeft + 14,
                    listTop + 28, 0xFF8798A8);
            int previewAreaWidth = Math.max(1, previewRight - previewLeft - 32);
            int previewAreaHeight = Math.max(1, listBottom - listTop - 82);
            float scale = Math.min(2.0F, Math.min(previewAreaWidth / (float) selected.getPreviewWidth(),
                    previewAreaHeight / (float) selected.getPreviewHeight()));
            int drawWidth = Math.round(selected.getPreviewWidth() * scale);
            int drawHeight = Math.round(selected.getPreviewHeight() * scale);
            int drawX = previewLeft + (previewRight - previewLeft - drawWidth) / 2;
            int drawY = listTop + 54 + Math.max(0, (previewAreaHeight - drawHeight) / 2);
            selected.renderPreview(graphics, drawX, drawY, scale);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int outline) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height, outline);
    }
}
