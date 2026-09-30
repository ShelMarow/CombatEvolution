package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shelmarow.combat_evolution.client.hud.execution.HUDAlignment;
import net.shelmarow.combat_evolution.client.hud.execution.HUDTypeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.config.CEClientConfig;
import net.shelmarow.combat_evolution.config.CECommonConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class CombatEvolutionConfigScreen extends Screen {
    private static final int OUTER_MARGIN = 6;
    private static final int FOOTER_HEIGHT = 38;
    private static final int TOGGLE_WIDTH = 86;
    private static final int TOGGLE_HEIGHT = 22;
    private static final int PANEL_MAX_WIDTH = 720;

    private final Screen parent;
    private final List<SettingRow> settingRows = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();

    private PaintedButton executionButton;
    private PaintedButton assassinationButton;
    private PaintedButton musicButton;
    private PaintedButton executionIconButton;
    private PaintedButton assassinationIconButton;
    private PaintedButton textDisplayButton;
    private PaintedButton hudPositionButton;
    private PaintedButton hudTypeButton;
    private PaintedButton damageSourceButton;
    private PaintedButton alignmentButton;
    private PaintedButton datapackEditorButton;
    private EditBox playerDamageInput;
    private EditBox massacreInput;
    private HUDAlignment selectedAlignment;
    private Component validationError = Component.empty();
    private boolean configLoaded;
    private String draftHudType;
    private String draftPlayerDamage;
    private String draftMassacre;
    private String draftBlacklist;

    private boolean showExecution;
    private boolean showAssassination;
    private boolean showPlayMusic;
    private boolean showIconDisplay;
    private boolean showAssassinationIconDisplay;
    private boolean showTextDisplay;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelBottom;
    private int headerHeight;
    private int footerHeight;
    private int contentLeft;
    private int contentWidth;
    private int viewportTop;
    private int viewportBottom;
    private int contentHeight;
    private int scrollOffset;

    public CombatEvolutionConfigScreen(Screen parent) {
        super(Component.translatable("config.combat_evolution.client_config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (configLoaded) captureDraftInputs();
        super.init();
        panelWidth = Math.max(1, Math.min(PANEL_MAX_WIDTH, width - OUTER_MARGIN * 2));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(OUTER_MARGIN, Math.max(0, height / 4));
        panelBottom = Math.max(panelTop + 1, height - OUTER_MARGIN);
        int panelHeight = panelBottom - panelTop;
        headerHeight = Math.min(50, Math.max(26, panelHeight / 5));
        footerHeight = Math.min(FOOTER_HEIGHT, Math.max(22, panelHeight / 6));
        contentLeft = panelLeft + Math.min(20, Math.max(4, panelWidth / 12));
        contentWidth = Math.max(1, panelWidth - (contentLeft - panelLeft) * 2);
        viewportTop = Math.min(panelBottom, panelTop + headerHeight);
        viewportBottom = Math.max(viewportTop, panelBottom - footerHeight);
        int editorWidth = Math.min(148, Math.max(1, panelWidth / 3));
        datapackEditorButton = createActionButton("config.combat_evolution.open_editor",
                this::openMobDatapackEditor, panelLeft + panelWidth - editorWidth - 12,
                panelTop + Math.min(7, Math.max(0, (headerHeight - 20) / 2)), editorWidth, 20);
        addRenderableWidget(datapackEditorButton);

        if (!configLoaded) {
            showExecution = CECommonConfig.ENABLED_EXECUTION.get();
            showAssassination = CECommonConfig.ENABLED_ASSASSINATION.get();
            showPlayMusic = CEClientConfig.PLAY_CE_MUSIC.get();
            showIconDisplay = CEClientConfig.ICON_DISPLAY.get();
            showAssassinationIconDisplay = CEClientConfig.ASSASSINATION_ICON_DISPLAY.get();
            showTextDisplay = CEClientConfig.SHOW_TEXT_DISPLAY.get();
            selectedAlignment = CEClientConfig.ICON_ALIGNMENT.get();
            draftHudType = CEClientConfig.HUD_TYPE.get();
            draftPlayerDamage = CECommonConfig.EXECUTION_DAMAGE_TO_PLAYER.get().toString();
            draftMassacre = CECommonConfig.MASSACRE_ENCHANTMENT.get().toString();
            draftBlacklist = String.join(", ", CECommonConfig.EXECUTION_ITEM_BLACKLIST.get());
            configLoaded = true;
        }

        settingRows.clear();
        sectionHeaders.clear();
        contentHeight = 8;
        boolean stacked = contentWidth < 390;
        int labelWidth = Math.max(1, contentWidth - (stacked ? 0 : TOGGLE_WIDTH + 28));

        addConfigGroup("config.combat_evolution.server_config");
        addSection("config.combat_evolution.gameplay");
        executionButton = addToggle("config.combat_evolution.execution_enabled", showExecution, value -> {
            showExecution = value;
            executionButton.setValue(value);
        }, stacked, labelWidth);
        assassinationButton = addToggle("config.combat_evolution.assassination_enabled", showAssassination, value -> {
            showAssassination = value;
            assassinationButton.setValue(value);
        }, stacked, labelWidth);

        addSection("config.combat_evolution.execution_tuning");
        playerDamageInput = addTextField("config.combat_evolution.execution_damage_to_player",
                draftPlayerDamage, 110, stacked);
        damageSourceButton = addToggle("config.combat_evolution.damage_source_enabled",
                CECommonConfig.ENABLE_DAMAGE_SOURCE_TO_PLAYER.get(), value -> damageSourceButton.setValue(value), stacked, labelWidth);
        massacreInput = addTextField("config.combat_evolution.massacre_per_level",
                draftMassacre, 110, stacked);
        addSelector("config.combat_evolution.blacklist_picker",
                Component.translatable("config.combat_evolution.edit_blacklist"), this::openBlacklistSelector,
                stacked, labelWidth);

        addConfigGroup("config.combat_evolution.client_config_group");
        addSection("config.combat_evolution.hud_display");
        executionIconButton = addToggle("config.combat_evolution.icon_display", showIconDisplay, value -> {
            showIconDisplay = value;
            executionIconButton.setValue(value);
        }, stacked, labelWidth);
        assassinationIconButton = addToggle("config.combat_evolution.assassination_icon_display", showAssassinationIconDisplay, value -> {
            showAssassinationIconDisplay = value;
            assassinationIconButton.setValue(value);
        }, stacked, labelWidth);
        textDisplayButton = addToggle("config.combat_evolution.text_display", showTextDisplay, value -> {
            showTextDisplay = value;
            textDisplayButton.setValue(value);
        }, stacked, labelWidth);

        addSection("config.combat_evolution.audio");
        musicButton = addToggle("config.combat_evolution.play_music", showPlayMusic, value -> {
            showPlayMusic = value;
            musicButton.setValue(value);
        }, stacked, labelWidth);

        addSection("config.combat_evolution.hud_layout");
        alignmentButton = addSelector("config.combat_evolution.icon_alignment", alignmentName(selectedAlignment), () -> {
            HUDAlignment[] values = HUDAlignment.values();
            selectedAlignment = values[(selectedAlignment.ordinal() + 1) % values.length];
            alignmentButton.setLabel(alignmentName(selectedAlignment));
        }, stacked, labelWidth);
        hudTypeButton = addSelector("config.combat_evolution.hud_type", Component.literal(draftHudType),
                this::openHudTypeSelector, stacked, labelWidth);
        hudPositionButton = addSelector("config.combat_evolution.icon_position",
                Component.translatable("config.combat_evolution.configure_position"), this::openHudPosition,
                stacked, labelWidth);

        int visibleHeight = Math.max(0, viewportBottom - viewportTop);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, contentHeight - visibleHeight)));
        positionContentWidgets(stacked);
    }

    private PaintedButton addToggle(String translationKey, boolean initialValue, Consumer<Boolean> onChange,
                                    boolean stacked, int labelWidth) {
        Component label = Component.translatable(translationKey);
        int lines = font.split(label, labelWidth).size();
        int rowHeight = stacked ? lines * font.lineHeight + TOGGLE_HEIGHT + 12
                : Math.max(TOGGLE_HEIGHT, lines * font.lineHeight) + 10;
        int rowY = contentHeight;
        contentHeight += rowHeight;
        int buttonX = stacked ? contentLeft : contentLeft + contentWidth - TOGGLE_WIDTH;
        int buttonY = stacked ? screenY(rowY + lines * font.lineHeight + 5)
                : screenY(rowY + (rowHeight - TOGGLE_HEIGHT) / 2);
        PaintedButton button = new PaintedButton(buttonX, buttonY, stacked ? contentWidth : TOGGLE_WIDTH,
                TOGGLE_HEIGHT, toggleText(initialValue), onChange, true);
        int controlYOffset = stacked ? lines * font.lineHeight + 5 : (rowHeight - TOGGLE_HEIGHT) / 2;
        settingRows.add(new SettingRow(label, rowY, rowHeight, labelWidth, stacked, button, controlYOffset));
        addRenderableWidget(button);
        return button;
    }

    private EditBox addTextField(String translationKey, String initialValue, int preferredWidth, boolean stacked) {
        Component label = Component.translatable(translationKey);
        int fieldWidth = stacked ? contentWidth : Math.min(preferredWidth, Math.max(80, contentWidth / 2));
        int labelWidth = stacked ? contentWidth : Math.max(1, contentWidth - fieldWidth - 14);
        int lines = font.split(label, labelWidth).size();
        int rowHeight = stacked ? lines * font.lineHeight + 34 : Math.max(lines * font.lineHeight, 22) + 10;
        int rowY = contentHeight;
        contentHeight += rowHeight;
        int fieldX = stacked ? contentLeft : contentLeft + contentWidth - fieldWidth;
        int fieldY = stacked ? rowY + lines * font.lineHeight + 5 : rowY + (rowHeight - 22) / 2;
        EditBox field = new EditBox(font, fieldX, screenY(fieldY), fieldWidth, 22, label);
        field.setValue(initialValue);
        settingRows.add(new SettingRow(label, rowY, rowHeight, labelWidth, stacked, field,
                stacked ? lines * font.lineHeight + 5 : (rowHeight - 22) / 2));
        addRenderableWidget(field);
        return field;
    }

    private PaintedButton addSelector(String translationKey, Component initialValue, Runnable onChange,
                                      boolean stacked, int labelWidth) {
        Component label = Component.translatable(translationKey);
        int lines = font.split(label, labelWidth).size();
        int rowHeight = stacked ? lines * font.lineHeight + TOGGLE_HEIGHT + 12
                : Math.max(TOGGLE_HEIGHT, lines * font.lineHeight) + 10;
        int rowY = contentHeight;
        contentHeight += rowHeight;
        int buttonWidth = stacked ? contentWidth : Math.max(120, contentWidth / 3);
        int buttonX = stacked ? contentLeft : contentLeft + contentWidth - buttonWidth;
        int buttonY = stacked ? screenY(rowY + lines * font.lineHeight + 5)
                : screenY(rowY + (rowHeight - TOGGLE_HEIGHT) / 2);
        PaintedButton button = createActionButton(initialValue, onChange, buttonX, buttonY, buttonWidth, TOGGLE_HEIGHT);
        int controlYOffset = stacked ? lines * font.lineHeight + 5 : (rowHeight - TOGGLE_HEIGHT) / 2;
        settingRows.add(new SettingRow(label, rowY, rowHeight, labelWidth, stacked, button, controlYOffset));
        addRenderableWidget(button);
        return button;
    }

    private Component alignmentName(HUDAlignment alignment) {
        return Component.translatable("config.combat_evolution.alignment." + alignment.name().toLowerCase(Locale.ROOT));
    }

    private void addSection(String translationKey) {
        contentHeight += 4;
        sectionHeaders.add(new SectionHeader(Component.translatable(translationKey), contentHeight, false));
        contentHeight += font.lineHeight + 8;
    }

    private void addConfigGroup(String translationKey) {
        contentHeight += 8;
        sectionHeaders.add(new SectionHeader(Component.translatable(translationKey), contentHeight, true));
        contentHeight += font.lineHeight + 12;
    }

    private void positionContentWidgets(boolean stacked) {
        for (SettingRow row : settingRows) {
            row.control.setY(screenY(row.y + row.controlYOffset));
        }
    }

    private int screenY(int contentY) {
        return viewportTop + contentY - scrollOffset;
    }

    private Component toggleText(boolean enabled) {
        return Component.translatable(enabled ? "config.combat_evolution.on" : "config.combat_evolution.off");
    }

    private PaintedButton createActionButton(String translationKey, Runnable action, int x, int y, int buttonWidth, int buttonHeight) {
        return createActionButton(Component.translatable(translationKey), action, x, y, buttonWidth, buttonHeight);
    }

    private PaintedButton createActionButton(Component label, Runnable action, int x, int y, int buttonWidth, int buttonHeight) {
        return new PaintedButton(x, y, buttonWidth, buttonHeight, label, ignored -> action.run());
    }

    private void saveAndClose() {
        Double playerDamage = parseDouble(playerDamageInput);
        Double massacreMultiplier = parseDouble(massacreInput);
        if (playerDamage == null || massacreMultiplier == null) {
            validationError = Component.translatable("config.combat_evolution.invalid_number");
            return;
        }

        String selected = draftHudType;
        String defaultValue = "combat_evolution:default";
        String valueToSave;
        List<String> allHUDTypes = HUDTypeManager.getAllHUDTypes().stream().map(Object::toString).toList();
        if (selected.isEmpty() || !allHUDTypes.contains(selected)) {
            valueToSave = allHUDTypes.contains(defaultValue) ? defaultValue
                    : (!allHUDTypes.isEmpty() ? allHUDTypes.get(0) : defaultValue);
        } else {
            valueToSave = selected;
        }

        CECommonConfig.ENABLED_EXECUTION.set(showExecution);
        CECommonConfig.ENABLED_ASSASSINATION.set(showAssassination);
        CECommonConfig.EXECUTION_DAMAGE_TO_PLAYER.set(playerDamage);
        CECommonConfig.ENABLE_DAMAGE_SOURCE_TO_PLAYER.set(damageSourceButton.value.getString()
                .equalsIgnoreCase(Component.translatable("config.combat_evolution.on").getString()));
        CECommonConfig.MASSACRE_ENCHANTMENT.set(massacreMultiplier);
        List<String> blacklist = Arrays.stream(draftBlacklist.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
        CECommonConfig.EXECUTION_ITEM_BLACKLIST.set(blacklist);
        CECommonConfig.COMMON_SPEC.save();
        CEClientConfig.PLAY_CE_MUSIC.set(showPlayMusic);
        CEClientConfig.HUD_TYPE.set(valueToSave);
        CEClientConfig.ICON_DISPLAY.set(showIconDisplay);
        CEClientConfig.ASSASSINATION_ICON_DISPLAY.set(showAssassinationIconDisplay);
        CEClientConfig.SHOW_TEXT_DISPLAY.set(showTextDisplay);
        CEClientConfig.ICON_ALIGNMENT.set(selectedAlignment);
        CEClientConfig.CLIENT_SPEC.save();
        closeWithoutSaving();
    }

    private void captureDraftInputs() {
        if (playerDamageInput != null) draftPlayerDamage = playerDamageInput.getValue();
        if (massacreInput != null) draftMassacre = massacreInput.getValue();
    }

    private void openHudPosition() {
        if (minecraft != null) {
            captureDraftInputs();
            minecraft.setScreen(new HUDConfigScreen(this));
        }
    }

    private void openHudTypeSelector() {
        if (minecraft != null) {
            captureDraftInputs();
            minecraft.setScreen(new HUDTypeSelectionScreen(this, draftHudType));
        }
    }

    private void openBlacklistSelector() {
        if (minecraft != null) {
            captureDraftInputs();
            List<String> selected = Arrays.stream(draftBlacklist.split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
            minecraft.setScreen(new ExecutionItemBlacklistScreen(this, selected));
        }
    }

    private void openMobDatapackEditor() {
        if (minecraft != null) {
            captureDraftInputs();
            minecraft.setScreen(new CEDatapackEditorScreen(this));
        }
    }

    public void setBlacklistDraft(List<String> itemIds) {
        draftBlacklist = String.join(", ", itemIds);
    }

    public void setHUDTypeDraft(String typeId) {
        draftHudType = typeId;
        if (hudTypeButton != null) hudTypeButton.setLabel(Component.literal(typeId));
    }

    private Double parseDouble(EditBox field) {
        try {
            double value = Double.parseDouble(field.getValue().trim());
            return Double.isFinite(value) && value >= 0.0D ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void closeWithoutSaving() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewportTop && mouseY <= viewportBottom) {
            int visibleHeight = Math.max(0, viewportBottom - viewportTop);
            int maxScroll = Math.max(0, contentHeight - visibleHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * 18)));
            repositionForScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void repositionForScroll() {
        for (SettingRow row : settingRows) {
            row.control.setY(screenY(row.y + row.controlYOffset));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, panelLeft + panelWidth - 5,
                viewportTop, viewportBottom, contentHeight, viewportBottom - viewportTop,
                scrollOffset, value -> { scrollOffset = value; repositionForScroll(); })) return true;
        if (button == 0 && mouseY >= panelBottom - footerHeight && mouseY <= panelBottom - 4) {
            int buttonWidth = Math.min(104, Math.max(60, contentWidth / 3));
            int gap = 8;
            int saveX = panelLeft + panelWidth - (contentLeft - panelLeft) - buttonWidth;
            int cancelX = saveX - gap - buttonWidth;
            if (mouseX >= saveX && mouseX <= saveX + buttonWidth) {
                saveAndClose();
                return true;
            }
            if (mouseX >= cancelX && mouseX <= cancelX + buttonWidth) {
                closeWithoutSaving();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.drag(mouseY, button, value -> { scrollOffset = value; repositionForScroll(); })) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.release(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, width, height, 0xB0090D13);
        drawPanel(guiGraphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop, 0xE6161D27, 0xFF3A4656);

        guiGraphics.fill(panelLeft + 12, panelTop + 10, panelLeft + 15, panelTop + headerHeight - 4, 0xFF64D2FF);
        int titleWidth = Math.max(1, datapackEditorButton.getX() - panelLeft - 32);
        guiGraphics.drawString(font, font.plainSubstrByWidth(title.getString(), titleWidth),
                panelLeft + 24, panelTop + 9, 0xFFF1F5F9);
        if (headerHeight >= 43) {
            String subtitle = Component.translatable("config.combat_evolution.settings_subtitle").getString();
            guiGraphics.drawString(font, font.plainSubstrByWidth(subtitle, Math.max(1, panelWidth - 48)),
                    panelLeft + 24, panelTop + 30, 0xFFAAB4C2);
        }

        guiGraphics.enableScissor(panelLeft + 1, viewportTop, panelLeft + panelWidth - 1, viewportBottom);
        for (SectionHeader header : sectionHeaders) {
            int y = screenY(header.y);
            if (header.group) {
                guiGraphics.drawString(font, Component.literal(">"), contentLeft, y, 0xFF64D2FF);
                guiGraphics.drawString(font, header.label, contentLeft + 12, y, 0xFF64D2FF);
                guiGraphics.fill(contentLeft + 12, y + font.lineHeight + 2, contentLeft + contentWidth,
                        y + font.lineHeight + 3, 0xFF344658);
            } else {
                guiGraphics.drawString(font, Component.literal("-"), contentLeft + 6, y, 0xFF91A5B8);
                guiGraphics.drawString(font, header.label, contentLeft + 18, y, 0xFFAAB8C7);
                guiGraphics.fill(contentLeft + 18, y + font.lineHeight + 2, contentLeft + contentWidth,
                        y + font.lineHeight + 3, 0xFF2C3949);
            }
        }
        for (SettingRow row : settingRows) {
            int y = screenY(row.y);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(row.label, row.labelWidth);
            int labelY = row.stacked ? y : y + Math.max(0, (row.height - lines.size() * font.lineHeight) / 2);
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawString(font, lines.get(i), contentLeft, labelY + i * font.lineHeight, 0xFFE7ECF2);
            }
        }

        boolean editorVisible = datapackEditorButton.visible;
        datapackEditorButton.visible = false;
        super.render(guiGraphics, mouseX, mouseY, delta);
        datapackEditorButton.visible = editorVisible;
        guiGraphics.disableScissor();
        if (editorVisible) datapackEditorButton.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.fill(panelLeft + 12, viewportTop - 1, panelLeft + panelWidth - 12, viewportTop, 0xFF46576B);
        guiGraphics.fill(panelLeft + 12, viewportBottom, panelLeft + panelWidth - 12, viewportBottom + 1, 0xFF46576B);

        int footerY = panelBottom - footerHeight;
        if (!validationError.getString().isEmpty()) {
            guiGraphics.drawString(font, validationError, contentLeft, footerY - font.lineHeight - 3, 0xFFFF7070);
        }
        guiGraphics.fill(panelLeft + 12, footerY, panelLeft + panelWidth - 12, footerY + 1, 0xFF2C3949);
        int buttonWidth = Math.min(104, Math.max(60, contentWidth / 3));
        int gap = 8;
        int saveX = panelLeft + panelWidth - (contentLeft - panelLeft) - buttonWidth;
        int cancelX = saveX - gap - buttonWidth;
        int footerButtonHeight = Math.min(22, Math.max(16, footerHeight - 8));
        int footerButtonY = footerY + (footerHeight - footerButtonHeight) / 2;
        drawButton(guiGraphics, cancelX, footerButtonY, buttonWidth, footerButtonHeight, Component.translatable("config.combat_evolution.close"), false,
                mouseX, mouseY);
        drawButton(guiGraphics, saveX, footerButtonY, buttonWidth, footerButtonHeight, Component.translatable("config.combat_evolution.save"), true,
                mouseX, mouseY);
        scrollbar.render(guiGraphics, panelLeft + panelWidth - 5, viewportTop, viewportBottom,
                contentHeight, viewportBottom - viewportTop, scrollOffset);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int panelWidth, int panelHeight, int fill, int outline) {
        if (panelWidth <= 0 || panelHeight <= 0) return;
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, fill);
        guiGraphics.renderOutline(x, y, panelWidth, panelHeight, outline);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int buttonWidth, int buttonHeight, Component label,
                            boolean primary, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight;
        int fill = primary ? (hovered ? 0xFF267E9B : 0xFF1D6178) : (hovered ? 0xFF354457 : 0xFF273342);
        int border = primary ? 0xFF64D2FF : 0xFF46576B;
        graphics.fill(x, y, x + buttonWidth, y + buttonHeight, fill);
        graphics.renderOutline(x, y, buttonWidth, buttonHeight, border);
        List<net.minecraft.util.FormattedCharSequence> labelLines = font.split(label, buttonWidth - 12);
        if (!labelLines.isEmpty()) {
            graphics.drawCenteredString(font, labelLines.get(0), x + buttonWidth / 2,
                    y + (buttonHeight - font.lineHeight) / 2, 0xFFF4F7FA);
        }
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    private record SectionHeader(Component label, int y, boolean group) { }

    private record SettingRow(Component label, int y, int height, int labelWidth, boolean stacked,
                              AbstractWidget control, int controlYOffset) { }

    private class PaintedButton extends AbstractWidget {
        private Component value;
        private final Consumer<Boolean> onPress;
        private final boolean toggle;

        private PaintedButton(int x, int y, int width, int height, Component value, Consumer<Boolean> onPress) {
            this(x, y, width, height, value, onPress, false);
        }

        private PaintedButton(int x, int y, int width, int height, Component value, Consumer<Boolean> onPress, boolean toggle) {
            super(x, y, width, height, value);
            this.value = value;
            this.onPress = onPress;
            this.toggle = toggle;
        }

        private void setValue(boolean enabled) {
            setLabel(toggleText(enabled));
        }

        private void setLabel(Component label) {
            value = label;
            setMessage(label);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean enabled = value.getString().equalsIgnoreCase(Component.translatable("config.combat_evolution.on").getString());
            boolean hovered = isHoveredOrFocused();
            int fill = toggle
                    ? (enabled ? (hovered ? 0xFF267E9B : 0xFF1D6178) : (hovered ? 0xFF354457 : 0xFF273342))
                    : (hovered ? 0xFF354457 : 0xFF273342);
            int border = toggle && enabled ? 0xFF64D2FF : 0xFF46576B;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
            graphics.renderOutline(getX(), getY(), width, height, border);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(value, width - 10);
            if (!lines.isEmpty()) {
                graphics.drawCenteredString(font, lines.get(0), getX() + width / 2,
                        getY() + (height - font.lineHeight) / 2, 0xFFF4F7FA);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            boolean current = value.getString().equalsIgnoreCase(Component.translatable("config.combat_evolution.on").getString());
            if (toggle) {
                onPress.accept(!current);
            } else {
                onPress.accept(false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
