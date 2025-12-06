package net.shelmarow.combat_evolution.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.client.execution.HUDTypeManager;
import net.shelmarow.combat_evolution.config.ClientConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class CombatEvolutionConfigScreen extends Screen {
    private final Screen parent;
    private EditBox inputBox;
    private Button textDisplayButton;
    private boolean showTextDisplayState;

    private final List<String> allHUDTypes = new ArrayList<>();
    private final List<String> suggestions = new ArrayList<>();

    private int selectedSuggestion = -1;
    private int suggestionBoxX;
    private int suggestionBoxY;
    private int suggestionBoxW;
    private final int suggestionLineHeight = 12;

    public CombatEvolutionConfigScreen(Screen parent) {
        super(Component.literal("Combat Evolution Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // 加载所有HUD类型名称
        allHUDTypes.clear();
        allHUDTypes.addAll(HUDTypeManager.getAllHUDTypes().stream().map(ResourceLocation::toString).distinct().toList());


        inputBox = new EditBox(this.font, width / 2 - 100, 80, 200, 20, Component.literal("HUD Type"));
        inputBox.setValue(ClientConfig.HUD_TYPE.get());
        inputBox.setResponder(this::updateSuggestions);
        addRenderableWidget(inputBox);

        showTextDisplayState = ClientConfig.SHOW_TEXT_DISPLAY.get();
        textDisplayButton = Button.builder(Component.literal(showTextDisplayState ? "on" : "off"), b -> {
                    showTextDisplayState = !showTextDisplayState;
                    textDisplayButton.setMessage(Component.literal(showTextDisplayState ? "on" : "off"));
                })
                .bounds(width / 2 - 100, 120, 200, 20)
                .build();
        addRenderableWidget(textDisplayButton);

        Button saveButton = Button.builder(Component.literal("Save"), b -> saveAndClose())
                .bounds(width / 2 - 40, height - 40, 80, 20).build();

        addRenderableWidget(saveButton);
    }

    private void saveAndClose() {
        String selected = inputBox.getValue();
        // 校验输入是否在可用列表中，不合法则回退到默认或第一个可用值
        String valueToSave;
        if (selected.isEmpty() || !allHUDTypes.contains(selected)) {
            String defaultValue = "combat_evolution:default";
            valueToSave = !allHUDTypes.isEmpty() && allHUDTypes.contains(defaultValue)
                    ? defaultValue
                    : (!allHUDTypes.isEmpty() ? allHUDTypes.get(0) : defaultValue);
            inputBox.setValue(valueToSave);
        } else {
            valueToSave = selected;
        }
        ClientConfig.HUD_TYPE.set(valueToSave);
        ClientConfig.SHOW_TEXT_DISPLAY.set(showTextDisplayState);
        ClientConfig.CLIENT_SPEC.save();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void updateSuggestions(String currentText) {
        suggestions.clear();
        if (currentText.isEmpty()) return;

        String lower = currentText.toLowerCase(Locale.ROOT);
        for (String type : allHUDTypes) {
            if (type.toLowerCase(Locale.ROOT).contains(lower)) {
                suggestions.add(type);
            }
        }

        if (suggestions.size() > 8)
            suggestions.subList(8, suggestions.size()).clear();

        selectedSuggestion = -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 键盘上下选择建议
        if (!suggestions.isEmpty()) {
            switch (keyCode) {
                case 264 -> { // 下键
                    selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
                    return true;
                }
                case 265 -> { // 上键
                    selectedSuggestion = (selectedSuggestion - 1 + suggestions.size()) % suggestions.size();
                    return true;
                }
                case 257, 335 -> { // 回车键
                    if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                        inputBox.setValue(suggestions.get(selectedSuggestion));
                        suggestions.clear();
                        selectedSuggestion = -1;
                        return true;
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers) || inputBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!suggestions.isEmpty()) {
            // 根据鼠标位置计算悬停索引
            int x = suggestionBoxX;
            int y = suggestionBoxY;
            int w = suggestionBoxW;
            int h = suggestionLineHeight * suggestions.size() + 4;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int relativeY = (int)mouseY - y - 2;
                int idx = relativeY / suggestionLineHeight;
                if (idx >= 0 && idx < suggestions.size()) {
                    selectedSuggestion = idx;
                }
            } else {
                selectedSuggestion = -1;
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !suggestions.isEmpty()) {
            int x = suggestionBoxX;
            int y = suggestionBoxY;
            int w = suggestionBoxW;
            int h = suggestionLineHeight * suggestions.size() + 4;
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int relativeY = (int)mouseY - y - 2;
                int idx = relativeY / suggestionLineHeight;
                if (idx >= 0 && idx < suggestions.size()) {
                    inputBox.setValue(suggestions.get(idx));
                    suggestions.clear();
                    selectedSuggestion = -1;
                    return true;
                }
            }
        }
        //点击其他区域直接关闭
        suggestions.clear();
        selectedSuggestion = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return inputBox.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.drawCenteredString(font, "Combat Evolution Config", width / 2, 30, 0xFFFFFF);
        String hudType = "HUD Type:";
        guiGraphics.drawString(font, hudType, inputBox.getX() - font.width(hudType) - 10, inputBox.getY() + 6, 0xA0A0A0);
        String showTip = "Text Display:";
        guiGraphics.drawString(font, showTip, textDisplayButton.getX() - font.width(showTip) - 10, textDisplayButton.getY() + 6, 0xA0A0A0);

        // 渲染建议框
        if (!suggestions.isEmpty()) {
            int x = inputBox.getX();
            int y = inputBox.getY() + inputBox.getHeight() + 2;
            int w = inputBox.getWidth();
            int lineHeight = suggestionLineHeight;
            // 保存用于鼠标交互的区域数据
            suggestionBoxX = x;
            suggestionBoxY = y;
            suggestionBoxW = w;

            RenderSystem.enableBlend();
            guiGraphics.fill(x, y, x + w, y + lineHeight * suggestions.size() + 4, 0xC0101010);

            for (int i = 0; i < suggestions.size(); i++) {
                int color = (i == selectedSuggestion) ? 0xFFFFA0 : 0xFFFFFF;
                guiGraphics.drawString(font, suggestions.get(i), x + 4, y + 2 + i * lineHeight, color);
            }
            RenderSystem.disableBlend();
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
