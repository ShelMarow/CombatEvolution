package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
class CEDatapackFormScreen extends Screen {
    private final Screen parent;
    private final JsonObject target;
    private final List<Field> fields;
    private final List<Row> rows = new ArrayList<>();
    private final List<CEDatapackButton> fixedButtons = new ArrayList<>();
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelBottom;
    private int viewportTop;
    private int viewportBottom;
    private int footerTop;
    private int rowHeight;
    private int scroll;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();
    private boolean compact;
    private Component error = Component.empty();

    CEDatapackFormScreen(Screen parent, JsonObject target, Component title, List<Field> fields) {
        super(title);
        this.parent = parent;
        this.target = target;
        this.fields = fields;
    }

    @Override
    protected void init() {
        for (Row row : rows) if (row.input != null) row.field.value = row.input.getValue().trim();
        super.init();
        panelWidth = Math.max(1, Math.min(700, width - 16));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(10, Math.max(0, height / 10));
        panelBottom = Math.max(panelTop + 1, height - 10);
        footerTop = Math.max(panelTop, panelBottom - Math.min(38, Math.max(26, height / 7)));
        int innerX = panelLeft + Math.min(20, Math.max(6, panelWidth / 18));
        int innerWidth = Math.max(1, panelWidth - (innerX - panelLeft) * 2);
        compact = innerWidth < 430;
        rowHeight = compact ? 43 : 32;
        viewportTop = panelTop + 43;
        viewportBottom = Math.max(viewportTop, footerTop - 8);
        int inputWidth = compact ? innerWidth : Math.max(100, innerWidth * 43 / 100);
        int labelWidth = compact ? innerWidth : innerWidth - inputWidth - 12;
        int visibleRows = Math.max(1, (viewportBottom - viewportTop + 4) / rowHeight);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, fields.size() - visibleRows)));

        rows.clear();
        fixedButtons.clear();
        for (int index = 0; index < fields.size(); index++) {
            Field field = fields.get(index);
            int y = viewportTop + (index - scroll) * rowHeight;
            Component label = Component.translatable("config.combat_evolution.datapack_field." + field.key.replace('.', '_'));
            if (field.value == null) field.value = readValue(field);
            int fieldY = compact ? y + 19 : y + 4;
            int fieldX = compact ? innerX : innerX + labelWidth + 12;
            int controlWidth = compact ? innerWidth : inputWidth;
            if (field.type == Type.BOOLEAN) {
                CEDatapackButton button = new CEDatapackButton(fieldX, fieldY, controlWidth, 20, toggleLabel(field.value), false, () -> {});
                button.setAction(() -> {
                    field.value = Boolean.parseBoolean(field.value) ? "false" : "true";
                    button.setMessage(toggleLabel(field.value));
                });
                addRenderableWidget(button);
                rows.add(new Row(field, label, button, null, innerX, y));
            } else if (field.type == Type.SELECT) {
                CEDatapackButton button = new CEDatapackButton(fieldX, fieldY, controlWidth, 20,
                        Component.literal(field.value), false, () -> {});
                button.setAction(() -> minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        label, field.options, field.value, selected -> field.value = selected)));
                addRenderableWidget(button);
                rows.add(new Row(field, label, button, null, innerX, y));
            } else {
                EditBox input = new EditBox(font, fieldX, fieldY, controlWidth, 20, label);
                input.setValue(field.value);
                input.setMaxLength(512);
                addRenderableWidget(input);
                rows.add(new Row(field, label, input, input, innerX, y));
            }
        }
        positionRows();

        int buttonWidth = Math.min(106, Math.max(62, (panelWidth - 48) / 4));
        int buttonHeight = Math.min(20, Math.max(16, footerTop < panelTop + 20 ? 16 : panelBottom - footerTop - 8));
        int buttonY = footerTop + Math.max(2, (panelBottom - footerTop - buttonHeight) / 2);
        CEDatapackButton cancel = new CEDatapackButton(panelLeft + panelWidth - 16 - buttonWidth * 2 - 7, buttonY,
                buttonWidth, buttonHeight, Component.translatable("config.combat_evolution.cancel"), false, this::onClose);
        CEDatapackButton apply = new CEDatapackButton(panelLeft + panelWidth - 16 - buttonWidth, buttonY,
                buttonWidth, buttonHeight, Component.translatable("config.combat_evolution.apply"), true, this::apply);
        fixedButtons.add(cancel);
        fixedButtons.add(apply);
        addRenderableWidget(cancel);
        addRenderableWidget(apply);
    }

    private void positionRows() {
        for (Row row : rows) {
            int y = viewportTop + (fields.indexOf(row.field) - scroll) * rowHeight;
            int fieldY = compact ? y + 19 : y + 4;
            AbstractWidget widget = row.widget;
            widget.setY(fieldY);
            widget.visible = y + rowHeight > viewportTop && y < viewportBottom;
            row.y = y;
        }
    }

    private Component toggleLabel(String value) {
        return Boolean.parseBoolean(value) ? Component.translatable("config.combat_evolution.on")
                : Component.translatable("config.combat_evolution.off");
    }

    private String readValue(Field field) {
        JsonElement element = getPath(target, field.key);
        if (element != null && element.isJsonPrimitive()) return element.getAsString();
        if (element != null && element.isJsonArray()) {
            List<String> values = new ArrayList<>();
            for (JsonElement value : element.getAsJsonArray()) if (value.isJsonPrimitive()) values.add(value.getAsString());
            return String.join(", ", values);
        }
        return field.defaultValue;
    }

    private void apply() {
        JsonObject updated = target.deepCopy();
        for (Row row : rows) {
            Field field = row.field;
            if (row.input != null) field.value = row.input.getValue().trim();
            if (!writeValue(updated, field)) {
                error = Component.translatable("config.combat_evolution.datapack_invalid_field",
                        Component.translatable("config.combat_evolution.datapack_field." + field.key.replace('.', '_')));
                return;
            }
        }
        target.entrySet().clear();
        updated.entrySet().forEach(entry -> target.add(entry.getKey(), entry.getValue().deepCopy()));
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private boolean writeValue(JsonObject root, Field field) {
        String value = field.value == null ? "" : field.value.trim();
        if (value.isEmpty()) {
            removePath(root, field.key);
            return true;
        }
        try {
            JsonElement primitive = switch (field.type) {
                case BOOLEAN -> new JsonPrimitive(Boolean.parseBoolean(value));
                case INTEGER -> new JsonPrimitive(Integer.parseInt(value));
                case DECIMAL -> {
                    double number = Double.parseDouble(value);
                    if (!Double.isFinite(number)) yield null;
                    yield new JsonPrimitive(number);
                }
                case STRING, SELECT -> new JsonPrimitive(value);
                case STRING_LIST, INTEGER_LIST -> {
                    JsonArray array = new JsonArray();
                    for (String part : value.split(",")) {
                        String item = part.trim();
                        if (item.isEmpty()) continue;
                        if (field.type == Type.STRING_LIST) array.add(item);
                        else array.add(Integer.parseInt(item));
                    }
                    yield array;
                }
            };
            if (primitive == null) return false;
            setPath(root, field.key, primitive);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static JsonElement getPath(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) return null;
            current = current.getAsJsonObject(parts[i]);
        }
        return current.get(parts[parts.length - 1]);
    }

    private static void setPath(JsonObject root, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) current.add(parts[i], new JsonObject());
            current = current.getAsJsonObject(parts[i]);
        }
        current.add(parts[parts.length - 1], value);
    }

    private static void removePath(JsonObject root, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) return;
            current = current.getAsJsonObject(parts[i]);
        }
        current.remove(parts[parts.length - 1]);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            int visibleRows = Math.max(1, (viewportBottom - viewportTop + 4) / rowHeight);
            scroll = Math.max(0, Math.min(Math.max(0, fields.size() - visibleRows), scroll - (int) Math.signum(delta) * 2));
            positionRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, panelLeft + panelWidth - 10,
                viewportTop, viewportBottom, fields.size(), visibleRows(), scroll, value -> {
            scroll = value;
            positionRows();
        })) return true;
        if (button == 0) {
            for (CEDatapackButton fixed : fixedButtons) {
                if (mouseX >= fixed.getX() && mouseX < fixed.getX() + fixed.getWidth()
                        && mouseY >= fixed.getY() && mouseY < fixed.getY() + fixed.getHeight()) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        if (button == 0 && (mouseY < viewportTop || mouseY >= viewportBottom)) {
            for (Row row : rows) {
                AbstractWidget widget = row.widget;
                if (widget.visible && mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int visibleRows() {
        return Math.max(1, (viewportBottom - viewportTop + 4) / rowHeight);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.drag(mouseY, button, value -> {
            scroll = value;
            positionRows();
        })) return true;
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
        drawPanel(graphics);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 14, 0xFFF1F5F9);
        graphics.enableScissor(panelLeft + 6, viewportTop, panelLeft + panelWidth - 6, viewportBottom);
        for (Row row : rows) {
            if (row.y + rowHeight <= viewportTop || row.y >= viewportBottom) continue;
            int labelWidth = compact ? Math.max(1, panelWidth - 32) : Math.max(1, row.widget.getX() - row.labelX - 6);
            graphics.drawString(font, font.plainSubstrByWidth(row.label.getString(), labelWidth),
                    row.labelX, compact ? row.y + 4 : row.y + 10, 0xFFCBD6E2);
            graphics.fill(panelLeft + 12, row.y + rowHeight - 2, panelLeft + panelWidth - 12, row.y + rowHeight - 1, 0x403A4656);
        }
        for (CEDatapackButton button : fixedButtons) button.visible = false;
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        scrollbar.render(graphics, panelLeft + panelWidth - 10, viewportTop, viewportBottom,
                fields.size(), visibleRows(), scroll);
        CEDatapackViewport.drawDividers(graphics, panelLeft + 12, panelLeft + panelWidth - 12,
                viewportTop, viewportBottom);
        for (CEDatapackButton button : fixedButtons) {
            button.visible = true;
            button.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.fill(panelLeft + 12, footerTop, panelLeft + panelWidth - 12, footerTop + 1, 0xFF46576B);
        if (!error.getString().isEmpty()) graphics.drawString(font, error, panelLeft + 16, footerTop + 6, 0xFFFF8585);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xE6161D27);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF3A4656);
        graphics.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
    }

    enum Type {
        STRING,
        BOOLEAN,
        INTEGER,
        DECIMAL,
        SELECT,
        STRING_LIST,
        INTEGER_LIST
    }

    static final class Field {
        private final String key;
        private final Type type;
        private final String defaultValue;
        private final List<String> options;
        private String value;

        private Field(String key, Type type, String defaultValue, List<String> options) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.options = options;
        }

        static Field text(String key, String defaultValue) { return new Field(key, Type.STRING, defaultValue, List.of()); }
        static Field bool(String key, boolean defaultValue) { return new Field(key, Type.BOOLEAN, Boolean.toString(defaultValue), List.of()); }
        static Field integer(String key, int defaultValue) { return new Field(key, Type.INTEGER, Integer.toString(defaultValue), List.of()); }
        static Field decimal(String key, double defaultValue) { return new Field(key, Type.DECIMAL, Double.toString(defaultValue), List.of()); }
        static Field select(String key, String defaultValue, String... options) {
            return new Field(key, Type.SELECT, defaultValue, List.of(options));
        }
        static Field stringList(String key) { return new Field(key, Type.STRING_LIST, "", List.of()); }
        static Field integerList(String key) { return new Field(key, Type.INTEGER_LIST, "", List.of()); }
    }

    private static final class Row {
        private final Field field;
        private final Component label;
        private final AbstractWidget widget;
        private final EditBox input;
        private final int labelX;
        private int y;

        private Row(Field field, Component label, AbstractWidget widget, EditBox input, int labelX, int y) {
            this.field = field;
            this.label = label;
            this.widget = widget;
            this.input = input;
            this.labelX = labelX;
            this.y = y;
        }
    }
}
