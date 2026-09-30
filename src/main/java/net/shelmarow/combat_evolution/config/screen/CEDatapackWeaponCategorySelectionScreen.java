package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
final class CEDatapackWeaponCategorySelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final Screen parent;
    private final JsonArray selected;
    private final List<String> options = new ArrayList<>();
    private final List<String> filtered = new ArrayList<>();
    private EditBox search;
    private int left, top, panelWidth, bottom, listTop, listBottom, scroll;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();

    CEDatapackWeaponCategorySelectionScreen(Screen parent, JsonArray selected) {
        super(Component.translatable("config.combat_evolution.datapack_weapon_category"));
        this.parent = parent;
        this.selected = selected;
        for (WeaponCategory category : WeaponCategory.ENUM_MANAGER.universalValues()) {
            String name = category.toString();
            if (!contains(options, name)) options.add(name);
        }
        for (JsonElement element : selected) {
            if (element.isJsonPrimitive() && !contains(options, element.getAsString())) options.add(element.getAsString());
        }
        options.sort(String.CASE_INSENSITIVE_ORDER);
        filtered.addAll(options);
    }

    private static boolean contains(List<String> list, String value) {
        return list.stream().anyMatch(item -> item.equalsIgnoreCase(value));
    }

    private boolean isSelected(String value) {
        for (JsonElement element : selected)
            if (element.isJsonPrimitive() && element.getAsString().equalsIgnoreCase(value)) return true;
        return false;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(620, width - 12));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);
        search = new EditBox(font, left + 12, top + 32, Math.max(1, panelWidth - 24), 20, title);
        search.setHint(Component.translatable("config.combat_evolution.datapack_search_weapon_category_hint"));
        search.setResponder(this::filter);
        addRenderableWidget(search);
        listTop = Math.min(top + 78, Math.max(top, bottom - 38));
        listBottom = Math.max(listTop, bottom - 38);
        addRenderableWidget(new CEDatapackButton(left + panelWidth - 108, bottom - 28, 96, 20,
                Component.translatable("config.combat_evolution.datapack_behavior_done"), true, this::onClose));
    }

    private void filter(String query) {
        filtered.clear();
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        for (String option : options)
            if (option.toLowerCase(Locale.ROOT).contains(normalized)) filtered.add(option);
        scroll = 0;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop + ROW_HEIGHT - 1) / ROW_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Math.max(0, Math.min(Math.max(0, filtered.size() - visibleRows()),
                    scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, left + panelWidth - 13, listTop, listBottom,
                filtered.size(), visibleRows(), scroll, value -> scroll = value)) return true;
        if (button == 0 && mouseX >= left + 8 && mouseX < left + panelWidth - 8
                && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + ((int) mouseY - listTop) / ROW_HEIGHT;
            if (index >= 0 && index < filtered.size()) {
                String value = filtered.get(index);
                if (isSelected(value)) {
                    for (int i = selected.size() - 1; i >= 0; i--)
                        if (selected.get(i).isJsonPrimitive() && selected.get(i).getAsString().equalsIgnoreCase(value))
                            selected.remove(i);
                } else selected.add(value);
            }
            return true;
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
        graphics.fill(left, top, left + panelWidth, bottom, 0xE6161D27);
        graphics.fill(left, top, left + panelWidth, top + 1, 0xFF3A4656);
        graphics.fill(left, bottom - 1, left + panelWidth, bottom, 0xFF3A4656);
        graphics.fill(left, top, left + 1, bottom, 0xFF3A4656);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, bottom, 0xFF3A4656);
        graphics.drawCenteredString(font, title, width / 2, top + 11, 0xFFF1F5F9);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_selected_count", selected.size()),
                left + 13, top + 58, 0xFF64D2FF);
        graphics.fill(left + 8, listTop, left + panelWidth - 8, listBottom, 0xA91D2632);
        graphics.enableScissor(left + 8, listTop, left + panelWidth - 8, listBottom);
        for (int row = 0; row < visibleRows() && scroll + row < filtered.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            String option = filtered.get(scroll + row);
            boolean checked = isSelected(option);
            boolean hovered = mouseX >= left + 8 && mouseX < left + panelWidth - 8
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(left + 9, y, left + panelWidth - 9, y + ROW_HEIGHT - 1,
                    checked ? 0xFF214D69 : hovered ? 0xFF34485C : 0xA91D2632);
            graphics.fill(left + 16, y + 6, left + 28, y + 18, checked ? 0xFF64D2FF : 0xFF526477);
            graphics.fill(left + 18, y + 8, left + 26, y + 16, checked ? 0xFF214D69 : 0xFF1D2632);
            if (checked) graphics.drawString(font, "x", left + 20, y + 8, 0xFFE7ECF2);
            graphics.drawString(font, font.plainSubstrByWidth(option, Math.max(1, panelWidth - 60)),
                    left + 36, y + 8, 0xFFE7ECF2);
        }
        graphics.disableScissor();
        scrollbar.render(graphics, left + panelWidth - 13, listTop, listBottom,
                filtered.size(), visibleRows(), scroll);
        CEDatapackViewport.drawDividers(graphics, left + 8, left + panelWidth - 8, listTop, listBottom);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
