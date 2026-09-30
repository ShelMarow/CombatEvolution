package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.client.hud.execution.ExecutionHUD;
import net.shelmarow.combat_evolution.client.hud.execution.HUDTypeManager;
import net.shelmarow.combat_evolution.client.hud.execution.types.HUDType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HUDTypeSelectionScreen extends Screen {
    private static final int OUTER_MARGIN = 8;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 24;

    private final CombatEvolutionConfigScreen parent;
    private final List<ResourceLocation> hudTypes;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();
    private String selectedType;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelBottom;
    private int footerHeight;
    private int leftX;
    private int rightX;
    private int columnWidth;
    private int listTop;
    private int listBottom;
    private int listScroll;

    public HUDTypeSelectionScreen(CombatEvolutionConfigScreen parent, String selectedType) {
        super(Component.translatable("config.combat_evolution.hud_type_screen"));
        this.parent = parent;
        this.selectedType = selectedType;
        this.hudTypes = HUDTypeManager.getAllHUDTypes().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(850, width - OUTER_MARGIN * 2));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(OUTER_MARGIN, Math.max(0, height / 4));
        panelBottom = Math.max(panelTop + 1, height - OUTER_MARGIN);
        footerHeight = Math.min(FOOTER_HEIGHT, Math.max(22, (panelBottom - panelTop) / 6));

        int innerLeft = panelLeft + Math.min(18, Math.max(4, panelWidth / 12));
        int innerWidth = Math.max(1, panelWidth - (innerLeft - panelLeft) * 2);
        int gap = Math.min(14, Math.max(4, innerWidth / 30));
        columnWidth = Math.max(1, (innerWidth - gap) / 2);
        leftX = innerLeft;
        rightX = innerLeft + columnWidth + gap;
        listTop = Math.min(panelBottom, panelTop + 58);
        listBottom = Math.max(listTop, panelBottom - footerHeight - 8);

        if (selectedType == null || HUDTypeManager.getHUDType(selectedType) == null) {
            selectedType = hudTypes.isEmpty() ? "combat_evolution:default" : hudTypes.get(0).toString();
        }
    }

    private int visibleRows() {
        return Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = visibleRows();
        if (mouseX >= leftX && mouseX <= leftX + columnWidth && mouseY >= listTop && mouseY <= listBottom && visible > 0) {
            listScroll = clamp(listScroll - (int) Math.signum(delta) * 3, 0, Math.max(0, hudTypes.size() - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, leftX + columnWidth - 3,
                listTop, listBottom, hudTypes.size(), visibleRows(), listScroll,
                value -> listScroll = value)) return true;
        if (button == 0 && mouseY >= panelBottom - footerHeight && mouseY < panelBottom - 4) {
            int buttonWidth = Math.min(112, Math.max(64, (panelWidth - 48) / 4));
            int saveX = panelLeft + panelWidth - 20 - buttonWidth;
            int cancelX = saveX - 8 - buttonWidth;
            if (mouseX >= saveX && mouseX <= saveX + buttonWidth) {
                parent.setHUDTypeDraft(selectedType);
                if (minecraft != null) minecraft.setScreen(parent);
                return true;
            }
            if (mouseX >= cancelX && mouseX <= cancelX + buttonWidth) {
                closeWithoutSaving();
                return true;
            }
        }
        if (button == 0 && mouseX >= leftX && mouseX < leftX + columnWidth && mouseY >= listTop && mouseY < listBottom) {
            int row = (int) (mouseY - listTop) / ROW_HEIGHT;
            int index = listScroll + row;
            if (index >= 0 && index < hudTypes.size()) {
                selectedType = hudTypes.get(index).toString();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.drag(mouseY, button, value -> listScroll = value)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.release(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeWithoutSaving();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            parent.setHUDTypeDraft(selectedType);
            if (minecraft != null) minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeWithoutSaving() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        drawPanel(graphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop, 0xE6161D27, 0xFF3A4656);

        graphics.drawString(font, title, leftX, panelTop + 12, 0xFFF1F5F9);
        graphics.fill(panelLeft + 12, panelTop + 40, panelLeft + panelWidth - 12, panelTop + 41, 0xFF344658);
        graphics.drawString(font, Component.translatable("config.combat_evolution.hud_type_available", hudTypes.size()),
                leftX, panelTop + 44, 0xFF64D2FF);
        graphics.drawString(font, Component.translatable("config.combat_evolution.hud_type_preview"),
                rightX, panelTop + 44, 0xFF64D2FF);

        drawPanel(graphics, leftX, listTop - 3, columnWidth, Math.max(0, listBottom - listTop + 3), 0xA91D2632, 0xFF2C3949);
        drawPanel(graphics, rightX, listTop - 3, columnWidth, Math.max(0, listBottom - listTop + 3), 0xA91D2632, 0xFF2C3949);

        graphics.enableScissor(leftX + 1, listTop, leftX + columnWidth - 1, listBottom);
        int visible = visibleRows();
        for (int row = 0; row < visible && listScroll + row < hudTypes.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            ResourceLocation typeId = hudTypes.get(listScroll + row);
            boolean selected = typeId.toString().equals(selectedType);
            boolean hovered = mouseX >= leftX && mouseX < leftX + columnWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            int fill = selected ? 0xFF1D6178 : (hovered ? 0xFF344658 : 0x001D2632);
            graphics.fill(leftX + 1, y, leftX + columnWidth - 1, y + ROW_HEIGHT, fill);
            graphics.drawString(font, typeId.toString(), leftX + 8, y + 8, selected ? 0xFF64D2FF : 0xFFE7ECF2);
            graphics.fill(leftX + 6, y + ROW_HEIGHT - 1, leftX + columnWidth - 6, y + ROW_HEIGHT, 0x332C3949);
        }
        graphics.disableScissor();

        graphics.enableScissor(rightX + 1, listTop, rightX + columnWidth - 1, listBottom);
        HUDType selectedHudType = HUDTypeManager.getHUDType(selectedType);
        String selectedName = selectedType == null ? "" : selectedType;
        int previewSize = Math.min(72, Math.max(32, columnWidth - 32));
        int previewX = rightX + (columnWidth - previewSize) / 2;
        int previewY = listTop + Math.max(12, (listBottom - listTop - previewSize) / 2 - 12);
        float previewElapsed = (System.currentTimeMillis() % 5000L) / 5000.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(previewX, previewY, 0);
        ExecutionHUD.drawExecutionIcon(graphics, partialTick, selectedHudType, previewSize, previewElapsed, previewElapsed);
        graphics.pose().popPose();
        graphics.drawCenteredString(font, selectedName, rightX + columnWidth / 2,
                Math.min(listBottom - font.lineHeight - 4, previewY + previewSize + 12), 0xFFE7ECF2);
        graphics.disableScissor();

        int footerY = panelBottom - footerHeight;
        graphics.fill(panelLeft + 12, footerY, panelLeft + panelWidth - 12, footerY + 1, 0xFF46576B);
        int buttonWidth = Math.min(112, Math.max(64, (panelWidth - 48) / 4));
        int saveX = panelLeft + panelWidth - 20 - buttonWidth;
        int cancelX = saveX - 8 - buttonWidth;
        int buttonHeight = Math.min(22, Math.max(16, footerHeight - 8));
        int buttonY = footerY + (footerHeight - buttonHeight) / 2;
        drawButton(graphics, cancelX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("config.combat_evolution.cancel"), false, mouseX, mouseY);
        drawButton(graphics, saveX, buttonY, buttonWidth, buttonHeight,
                Component.translatable("config.combat_evolution.save"), true, mouseX, mouseY);
        scrollbar.render(graphics, leftX + columnWidth - 3, listTop, listBottom,
                hudTypes.size(), visible, listScroll);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int fill, int outline) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height, outline);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, Component label,
                            boolean primary, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int fill = primary ? (hovered ? 0xFF267E9B : 0xFF1D6178) : (hovered ? 0xFF354457 : 0xFF273342);
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height, primary ? 0xFF64D2FF : 0xFF46576B);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(label, width - 10);
        if (!lines.isEmpty()) graphics.drawCenteredString(font, lines.get(0), x + width / 2,
                y + (height - font.lineHeight) / 2, 0xFFF4F7FA);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }
}
