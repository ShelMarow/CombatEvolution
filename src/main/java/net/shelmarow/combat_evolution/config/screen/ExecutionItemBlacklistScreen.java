package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ExecutionItemBlacklistScreen extends Screen {
    private static final int OUTER_MARGIN = 8;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 22;

    private final CombatEvolutionConfigScreen parent;
    private final Set<String> selectedItems = new LinkedHashSet<>();
    private final List<ItemEntry> allItems = new ArrayList<>();
    private final List<ItemEntry> filteredItems = new ArrayList<>();
    private final CEDatapackScrollbar leftScrollbar = new CEDatapackScrollbar();
    private final CEDatapackScrollbar rightScrollbar = new CEDatapackScrollbar();

    private EditBox searchBox;
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
    private int leftScroll;
    private int rightScroll;

    public ExecutionItemBlacklistScreen(CombatEvolutionConfigScreen parent, List<String> selected) {
        super(Component.translatable("config.combat_evolution.blacklist_title"));
        this.parent = parent;
        selectedItems.addAll(selected);
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item == Items.AIR) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) allItems.add(new ItemEntry(id, item, item.getDescription()));
        }
        allItems.sort(Comparator.comparing(entry -> entry.id.toString()));
        filteredItems.addAll(allItems);
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(900, width - OUTER_MARGIN * 2));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(OUTER_MARGIN, Math.max(0, height / 4));
        panelBottom = Math.max(panelTop + 1, height - OUTER_MARGIN);
        footerHeight = Math.min(FOOTER_HEIGHT, Math.max(22, (panelBottom - panelTop) / 6));

        int innerLeft = panelLeft + Math.min(18, Math.max(4, panelWidth / 12));
        int innerWidth = Math.max(1, panelWidth - (innerLeft - panelLeft) * 2);
        int gap = Math.min(12, Math.max(4, innerWidth / 30));
        columnWidth = Math.max(1, (innerWidth - gap) / 2);
        leftX = innerLeft;
        rightX = innerLeft + columnWidth + gap;
        listTop = Math.min(panelBottom, panelTop + 76);
        listBottom = Math.max(listTop, panelBottom - footerHeight - 8);

        searchBox = new EditBox(font, rightX, panelTop + 44, columnWidth, 20,
                Component.translatable("config.combat_evolution.blacklist_search"));
        searchBox.setHint(Component.translatable("config.combat_evolution.blacklist_search_hint"));
        searchBox.setResponder(this::filterItems);
        addRenderableWidget(searchBox);
    }

    private void filterItems(String text) {
        filteredItems.clear();
        String query = text.trim().toLowerCase(Locale.ROOT);
        for (ItemEntry entry : allItems) {
            if (query.isEmpty() || entry.id.toString().toLowerCase(Locale.ROOT).contains(query)
                    || entry.name.getString().toLowerCase(Locale.ROOT).contains(query)) {
                filteredItems.add(entry);
            }
        }
        rightScroll = 0;
    }

    private int visibleRows() {
        return Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = visibleRows();
        if (visible <= 0 || mouseY < listTop || mouseY > listBottom) return super.mouseScrolled(mouseX, mouseY, delta);
        int amount = (int) Math.signum(delta) * 3;
        if (mouseX < rightX) {
            leftScroll = clamp(leftScroll - amount, 0, Math.max(0, selectedItems.size() - visible));
        } else {
            rightScroll = clamp(rightScroll - amount, 0, Math.max(0, filteredItems.size() - visible));
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int visible = visibleRows();
        if (leftScrollbar.press(mouseX, mouseY, button, leftX + columnWidth - 3,
                listTop, listBottom, selectedItems.size(), visible, leftScroll, value -> leftScroll = value)) return true;
        if (rightScrollbar.press(mouseX, mouseY, button, rightX + columnWidth - 3,
                listTop, listBottom, filteredItems.size(), visible, rightScroll, value -> rightScroll = value)) return true;
        if (button == 0 && mouseY >= panelBottom - footerHeight && mouseY < panelBottom - 4) {
            int buttonWidth = Math.min(112, Math.max(64, (panelWidth - 48) / 4));
            int saveX = panelLeft + panelWidth - 20 - buttonWidth;
            int cancelX = saveX - 8 - buttonWidth;
            if (mouseX >= saveX && mouseX <= saveX + buttonWidth) {
                saveAndClose();
                return true;
            }
            if (mouseX >= cancelX && mouseX <= cancelX + buttonWidth) {
                closeWithoutSaving();
                return true;
            }
        }

        if (button == 0 && mouseY >= listTop && mouseY < listBottom) {
            int row = (int) (mouseY - listTop) / ROW_HEIGHT;
            if (mouseX >= leftX && mouseX < leftX + columnWidth) {
                int index = leftScroll + row;
                if (index >= 0 && index < selectedItems.size()) {
                    String itemId = selectedItems.stream().skip(index).findFirst().orElse(null);
                    if (itemId != null) selectedItems.remove(itemId);
                    leftScroll = Math.min(leftScroll, Math.max(0, selectedItems.size() - visibleRows()));
                    return true;
                }
            } else if (mouseX >= rightX && mouseX < rightX + columnWidth) {
                int index = rightScroll + row;
                if (index >= 0 && index < filteredItems.size()) {
                    String itemId = filteredItems.get(index).id.toString();
                    if (!selectedItems.add(itemId)) selectedItems.remove(itemId);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (leftScrollbar.drag(mouseY, button, value -> leftScroll = value)
                || rightScrollbar.drag(mouseY, button, value -> rightScroll = value)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (leftScrollbar.release(button) | rightScrollbar.release(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void saveAndClose() {
        parent.setBlacklistDraft(new ArrayList<>(selectedItems));
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void closeWithoutSaving() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeWithoutSaving();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        drawPanel(graphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop, 0xE6161D27, 0xFF3A4656);

        int innerLeft = leftX;
        int innerRight = rightX + columnWidth;
        graphics.fill(panelLeft + 12, panelTop + 40, panelLeft + panelWidth - 12, panelTop + 41, 0xFF344658);
        graphics.drawString(font, title, innerLeft, panelTop + 12, 0xFFF1F5F9);
        graphics.drawString(font, Component.translatable("config.combat_evolution.blacklist_selected", selectedItems.size()),
                leftX, panelTop + 48, 0xFF64D2FF);
        graphics.drawString(font, Component.translatable("config.combat_evolution.blacklist_all", filteredItems.size()),
                rightX, panelTop + 28, 0xFF64D2FF);

        drawPanel(graphics, leftX, listTop - 3, columnWidth, Math.max(0, listBottom - listTop + 3), 0xA91D2632, 0xFF2C3949);
        drawPanel(graphics, rightX, listTop - 3, columnWidth, Math.max(0, listBottom - listTop + 3), 0xA91D2632, 0xFF2C3949);

        graphics.enableScissor(leftX + 1, listTop, leftX + columnWidth - 1, listBottom);
        List<String> selected = new ArrayList<>(selectedItems);
        int visible = visibleRows();
        for (int row = 0; row < visible && leftScroll + row < selected.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            String id = selected.get(leftScroll + row);
            boolean hovered = mouseX >= leftX && mouseX < leftX + columnWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            drawListRow(graphics, leftX + 1, y, columnWidth - 2, hovered, false);
            ItemEntry entry = findItem(id);
            drawItemIcon(graphics, entry, id, leftX + 5, y + 3);
            String name = entry == null ? id : entry.name.getString();
            drawClippedText(graphics, name, leftX + 25, y + 7, columnWidth - 52, 0xFFE7ECF2);
            graphics.drawString(font, "x", leftX + columnWidth - 18, y + 6, 0xFFFF8585);
        }
        graphics.disableScissor();

        graphics.enableScissor(rightX + 1, listTop, rightX + columnWidth - 1, listBottom);
        for (int row = 0; row < visible && rightScroll + row < filteredItems.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            ItemEntry entry = filteredItems.get(rightScroll + row);
            String id = entry.id.toString();
            boolean included = selectedItems.contains(id);
            boolean hovered = mouseX >= rightX && mouseX < rightX + columnWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            drawListRow(graphics, rightX + 1, y, columnWidth - 2, hovered, included);
            graphics.renderItem(new ItemStack(entry.item), rightX + 5, y + 3);
            drawClippedText(graphics, entry.name.getString(), rightX + 25, y + 7, columnWidth - 52, 0xFFE7ECF2);
            graphics.drawString(font, included ? "*" : "+", rightX + columnWidth - 18, y + 6,
                    included ? 0xFF64D2FF : 0xFFAAB8C7);
        }
        graphics.disableScissor();

        super.render(graphics, mouseX, mouseY, partialTick);
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

        leftScrollbar.render(graphics, leftX + columnWidth - 3, listTop, listBottom,
                selectedItems.size(), visible, leftScroll);
        rightScrollbar.render(graphics, rightX + columnWidth - 3, listTop, listBottom,
                filteredItems.size(), visible, rightScroll);
    }

    private ItemEntry findItem(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return null;
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null || item == Items.AIR ? null : new ItemEntry(location, item, item.getDescription());
    }

    private void drawItemIcon(GuiGraphics graphics, ItemEntry entry, String id, int x, int y) {
        if (entry != null) {
            graphics.renderItem(new ItemStack(entry.item), x, y);
        } else {
            drawClippedText(graphics, "?", x + 4, y + 4, 12, 0xFFFF8585);
        }
    }

    private void drawListRow(GuiGraphics graphics, int x, int y, int rowWidth, boolean hovered, boolean included) {
        int fill = hovered ? 0xFF344658 : (included ? 0x6626758D : 0x001D2632);
        graphics.fill(x, y, x + rowWidth, y + ROW_HEIGHT, fill);
    }

    private void drawClippedText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) return;
        String clipped = font.plainSubstrByWidth(text, maxWidth);
        graphics.drawString(font, clipped, x, y, color);
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

    private record ItemEntry(ResourceLocation id, Item item, Component name) { }
}
