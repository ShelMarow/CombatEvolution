package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
final class CEBehaviorGlobalSelectionScreen extends Screen {
    private final Screen parent;
    private final JsonObject node;
    private final JsonObject document;
    private final List<String> available = new ArrayList<>();
    private final CEDatapackScrollbar leftScrollbar = new CEDatapackScrollbar();
    private final CEDatapackScrollbar rightScrollbar = new CEDatapackScrollbar();
    private int left, top, panelWidth, bottom, listTop, listBottom, leftScroll, rightScroll;

    CEBehaviorGlobalSelectionScreen(Screen parent, JsonObject node, JsonObject document) {
        super(Component.translatable("config.combat_evolution.datapack_field.allowedGlobalNameList"));
        this.parent = parent;
        this.node = node;
        this.document = document;
        if (document != null && document.has("combatBehaviors") && document.get("combatBehaviors").isJsonArray()) {
            for (JsonElement group : document.getAsJsonArray("combatBehaviors")) {
                if (!group.isJsonObject() || !group.getAsJsonObject().has("behaviorRoots")) continue;
                for (JsonElement entry : group.getAsJsonObject().getAsJsonArray("behaviorRoots")) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject root = entry.getAsJsonObject();
                    if (root.has("rootName") && root.has("isGlobal") && root.get("isGlobal").getAsBoolean()) {
                        String name = root.get("rootName").getAsString();
                        if (!available.contains(name)) available.add(name);
                    }
                }
            }
        }
        available.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private JsonArray selected() {
        if (!node.has("allowedGlobalNameList") || !node.get("allowedGlobalNameList").isJsonArray())
            node.add("allowedGlobalNameList", new JsonArray());
        return node.getAsJsonArray("allowedGlobalNameList");
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(700, width - 12));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);
        listTop = top + 48;
        listBottom = Math.max(listTop, bottom - 38);
        addRenderableWidget(new CEDatapackButton(left + panelWidth - 106, bottom - 28, 94, 20,
                Component.translatable("config.combat_evolution.back"), false, this::onClose));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            boolean leftSide = mouseX < left + panelWidth / 2;
            int count = leftSide ? selected().size() : available.size();
            int max = Math.max(0, count - Math.max(1, (listBottom - listTop) / 23));
            if (leftSide) leftScroll = Math.max(0, Math.min(max, leftScroll - (int) Math.signum(delta) * 3));
            else rightScroll = Math.max(0, Math.min(max, rightScroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mid = left + panelWidth / 2;
        int visible = Math.max(1, (listBottom - listTop) / 23);
        if (leftScrollbar.press(mouseX, mouseY, button, mid - 10, listTop, listBottom,
                selected().size(), visible, leftScroll, value -> leftScroll = value)) return true;
        if (rightScrollbar.press(mouseX, mouseY, button, left + panelWidth - 12, listTop, listBottom,
                available.size(), visible, rightScroll, value -> rightScroll = value)) return true;
        if (button == 0 && mouseY >= listTop && mouseY < listBottom) {
            JsonArray selected = selected();
            if (mouseX < left + panelWidth / 2) {
                int index = leftScroll + ((int) mouseY - listTop) / 23;
                if (index >= 0 && index < selected.size()) {
                    selected.remove(index);
                    leftScroll = Math.min(leftScroll, Math.max(0,
                            selected.size() - Math.max(1, (listBottom - listTop) / 23)));
                }
            } else {
                int index = rightScroll + ((int) mouseY - listTop) / 23;
                if (index >= 0 && index < available.size()) {
                    String name = available.get(index);
                    boolean exists = false;
                    for (JsonElement value : selected) if (value.getAsString().equals(name)) exists = true;
                    if (!exists) selected.add(name);
                }
            }
            return true;
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

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        graphics.fill(left, top, left + panelWidth, bottom, 0xE6161D27);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFF1F5F9);
        int mid = left + panelWidth / 2;
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_global_selected"), left + 12, top + 32, 0xFF8BE2FF);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_global_available"), mid + 12, top + 32, 0xFF8BE2FF);
        graphics.fill(left + 8, listTop, mid - 2, listBottom, 0xFF101923);
        graphics.fill(mid + 2, listTop, left + panelWidth - 8, listBottom, 0xFF111B25);
        graphics.fill(mid, listTop, mid + 1, listBottom, 0xFF46576B);
        graphics.enableScissor(left + 8, listTop, left + panelWidth - 8, listBottom);
        drawColumn(graphics, selected(), leftScroll, left + 12, mid - 8, mouseX, mouseY);
        JsonArray all = new JsonArray();
        for (String name : available) all.add(name);
        drawColumn(graphics, all, rightScroll, mid + 12, left + panelWidth - 8, mouseX, mouseY);
        graphics.disableScissor();
        int visible = Math.max(1, (listBottom - listTop) / 23);
        leftScrollbar.render(graphics, mid - 10, listTop, listBottom, selected().size(), visible, leftScroll);
        rightScrollbar.render(graphics, left + panelWidth - 12, listTop, listBottom,
                available.size(), visible, rightScroll);
        CEDatapackViewport.drawDividers(graphics, left + 8, left + panelWidth - 8, listTop, listBottom);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawColumn(GuiGraphics graphics, JsonArray entries, int scroll, int x, int right, int mouseX, int mouseY) {
        int rows = Math.max(0, (listBottom - listTop) / 23);
        for (int row = 0; row < rows && row + scroll < entries.size(); row++) {
            int y = listTop + row * 23;
            graphics.fill(x - 4, y, right, y + 22,
                    mouseX >= x && mouseX < right && mouseY >= y && mouseY < y + 22
                            ? 0xFF34485C : row % 2 == 0 ? 0xFF273746 : 0xFF202F3D);
            graphics.drawString(font, font.plainSubstrByWidth(entries.get(row + scroll).getAsString(), right - x - 5),
                    x, y + 7, 0xFFE7ECF2);
        }
    }
}
