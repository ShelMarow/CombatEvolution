package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.gui.datapack.widgets.ModelPreviewer;
import yesman.epicfight.gameasset.Armatures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
final class CEAnimationPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private final Screen parent;
    private final Consumer<AssetAccessor<? extends StaticAnimation>> onSelect;
    private final List<AssetAccessor<? extends StaticAnimation>> animations = new ArrayList<>();
    private final List<AssetAccessor<? extends StaticAnimation>> filtered = new ArrayList<>();
    private EditBox search;
    private ModelPreviewer previewer;
    private AssetAccessor<? extends StaticAnimation> selected;
    private int left;
    private int top;
    private int panelWidth;
    private int bottom;
    private int listLeft;
    private int listTop;
    private int listBottom;
    private int listWidth;
    private int scroll;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();

    CEAnimationPickerScreen(Screen parent, AssetAccessor<? extends StaticAnimation> initial,
                            Consumer<AssetAccessor<? extends StaticAnimation>> onSelect) {
        super(Component.translatable("config.combat_evolution.datapack_animation_picker_title"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.selected = initial;
        Map<ResourceLocation, AnimationManager.AnimationAccessor<? extends StaticAnimation>> registered =
                AnimationManager.getInstance().getAnimations(animation -> true);
        animations.addAll(registered.values());
        animations.sort(Comparator.comparing(animation -> animation.registryName().toString()));
        filtered.addAll(animations);
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(920, width - 16));
        left = (width - panelWidth) / 2;
        top = Math.min(10, Math.max(0, height / 10));
        bottom = Math.max(top + 1, height - 10);
        listLeft = left + 12;
        listWidth = Math.max(1, (panelWidth - 32) / 2);
        search = new EditBox(font, listLeft, top + 38, listWidth, 20, Component.translatable("config.combat_evolution.datapack_search"));
        search.setHint(Component.translatable("config.combat_evolution.datapack_animation_search_hint"));
        search.setResponder(this::filter);
        addRenderableWidget(search);
        listTop = Math.min(top + 78, Math.max(top, bottom - 42));
        listBottom = Math.max(listTop, bottom - 42);

        previewer = new ModelPreviewer(10, 20, 36, 60, null, null, Armatures.BIPED, Meshes.BIPED);
        int previewX = left + panelWidth / 2 + 8;
        previewer._setX(previewX);
        previewer._setY(top + 42);
        previewer._setWidth(Math.max(1, panelWidth / 2 - 24));
        previewer._setHeight(Math.max(1, bottom - top - 94));
        previewer.resize(null);
        addRenderableWidget(previewer);
        if (selected != null) setPreview(selected);

        int buttonWidth = Math.min(120, Math.max(70, (panelWidth - 48) / 4));
        int buttonY = bottom - 30;
        addRenderableWidget(new CEDatapackButton(left + panelWidth / 2 - buttonWidth - 4, buttonY, buttonWidth, 20,
                Component.translatable("config.combat_evolution.cancel"), false, this::closePicker));
        addRenderableWidget(new CEDatapackButton(left + panelWidth / 2 + 4, buttonY, buttonWidth, 20,
                Component.translatable("config.combat_evolution.select"), true, this::selectAnimation));
    }

    private void filter(String text) {
        filtered.clear();
        String query = text.trim().toLowerCase(Locale.ROOT);
        for (AssetAccessor<? extends StaticAnimation> animation : animations) {
            if (query.isEmpty() || animation.registryName().toString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(animation);
        }
        scroll = 0;
    }

    private void setPreview(AssetAccessor<? extends StaticAnimation> animation) {
        selected = animation;
        previewer.clearAnimations();
        previewer.addAnimationToPlay(animation);
        previewer.restartAnimations();
    }

    private void selectAnimation() {
        if (selected == null) return;
        onSelect.accept(selected);
        closePicker();
    }

    private void closePicker() {
        if (minecraft != null) minecraft.setScreen(parent);
        if (previewer != null) previewer.onDestroy();
    }

    @Override
    public void tick() {
        super.tick();
        if (previewer != null) previewer._tick();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.drag(mouseY, button, value -> scroll = value)) return true;
        if (previewer != null && previewer.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.release(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listBottom) {
            int visible = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
            scroll = Math.max(0, Math.min(Math.max(0, filtered.size() - visible), scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, listLeft + listWidth - 5, listTop, listBottom,
                filtered.size(), visibleRows(), scroll, value -> scroll = value)) return true;
        if (button == 0 && mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + (int) (mouseY - listTop) / ROW_HEIGHT;
            if (index >= 0 && index < filtered.size()) {
                setPreview(filtered.get(index));
                return true;
            }
        }
        if (previewer != null && previewer.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int visibleRows() { return Math.max(0, (listBottom - listTop) / ROW_HEIGHT); }

    @Override
    public void onClose() {
        closePicker();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        drawPanel(graphics);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFF1F5F9);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_animation_available", filtered.size()),
                listLeft, top + 61, 0xFF64D2FF);
        graphics.fill(listLeft, listTop, listLeft + listWidth, listBottom, 0xA91D2632);
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + 1, 0xFF344658);
        int visible = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listBottom);
        for (int row = 0; row < visible && scroll + row < filtered.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            AssetAccessor<? extends StaticAnimation> animation = filtered.get(scroll + row);
            boolean active = selected != null && animation.registryName().equals(selected.registryName());
            boolean hovered = mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(listLeft, y, listLeft + listWidth, y + ROW_HEIGHT - 1,
                    active ? 0xFF214D69 : hovered ? 0xFF34485C : 0xA91D2632);
            String id = animation.registryName().toString();
            graphics.drawString(font, font.plainSubstrByWidth(id, listWidth - 10), listLeft + 5, y + 6, 0xFFE7ECF2);
        }
        graphics.disableScissor();
        scrollbar.render(graphics, listLeft + listWidth - 5, listTop, listBottom,
                filtered.size(), visibleRows(), scroll);
        CEDatapackViewport.drawDividers(graphics, listLeft, listLeft + listWidth, listTop, listBottom);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_animation_preview"),
                left + panelWidth / 2 + 8, top + 26, 0xFF64D2FF);
        if (selected != null) {
            String id = font.plainSubstrByWidth(selected.registryName().toString(), Math.max(1, panelWidth / 2 - 24));
            graphics.drawString(font, id, left + panelWidth / 2 + 8, bottom - 39, 0xFFB9C8D8);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(left, top, left + panelWidth, bottom, 0xE6161D27);
        graphics.fill(left, top, left + panelWidth, top + 1, 0xFF3A4656);
        graphics.fill(left, bottom - 1, left + panelWidth, bottom, 0xFF3A4656);
        graphics.fill(left, top, left + 1, bottom, 0xFF3A4656);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, bottom, 0xFF3A4656);
        graphics.fill(left + panelWidth / 2, top + 36, left + panelWidth / 2 + 1, bottom - 36, 0xFF344658);
    }
}
