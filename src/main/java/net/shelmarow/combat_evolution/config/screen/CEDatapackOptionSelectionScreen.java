package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
final class CEDatapackOptionSelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 23;
    private final Screen parent;
    private final List<String> options;
    private final Consumer<String> onSelect;
    private final String selected;
    private final Function<String, Component> optionLabel;
    private final Component searchHint;
    private final List<String> filtered = new ArrayList<>();
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();
    private EditBox search;
    private int left, top, panelWidth, bottom, listTop, listBottom, scroll;

    CEDatapackOptionSelectionScreen(Screen parent, Component title, List<String> options, String selected, Consumer<String> onSelect) {
        this(parent, title, options, selected, onSelect, Component::literal,
                Component.translatable("config.combat_evolution.datapack_option_search_hint"));
    }

    CEDatapackOptionSelectionScreen(Screen parent, Component title, List<String> options, String selected,
                                    Consumer<String> onSelect, Function<String, Component> optionLabel, Component searchHint) {
        super(title);
        this.parent = parent;
        this.options = List.copyOf(options);
        this.selected = selected;
        this.onSelect = onSelect;
        this.optionLabel = optionLabel;
        this.searchHint = searchHint;
        filtered.addAll(options);
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(620, width - 12));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);
        search = new EditBox(font, left + 12, top + 32, Math.max(1, panelWidth - 24), 20,
                Component.translatable("config.combat_evolution.datapack_search_options"));
        search.setHint(searchHint);
        search.setResponder(this::filter);
        addRenderableWidget(search);
        listTop = Math.min(top + 78, Math.max(top, bottom - 38));
        listBottom = Math.max(listTop, bottom - 38);
        addRenderableWidget(new CEDatapackButton(left + panelWidth - 108, bottom - 28, 96, 20,
                Component.translatable("config.combat_evolution.cancel"), false, this::onClose));
    }

    private void filter(String query) {
        filtered.clear();
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (normalized.isEmpty() || optionLabel.apply(option).getString().toLowerCase(Locale.ROOT).contains(normalized)) filtered.add(option);
        }
        scroll = 0;
    }

    private int visibleRows() {
        return Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
    }

    private void select(String value) {
        onSelect.accept(value);
        if (minecraft != null) minecraft.setScreen(parent);
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
        if (button == 0 && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) (mouseY - listTop) / ROW_HEIGHT;
            if (index >= 0 && index < filtered.size()) {
                select(filtered.get(index));
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
        graphics.fill(left, top, left + panelWidth, bottom, 0xE6161D27);
        graphics.fill(left, top, left + panelWidth, top + 1, 0xFF3A4656);
        graphics.fill(left, bottom - 1, left + panelWidth, bottom, 0xFF3A4656);
        graphics.fill(left, top, left + 1, bottom, 0xFF3A4656);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, bottom, 0xFF3A4656);
        graphics.drawCenteredString(font, title, width / 2, top + 11, 0xFFF1F5F9);
        graphics.drawString(font, Component.literal(filtered.size() + " / " + options.size()),
                left + 13, top + 58, 0xFF64D2FF);
        graphics.fill(left + 8, listTop, left + panelWidth - 8, listBottom, 0xA91D2632);
        graphics.fill(left + 8, listTop, left + panelWidth - 8, listTop + 1, 0xFF344658);
        graphics.enableScissor(left + 8, listTop, left + panelWidth - 8, listBottom);
        int visible = visibleRows();
        for (int row = 0; row < visible && scroll + row < filtered.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            String option = filtered.get(scroll + row);
            boolean active = option.equalsIgnoreCase(selected);
            boolean hovered = mouseX >= left + 8 && mouseX < left + panelWidth - 8 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(left + 9, y, left + panelWidth - 9, y + ROW_HEIGHT - 1,
                    active ? 0xFF214D69 : hovered ? 0xFF34485C : 0xA91D2632);
            graphics.drawString(font, font.plainSubstrByWidth(optionLabel.apply(option).getString(),
                    Math.max(1, panelWidth - 34)), left + 15, y + 7, 0xFFE7ECF2);
        }
        graphics.disableScissor();
        scrollbar.render(graphics, left + panelWidth - 13, listTop, listBottom,
                filtered.size(), visibleRows(), scroll);
        CEDatapackViewport.drawDividers(graphics, left + 8, left + panelWidth - 8, listTop, listBottom);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
