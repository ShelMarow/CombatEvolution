package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
abstract class CEBehaviorLayoutScreen extends Screen {
    protected final Screen parent;
    protected int left, top, panelWidth, bottom, viewportTop, viewportBottom, innerX, innerWidth;
    private final List<PlacedWidget> contentWidgets = new ArrayList<>();
    private final List<TextLine> textLines = new ArrayList<>();
    private final List<Border> borders = new ArrayList<>();
    private final List<Border> sections = new ArrayList<>();
    private final List<Integer> headerLines = new ArrayList<>();
    private int openHeading = -1;
    private final List<FoldHeader> foldHeaders = new ArrayList<>();
    private final List<CEDatapackButton> fixedButtons = new ArrayList<>();
    private int contentHeight, scroll;
    private boolean draggingScrollbar;
    private int scrollbarDragOffset;
    private Component tooltipTitle, tooltipText;
    private CEDatapackButton tooltipButton;
    private double tooltipX, tooltipY;
    private float tooltipAlpha;
    private long tooltipLastUpdate;
    private boolean tooltipPositionInitialized;

    CEBehaviorLayoutScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected final void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(740, width - 16));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);
        innerX = left + Math.min(18, Math.max(8, panelWidth / 24));
        innerWidth = Math.max(1, panelWidth - (innerX - left) * 2 - 22 - sidebarWidth());
        viewportTop = top + 37;
        viewportBottom = Math.max(viewportTop, bottom - 38);
        contentWidgets.clear();
        textLines.clear();
        borders.clear();
        sections.clear();
        headerLines.clear();
        openHeading = -1;
        foldHeaders.clear();
        fixedButtons.clear();
        int endY = buildContent(viewportTop + 7);
        closeHeading(endY);
        contentHeight = Math.max(0, endY - viewportTop + 5);
        boolean hasDone = this instanceof CEBehaviorNodeScreen || this instanceof CEBehaviorRootScreen;
        int backWidth = Math.min(110, Math.max(1, hasDone ? (panelWidth - 40) / 2 : panelWidth - 32));
        int right = left + panelWidth - 16;
        CEDatapackButton back = new CEDatapackButton(right - backWidth - (hasDone ? backWidth + 8 : 0), bottom - 28,
                backWidth, 20, Component.translatable("config.combat_evolution.back"), false, this::onClose);
        fixedButtons.add(back);
        addRenderableWidget(back);
        if (hasDone) {
            Runnable finish = this instanceof CEBehaviorNodeScreen nodeScreen ? nodeScreen::returnToRoot : this::onClose;
            CEDatapackButton done = new CEDatapackButton(right - backWidth, bottom - 28, backWidth, 20,
                    Component.translatable("config.combat_evolution.datapack_behavior_done"), true,
                    finish);
            fixedButtons.add(done);
            addRenderableWidget(done);
        }
        initSidebarControls();
        updateScroll();
    }

    protected abstract int buildContent(int y);

    protected int sidebarWidth() { return 0; }

    protected List<String> hierarchyPath() { return List.of(); }

    protected void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) { }

    protected void initSidebarControls() { }

    protected void addFixedWidget(CEDatapackButton button) {
        fixedButtons.add(button);
        addRenderableWidget(button);
    }

    protected void addText(Component text, int x, int y, int color) {
        textLines.add(new TextLine(text, x, y, color));
    }

    protected int addHeading(Component title, int y) {
        closeHeading(y - 3);
        addText(Component.literal("|  " + font.plainSubstrByWidth(title.getString(), Math.max(1, innerWidth - 20))),
                innerX + 3, y + 3, 0xFF8BE2FF);
        headerLines.add(y);
        openHeading = y + 21;
        return y + 23;
    }

    protected int addCategoryGap(int y) {
        closeHeading(y);
        return y + 22;
    }

    private void closeHeading(int y) {
        if (openHeading >= 0 && y > openHeading) sections.add(new Border(openHeading, y, false));
        openHeading = -1;
    }

    protected int addFoldHeader(Component title, boolean collapsed, Runnable toggle, int y) {
        closeHeading(y - 3);
        int headerY = y;
        CEDatapackButton button = new CEDatapackButton(innerX, y, innerWidth, 20,
                Component.literal(collapsed ? ">  " : "v  ").append(title), false, () -> {
            toggle.run();
            if (collapsed) {
                scroll = Math.max(scroll, headerY - viewportTop - 14);
                updateScroll();
            }
        });
        button.setSectionHeader(true);
        button.setCustomTooltip(Component.translatable("config.combat_evolution.datapack_section_tooltip."
                + (collapsed ? "expand" : "collapse")));
        addContentWidget(button);
        headerLines.add(y);
        foldHeaders.add(new FoldHeader(button, title));
        return y + 24;
    }

    protected void addBorder(int topY, int bottomY) {
        if (bottomY > topY) borders.add(new Border(topY, bottomY, false));
    }

    protected void addItemBorder(int topY, int bottomY) {
        if (bottomY > topY) borders.add(new Border(topY, bottomY, true));
    }

    protected int addFields(JsonObject target, List<Field> fields, int y) {
        return addFields(target, fields, y, innerX, innerWidth);
    }

    protected int addInsetFields(JsonObject target, List<Field> fields, int y) {
        return addFields(target, fields, y, innerX + 12, Math.max(1, innerWidth - 24), 254);
    }

    private int addFields(JsonObject target, List<Field> fields, int y, int startX, int availableWidth) {
        return addFields(target, fields, y, startX, availableWidth, 390);
    }

    private int addFields(JsonObject target, List<Field> fields, int y, int startX, int availableWidth,
                          int twoColumnWidth) {
        int columns = availableWidth >= twoColumnWidth ? 2 : 1;
        int gap = 10;
        int cellWidth = columns == 2 ? (availableWidth - gap) / 2 : availableWidth;
        for (int index = 0; index < fields.size(); index += columns) {
            for (int column = 0; column < columns && index + column < fields.size(); column++) {
                Field field = fields.get(index + column);
                int x = startX + column * (cellWidth + gap);
                addField(target, field, x, y, cellWidth);
            }
            y += 42;
        }
        return y;
    }

    protected int addFullWidthField(JsonObject target, Field field, int y) {
        addField(target, field, innerX, y, innerWidth);
        return y + 42;
    }

    protected int addInsetFullWidthField(JsonObject target, Field field, int y) {
        addField(target, field, innerX + 12, y, Math.max(1, innerWidth - 24));
        return y + 42;
    }

    protected int addNumberPair(JsonObject target, String key, int y) {
        JsonArray values = array(target, key);
        while (values.size() < 2) values.add(values.size() == 0 ? 0 : 1);
        int gap = 10;
        int cellWidth = Math.max(1, (innerWidth - gap) / 2);
        for (int index = 0; index < 2; index++) {
            int position = index;
            int x = innerX + index * (cellWidth + gap);
            Component label = Component.translatable("config.combat_evolution.datapack_field."
                    + key + "_" + index);
            addText(label, x + 2, y + 2, 0xFFCBD6E2);
            EditBox input = new EditBox(font, x, y + 17, cellWidth, 20, label);
            input.setValue(values.get(index).getAsString());
            input.setResponder(raw -> {
                try {
                    double parsed = Double.parseDouble(raw.trim());
                    if (Double.isFinite(parsed)) values.set(position, new JsonPrimitive(parsed));
                } catch (NumberFormatException ignored) { }
            });
            addContentWidget(input);
        }
        return y + 42;
    }

    private void addField(JsonObject target, Field field, int x, int y, int fieldWidth) {
        String labelKey = "config.combat_evolution.datapack_field." + field.key.replace('.', '_');
        Component label = I18n.exists(labelKey) ? Component.translatable(labelKey) : Component.literal(field.key);
        addText(Component.literal(font.plainSubstrByWidth(label.getString(), Math.max(1, fieldWidth - 4))),
                x + 2, y + 2, 0xFFCBD6E2);
        String current = readValue(target, field);
        if (field.kind == Kind.BOOLEAN) {
            CEDatapackButton button = new CEDatapackButton(x, y + 17, fieldWidth, 20,
                    booleanLabel(current), false, () -> {});
            button.setAction(() -> {
                boolean next = !Boolean.parseBoolean(readValue(target, field));
                writeValue(target, field, Boolean.toString(next));
                button.setMessage(booleanLabel(Boolean.toString(next)));
                if (field.key.equals("canInsertGlobalBehavior")) minecraft.setScreen(this);
            });
            addContentWidget(button);
        } else if (field.kind == Kind.SELECT || field.kind == Kind.CYCLE) {
            CEDatapackButton button = new CEDatapackButton(x, y + 17, fieldWidth, 20,
                    optionLabel(field, current), false, () -> {});
            button.setAction(() -> {
                if (field.kind == Kind.CYCLE) {
                    int next = (field.options.indexOf(readValue(target, field)) + 1) % field.options.size();
                    String chosen = field.options.get(next);
                    writeValue(target, field, chosen);
                    button.setMessage(optionLabel(field, chosen));
                    if (field.key.equals("interruptType") || field.key.equals("counterType")
                            || field.key.equals("type")) minecraft.setScreen(this);
                } else minecraft.setScreen(new CEDatapackOptionSelectionScreen(this, label,
                        field.options, current, chosen -> {
                    writeValue(target, field, chosen);
                    button.setMessage(optionLabel(field, chosen));
                }));
            });
            addContentWidget(button);
        } else {
            EditBox input = new EditBox(font, x, y + 17, fieldWidth, 20, label);
            input.setMaxLength(512);
            input.setValue(current);
            input.setResponder(value -> writeValue(target, field, value));
            addContentWidget(input);
        }
    }

    private Component optionLabel(Field field, String value) {
        String key = "config.combat_evolution.datapack_option." + field.key + "." + value;
        return field.kind == Kind.CYCLE && I18n.exists(key) ? Component.translatable(key) : Component.literal(value);
    }

    protected int addAction(Component title, int y, int buttonWidth, boolean primary, Runnable action) {
        CEDatapackButton button = new CEDatapackButton(innerX, y, Math.min(innerWidth, buttonWidth), 22,
                title, primary, action);
        addContentWidget(button);
        return y + 28;
    }

    protected int addChildren(JsonArray children, int y) {
        int removeWidth = Math.min(28, innerWidth);
        int editWidth = Math.max(1, innerWidth - removeWidth - 4);
        for (int index = 0; index < children.size(); index++) {
            if (!children.get(index).isJsonObject()) continue;
            JsonObject child = children.get(index).getAsJsonObject();
            String type = child.has("behaviorType") ? child.get("behaviorType").getAsString() :
                    child.has("animation") ? "animation" : child.has("wanderTime") ? "wander" : "defense";
            String name = child.has("behaviorName") ? child.get("behaviorName").getAsString() : type;
            Component rowTitle = Component.translatable("config.combat_evolution.datapack_behavior_type." + type)
                    .append("  /  " + name);
            CEDatapackButton edit = new CEDatapackButton(innerX, y, editWidth, 22, rowTitle, false,
                    () -> minecraft.setScreen(new CEBehaviorNodeScreen(this, child, hierarchyPath())));
            CEDatapackButton remove = new CEDatapackButton(innerX + innerWidth - removeWidth, y, removeWidth,
                    22, Component.literal("x"), false, () -> {
                children.remove(child);
                minecraft.setScreen(this);
            });
            addContentWidget(edit);
            addContentWidget(remove);
            y += 27;
        }
        CEDatapackButton add = new CEDatapackButton(innerX, y, Math.min(180, innerWidth), 22,
                Component.translatable("config.combat_evolution.datapack_add_child"), true, () -> {
                JsonObject child = new JsonObject();
                child.addProperty("behaviorType", "animation");
                child.addProperty("behaviorName", "behavior_" + (children.size() + 1));
                child.addProperty("animation", "epicfight:biped/combat/longsword_auto1");
                children.add(child);
                minecraft.setScreen(new CEBehaviorNodeScreen(this, child, hierarchyPath()));
        });
        addContentWidget(add);
        return y + 30;
    }

    protected void addContentWidget(AbstractWidget widget) {
        contentWidgets.add(new PlacedWidget(widget, widget.getY()));
        addRenderableWidget(widget);
    }

    protected static JsonArray array(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) object.add(key, new JsonArray());
        return object.getAsJsonArray(key);
    }

    protected static String readValue(JsonObject root, Field field) {
        JsonElement value = path(root, field.key);
        if (value == null || value.isJsonNull()) return field.defaultValue;
        if (value.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonElement element : value.getAsJsonArray()) if (element.isJsonPrimitive()) parts.add(element.getAsString());
            return String.join(", ", parts);
        }
        return value.isJsonPrimitive() ? value.getAsString() : field.defaultValue;
    }

    private static void writeValue(JsonObject root, Field field, String raw) {
        String value = raw.trim();
        if (field.key.equals("interruptType") && value.equalsIgnoreCase("NONE")) {
            removePath(root, field.key);
            removePath(root, "interruptedWindow");
            return;
        }
        if (value.isEmpty()) {
            removePath(root, field.key);
            return;
        }
        try {
            JsonElement parsed = switch (field.kind) {
                case STRING, SELECT, CYCLE -> new JsonPrimitive(value);
                case BOOLEAN -> new JsonPrimitive(Boolean.parseBoolean(value));
                case INTEGER -> new JsonPrimitive(Integer.parseInt(value));
                case DECIMAL -> {
                    double number = Double.parseDouble(value);
                    yield Double.isFinite(number) ? new JsonPrimitive(number) : null;
                }
                case STRING_LIST, NUMBER_LIST -> {
                    JsonArray values = new JsonArray();
                    for (String part : value.split(",")) {
                        String item = part.trim();
                        if (item.isEmpty()) continue;
                        if (field.kind == Kind.STRING_LIST) values.add(item);
                        else {
                            double number = Double.parseDouble(item);
                            if (!Double.isFinite(number)) throw new NumberFormatException();
                            values.add(number);
                        }
                    }
                    yield values;
                }
            };
            if (parsed != null) {
                setPath(root, field.key, parsed);
                if (field.key.equals("type")) {
                    if (value.equals("TICK")) {
                        removePath(root, "timeStart");
                        removePath(root, "timeEnd");
                    } else if (value.equals("IN_TIME")) {
                        if (path(root, "timeStart") == null && path(root, "timeEnd") != null)
                            setPath(root, "timeStart", path(root, "timeEnd").deepCopy());
                        if (path(root, "timeStart") == null) setPath(root, "timeStart", new JsonPrimitive(0));
                        removePath(root, "timeEnd");
                    } else if (value.equals("BETWEEN_TIMES")) {
                        if (path(root, "timeStart") == null) setPath(root, "timeStart", new JsonPrimitive(0));
                        if (path(root, "timeEnd") == null)
                            setPath(root, "timeEnd", path(root, "timeStart").deepCopy());
                    }
                }
                if (field.key.equals("counterType")) {
                    if (!value.equals("RANDOM")) removePath(root, "counterChance");
                    if (!value.equals("END")) removePath(root, "maxGuardHit");
                }
                if (field.key.equals("interruptType") && path(root, "interruptedWindow") == null) {
                    JsonArray window = new JsonArray();
                    window.add(value.equalsIgnoreCase("TIME") ? 0.0 : 1.0);
                    if (value.equalsIgnoreCase("TIME")) window.add(1.0);
                    setPath(root, "interruptedWindow", window);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static JsonElement path(JsonObject root, String key) {
        String[] parts = key.split("\\.");
        JsonObject current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            if (!current.has(parts[index]) || !current.get(parts[index]).isJsonObject()) return null;
            current = current.getAsJsonObject(parts[index]);
        }
        return current.get(parts[parts.length - 1]);
    }

    private static void setPath(JsonObject root, String key, JsonElement value) {
        String[] parts = key.split("\\.");
        JsonObject current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            if (!current.has(parts[index]) || !current.get(parts[index]).isJsonObject()) current.add(parts[index], new JsonObject());
            current = current.getAsJsonObject(parts[index]);
        }
        current.add(parts[parts.length - 1], value);
    }

    private static void removePath(JsonObject root, String key) {
        String[] parts = key.split("\\.");
        JsonObject current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            if (!current.has(parts[index]) || !current.get(parts[index]).isJsonObject()) return;
            current = current.getAsJsonObject(parts[index]);
        }
        current.remove(parts[parts.length - 1]);
    }

    private Component booleanLabel(String value) {
        return Boolean.parseBoolean(value) ? Component.translatable("config.combat_evolution.on")
                : Component.translatable("config.combat_evolution.off");
    }

    private void updateScroll() {
        int viewHeight = viewportBottom - viewportTop;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewHeight)));
        for (PlacedWidget placed : contentWidgets) {
            int y = placed.y - scroll;
            placed.widget.setY(y);
            placed.widget.visible = y + placed.widget.getHeight() > viewportTop && y < viewportBottom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            scroll -= (int) Math.signum(delta) * 18;
            updateScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (CEDatapackButton fixed : fixedButtons) {
                if (mouseX >= fixed.getX() && mouseX < fixed.getX() + fixed.getWidth()
                        && mouseY >= fixed.getY() && mouseY < fixed.getY() + fixed.getHeight()) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        if (button == 0 && hasScrollbar() && mouseX >= scrollbarX() - 2 && mouseX <= scrollbarX() + 4
                && mouseY >= viewportTop && mouseY < viewportBottom) {
            scrollbarDragOffset = Math.max(0, Math.min(thumbHeight(), (int) mouseY - thumbTop()));
            draggingScrollbar = true;
            dragScrollbar(mouseY);
            return true;
        }
        if (button == 0 && (mouseY < viewportTop || mouseY >= viewportBottom)) {
            for (PlacedWidget placed : contentWidgets) {
                AbstractWidget widget = placed.widget;
                if (widget.visible && mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            dragScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean hasScrollbar() {
        return contentHeight > viewportBottom - viewportTop && viewportBottom > viewportTop;
    }

    private int scrollbarX() {
        return sidebarWidth() > 0 ? innerX + innerWidth + 6 : left + panelWidth - 10;
    }

    private int thumbHeight() {
        int track = viewportBottom - viewportTop;
        return Math.min(track, Math.max(20, track * track / Math.max(1, contentHeight)));
    }

    private int thumbTop() {
        int travel = viewportBottom - viewportTop - thumbHeight();
        int max = Math.max(1, contentHeight - (viewportBottom - viewportTop));
        return viewportTop + travel * scroll / max;
    }

    private void dragScrollbar(double mouseY) {
        int travel = viewportBottom - viewportTop - thumbHeight();
        if (travel <= 0) return;
        int at = Math.max(0, Math.min(travel, (int) mouseY - scrollbarDragOffset - viewportTop));
        scroll = at * (contentHeight - (viewportBottom - viewportTop)) / travel;
        updateScroll();
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
        graphics.enableScissor(left + 7, viewportTop, left + panelWidth - 7, viewportBottom);
        for (int headerY : headerLines) {
            int y = headerY - scroll;
            if (y + 20 <= viewportTop || y >= viewportBottom) continue;
            graphics.fill(innerX - 5, y, innerX + innerWidth + 5, y + 20, 0xFF223342);
            graphics.fill(innerX - 5, y, innerX - 2, y + 20, 0xFF64D2FF);
        }
        for (Border border : sections) {
            int y1 = border.top - scroll;
            int y2 = border.bottom - scroll;
            if (y2 <= viewportTop || y1 >= viewportBottom) continue;
            graphics.fill(innerX - 5, y1, innerX + innerWidth + 5, y2, 0xFF17232F);
            graphics.fill(innerX - 5, y1, innerX + innerWidth + 5, y1 + 1, 0xFF405165);
            graphics.fill(innerX - 5, y2 - 1, innerX + innerWidth + 5, y2, 0xFF405165);
            graphics.fill(innerX - 5, y1, innerX - 4, y2, 0xFF405165);
            graphics.fill(innerX + innerWidth + 4, y1, innerX + innerWidth + 5, y2, 0xFF405165);
        }
        List<Border> orderedBorders = new ArrayList<>(borders);
        orderedBorders.sort((first, second) -> Integer.compare(second.bottom - second.top, first.bottom - first.top));
        for (Border border : orderedBorders) {
            int y1 = border.top - scroll;
            int y2 = border.bottom - scroll;
            if (y2 <= viewportTop || y1 >= viewportBottom) continue;
            int borderLeft = border.item ? innerX + 4 : innerX - 5;
            int borderRight = border.item ? innerX + innerWidth - 4 : innerX + innerWidth + 5;
            graphics.fill(borderLeft + 1, y1 + 1, borderRight - 1, y2,
                    border.item ? 0xFF283A49 : 0xFF1B2835);
            graphics.fill(borderLeft, y1, borderRight, y1 + 1, 0xFF405165);
            graphics.fill(borderLeft, y2, borderRight, y2 + 1, 0xFF405165);
            graphics.fill(borderLeft, y1, borderLeft + 1, y2 + 1, 0xFF405165);
            graphics.fill(borderRight - 1, y1, borderRight, y2 + 1, 0xFF405165);
        }
        for (int headerY : headerLines) {
            int y = headerY - scroll + 18;
            if (y >= viewportTop && y < viewportBottom) {
                graphics.fill(innerX + 10, y, innerX + innerWidth - 4, y + 1, 0xFF405165);
            }
        }
        for (TextLine line : textLines) {
            int y = line.y - scroll;
            if (y + 9 >= viewportTop && y < viewportBottom) {
                graphics.drawString(font, line.text, line.x, y, line.color);
            }
        }
        List<Boolean> visibility = new ArrayList<>(fixedButtons.size());
        for (CEDatapackButton button : fixedButtons) {
            visibility.add(button.visible);
            button.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        CEDatapackViewport.drawDividers(graphics, left + 10, left + panelWidth - 10, viewportTop, viewportBottom);
        renderSidebar(graphics, mouseX, mouseY);
        for (int index = 0; index < fixedButtons.size(); index++) {
            CEDatapackButton button = fixedButtons.get(index);
            button.visible = visibility.get(index);
            if (button.visible) button.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.fill(left + 10, bottom - 37, left + panelWidth - 10, bottom - 36, 0xFF46576B);
        if (hasScrollbar()) {
            int x = scrollbarX();
            graphics.fill(x, viewportTop, x + 4, viewportBottom, 0xFF1B2530);
            graphics.fill(x, thumbTop(), x + 4, thumbTop() + thumbHeight(), 0xFF71869A);
        }
        drawFoldTooltip(graphics, mouseX, mouseY);
    }

    private void drawFoldTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        FoldHeader hovered = null;
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            for (FoldHeader header : foldHeaders) {
                CEDatapackButton button = header.button;
                if (button.visible && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight()) {
                    hovered = header;
                    break;
                }
            }
        }
        long now = System.nanoTime() / 1_000_000L;
        long elapsed = tooltipLastUpdate == 0 ? 16 : Math.min(50, Math.max(0, now - tooltipLastUpdate));
        tooltipLastUpdate = now;
        if (hovered != null) {
            if (tooltipButton != hovered.button) {
                tooltipButton = hovered.button;
                tooltipTitle = hovered.title;
                tooltipText = hovered.button.getCustomTooltip();
            }
            int textWidth = Math.max(font.width(tooltipTitle), font.width(tooltipText));
            int boxWidth = Math.max(1, Math.min(width - 8, textWidth + 20));
            int targetX = mouseX + 14;
            if (targetX + boxWidth > width - 4) targetX = mouseX - boxWidth - 14;
            targetX = Math.max(4, Math.min(targetX, width - boxWidth - 4));
            int targetY = mouseY + 14;
            if (targetY + 36 > height - 4) targetY = mouseY - 46;
            targetY = Math.max(4, Math.min(targetY, height - 40));
            float movement = 1.0F - (float) Math.exp(-elapsed / 85.0F);
            if (!tooltipPositionInitialized) {
                tooltipX = targetX;
                tooltipY = targetY;
                tooltipPositionInitialized = true;
            } else {
                tooltipX += (targetX - tooltipX) * movement;
                tooltipY += (targetY - tooltipY) * movement;
            }
            tooltipAlpha = Math.min(1.0F, tooltipAlpha + elapsed / 110.0F);
        } else {
            tooltipAlpha = Math.max(0.0F, tooltipAlpha - elapsed / 110.0F);
        }
        if (tooltipAlpha <= 0 || tooltipTitle == null || tooltipText == null) return;
        int textWidth = Math.max(font.width(tooltipTitle), font.width(tooltipText));
        int boxWidth = Math.max(1, Math.min(width - 8, textWidth + 20));
        int x = Math.max(4, Math.min((int) tooltipX, width - boxWidth - 4));
        int y = Math.max(4, Math.min((int) tooltipY, height - 40));
        int alpha = Math.round(tooltipAlpha * 255);
        graphics.pose().pushPose();
        graphics.pose().translate(0, Math.round((1.0F - tooltipAlpha) * 3), 400);
        graphics.fill(x, y, x + boxWidth, y + 36, fadeColor(0xF0161D27, alpha));
        graphics.fill(x, y, x + boxWidth, y + 1, fadeColor(0xFF64D2FF, alpha));
        graphics.fill(x, y, x + 2, y + 36, fadeColor(0xFF64D2FF, alpha));
        graphics.drawString(font, font.plainSubstrByWidth(tooltipTitle.getString(), Math.max(1, boxWidth - 20)),
                x + 10, y + 5, fadeColor(0xFF8BE2FF, alpha));
        graphics.drawString(font, font.plainSubstrByWidth(tooltipText.getString(), Math.max(1, boxWidth - 20)),
                x + 10, y + 18, fadeColor(0xFFEAF7FC, alpha));
        graphics.pose().popPose();
    }

    private static int fadeColor(int color, int alpha) {
        return ((color >>> 24) * alpha / 255 << 24) | (color & 0x00FFFFFF);
    }

    protected enum Kind { STRING, INTEGER, DECIMAL, BOOLEAN, SELECT, CYCLE, STRING_LIST, NUMBER_LIST }

    protected record Field(String key, Kind kind, String defaultValue, List<String> options) {
        static Field text(String key, String value) { return new Field(key, Kind.STRING, value, List.of()); }
        static Field integer(String key, int value) { return new Field(key, Kind.INTEGER, Integer.toString(value), List.of()); }
        static Field decimal(String key, double value) { return new Field(key, Kind.DECIMAL, Double.toString(value), List.of()); }
        static Field bool(String key, boolean value) { return new Field(key, Kind.BOOLEAN, Boolean.toString(value), List.of()); }
        static Field select(String key, String value, String... options) { return new Field(key, Kind.SELECT, value, List.of(options)); }
        static Field cycle(String key, String value, String... options) { return new Field(key, Kind.CYCLE, value, List.of(options)); }
        static Field stringList(String key) { return new Field(key, Kind.STRING_LIST, "", List.of()); }
        static Field numberList(String key) { return new Field(key, Kind.NUMBER_LIST, "", List.of()); }
    }

    private record PlacedWidget(AbstractWidget widget, int y) { }
    private record TextLine(Component text, int x, int y, int color) { }
    private record Border(int top, int bottom, boolean item) { }
    private record FoldHeader(CEDatapackButton button, Component title) { }
}
