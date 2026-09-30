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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class CEBehaviorRootsOverviewScreen extends Screen {
    private final Screen parent;
    private final JsonObject document;
    private final List<RootEntry> allRoots = new ArrayList<>();
    private final List<RootEntry> roots = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final CEDatapackScrollbar listScrollbar = new CEDatapackScrollbar();
    private String selectedCategory;
    private JsonObject selectedRoot;
    private int left, top, panelWidth, bottom, viewTop, viewBottom, listRight, listScroll;
    private double treePanX, treePanY, pressX, pressY;
    private boolean treePressed, treeMoved;

    CEBehaviorRootsOverviewScreen(Screen parent, JsonObject document) {
        super(Component.translatable("config.combat_evolution.datapack_all_behavior_roots"));
        this.parent = parent;
        this.document = document;
        if (document.has("combatBehaviors") && document.get("combatBehaviors").isJsonArray()) {
            for (JsonElement groupElement : document.getAsJsonArray("combatBehaviors")) {
                if (!groupElement.isJsonObject()) continue;
                JsonObject group = groupElement.getAsJsonObject();
                if (!group.has("weaponCategories") || !group.get("weaponCategories").isJsonArray()) continue;
                Set<String> groupCategories = new LinkedHashSet<>();
                for (JsonElement category : group.getAsJsonArray("weaponCategories"))
                    if (category.isJsonPrimitive()) groupCategories.add(category.getAsString());
                for (String category : groupCategories) if (!categories.contains(category)) categories.add(category);
                if (!group.has("behaviorRoots") || !group.get("behaviorRoots").isJsonArray()) continue;
                String style = group.has("style") ? group.get("style").getAsString() : "common";
                for (JsonElement rootElement : group.getAsJsonArray("behaviorRoots"))
                    if (rootElement.isJsonObject()) allRoots.add(new RootEntry(rootElement.getAsJsonObject(),
                            groupCategories, style));
            }
        }
        if (!categories.isEmpty()) selectCategory(categories.get(0));
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        roots.clear();
        for (RootEntry entry : allRoots) if (entry.categories.contains(category)) roots.add(entry);
        selectedRoot = roots.isEmpty() ? null : roots.get(0).root;
        listScroll = 0;
        treePanX = 0;
        treePanY = 0;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(900, width - 12));
        left = (width - panelWidth) / 2;
        top = Math.min(8, Math.max(0, height / 12));
        bottom = Math.max(top + 1, height - 8);
        viewTop = top + 64;
        viewBottom = Math.max(viewTop, bottom - 38);
        listRight = left + Math.min(260, Math.max(90, panelWidth / 3));
        CEDatapackButton categoryButton = new CEDatapackButton(left + 12, top + 31,
                Math.max(1, panelWidth - 24), 22,
                Component.translatable("config.combat_evolution.datapack_weapon_category")
                        .append(": " + (selectedCategory == null ? "-" : selectedCategory) + "  v"),
                false, () -> {
            if (!categories.isEmpty()) minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                    Component.translatable("config.combat_evolution.datapack_weapon_category"), categories,
                    selectedCategory, this::selectCategory, Component::literal,
                    Component.translatable("config.combat_evolution.datapack_search_weapon_category_hint")));
        });
        categoryButton.active = !categories.isEmpty();
        addRenderableWidget(categoryButton);
        addRenderableWidget(new CEDatapackButton(left + panelWidth - 108, bottom - 28, 96, 20,
                Component.translatable("config.combat_evolution.back"), false, this::onClose));
        clampPan(entries());
    }

    private int treeLeft() { return listRight + 12; }
    private int treeRight() { return left + panelWidth - 12; }
    private int visibleRows() { return Math.max(1, (viewBottom - viewTop - 24 + 33) / 34); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewTop && mouseY < viewBottom) {
            if (mouseX < listRight) {
                listScroll = Math.max(0, Math.min(Math.max(0, roots.size() - visibleRows()),
                        listScroll - (int) Math.signum(delta) * 2));
            } else {
                treePanY += Math.signum(delta) * 36;
                clampPan(entries());
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listScrollbar.press(mouseX, mouseY, button, listRight - 6, viewTop + 23, viewBottom - 1,
                roots.size(), visibleRows(), listScroll, value -> listScroll = value)) return true;
        if (button == 0 && mouseY >= viewTop + 24 && mouseY < viewBottom) {
            if (mouseX >= left + 12 && mouseX < listRight - 6) {
                int index = listScroll + ((int) mouseY - viewTop - 25) / 34;
                if (index >= 0 && index < roots.size()) {
                    RootEntry entry = roots.get(index);
                    if (mouseX >= listRight - 35) openRoot(entry.root);
                    else {
                        selectedRoot = entry.root;
                        List<TreeEntry> entries = entries();
                        for (int row = 0; row < entries.size(); row++) {
                            if (entries.get(row).object == selectedRoot) {
                                treePanY = -row * 30;
                                clampPan(entries);
                                break;
                            }
                        }
                    }
                }
                return true;
            }
            if (mouseX >= treeLeft() && mouseX < treeRight()) {
                treePressed = true;
                treeMoved = false;
                pressX = mouseX;
                pressY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (listScrollbar.drag(mouseY, button, value -> listScroll = value)) return true;
        if (button == 0 && treePressed) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) > 4) treeMoved = true;
            if (treeMoved) {
                treePanX += dragX;
                treePanY += dragY;
                clampPan(entries());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (listScrollbar.release(button)) return true;
        if (button == 0 && treePressed) {
            treePressed = false;
            if (!treeMoved && Math.hypot(mouseX - pressX, mouseY - pressY) <= 4)
                openTreeEntry(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void openRoot(JsonObject root) {
        minecraft.setScreen(new CEBehaviorRootScreen(this, root, document));
    }

    private void openTreeEntry(double mouseX, double mouseY) {
        List<TreeEntry> entries = entries();
        for (int index = entries.size() - 1; index >= 0; index--) {
            TreeEntry entry = entries.get(index);
            int x = treeLeft() + 8 + entry.depth * 20 + (int) Math.round(treePanX);
            int y = viewTop + 27 + index * 30 + (int) Math.round(treePanY);
            int boxWidth = Math.max(30, Math.min(180, font.width(entry.name) + 18));
            if (mouseX < x || mouseX >= x + boxWidth || mouseY < y || mouseY >= y + 19) continue;
            CEBehaviorRootScreen rootScreen = new CEBehaviorRootScreen(this, entry.root, document);
            if (entry.object == entry.root) minecraft.setScreen(rootScreen);
            else {
                List<String> parentPath = new ArrayList<>();
                int parentIndex = entry.parent;
                while (parentIndex >= 0) {
                    TreeEntry ancestor = entries.get(parentIndex);
                    parentPath.add(0, ancestor.name);
                    parentIndex = ancestor.parent;
                }
                minecraft.setScreen(new CEBehaviorNodeScreen(rootScreen, entry.object, parentPath));
            }
            return;
        }
    }

    private List<TreeEntry> entries() {
        List<TreeEntry> entries = new ArrayList<>();
        for (RootEntry rootEntry : roots) {
            JsonObject root = rootEntry.root;
            int index = entries.size();
            entries.add(new TreeEntry(root, root, -1, 0,
                    root.has("rootName") ? root.get("rootName").getAsString() : "root"));
            if (root.has("firstBehaviors") && root.get("firstBehaviors").isJsonArray())
                collect(root.getAsJsonArray("firstBehaviors"), root, index, 1, entries);
        }
        return entries;
    }

    private void collect(JsonArray children, JsonObject root, int parentIndex, int depth, List<TreeEntry> entries) {
        for (JsonElement element : children) {
            if (!element.isJsonObject()) continue;
            JsonObject node = element.getAsJsonObject();
            int index = entries.size();
            String name = node.has("behaviorName") ? node.get("behaviorName").getAsString()
                    : node.has("behaviorType") ? node.get("behaviorType").getAsString() : "behavior";
            entries.add(new TreeEntry(node, root, parentIndex, depth, name));
            if (node.has("nextBehaviors") && node.get("nextBehaviors").isJsonArray())
                collect(node.getAsJsonArray("nextBehaviors"), root, index, depth + 1, entries);
        }
    }

    private void clampPan(List<TreeEntry> entries) {
        int widest = 0;
        for (TreeEntry entry : entries)
            widest = Math.max(widest, entry.depth * 20 + Math.max(30, Math.min(180, font.width(entry.name) + 18)) + 8);
        int visibleWidth = Math.max(1, treeRight() - treeLeft());
        int slack = Math.min(60, visibleWidth / 3);
        treePanX = Math.max(Math.min(0, visibleWidth - widest) - slack, Math.min(slack, treePanX));
        treePanY = Math.max(Math.min(0, viewBottom - viewTop - entries.size() * 30 - 28), Math.min(0, treePanY));
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        graphics.fill(left, top, left + panelWidth, bottom, 0xE6161D27);
        graphics.drawCenteredString(font, title, width / 2, top + 11, 0xFFF1F5F9);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_all_behavior_roots"),
                left + 13, viewTop + 5, 0xFF8BE2FF);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_behavior_hierarchy"),
                treeLeft() + 4, viewTop + 5, 0xFF8BE2FF);
        graphics.fill(left + 10, viewTop + 1, listRight - 5, viewBottom - 1, 0xFF101923);
        graphics.fill(treeLeft() - 3, viewTop + 1, treeRight() + 1, viewBottom - 1, 0xFF111B25);
        drawBorder(graphics, left + 9, listRight - 4);
        drawBorder(graphics, treeLeft() - 4, treeRight() + 2);
        graphics.enableScissor(left + 10, viewTop + 23, listRight - 5, viewBottom - 1);
        for (int row = 0; row < visibleRows() && row + listScroll < roots.size(); row++) {
            RootEntry entry = roots.get(row + listScroll);
            int y = viewTop + 25 + row * 34;
            boolean active = entry.root == selectedRoot;
            graphics.fill(left + 12, y, listRight - 7, y + 32,
                    active ? 0xFF214D69 : row % 2 == 0 ? 0xFF283847 : 0xFF202F3D);
            String name = entry.root.has("rootName") ? entry.root.get("rootName").getAsString() : "root";
            graphics.drawString(font, font.plainSubstrByWidth(name, Math.max(1, listRight - left - 52)),
                    left + 18, y + 4, 0xFFE7ECF2);
            graphics.drawString(font, font.plainSubstrByWidth(entry.style, Math.max(1, listRight - left - 52)),
                    left + 18, y + 18, 0xFF9FB1C4);
            graphics.fill(listRight - 35, y + 4, listRight - 11, y + 28, 0xFF375166);
            graphics.drawCenteredString(font, ">", listRight - 23, y + 12, 0xFFE7ECF2);
        }
        graphics.disableScissor();
        listScrollbar.render(graphics, listRight - 6, viewTop + 23, viewBottom - 1,
                roots.size(), visibleRows(), listScroll);
        if (roots.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("config.combat_evolution.datapack_no_behavior_roots"),
                left + (listRight - left) / 2, viewTop + 36, 0xFF9FB1C4);

        List<TreeEntry> entries = entries();
        clampPan(entries);
        graphics.enableScissor(treeLeft() - 3, viewTop + 23, treeRight() + 1, viewBottom - 1);
        for (int index = 0; index < entries.size(); index++) {
            TreeEntry entry = entries.get(index);
            int x = treeLeft() + 8 + entry.depth * 20 + (int) Math.round(treePanX);
            int y = viewTop + 27 + index * 30 + (int) Math.round(treePanY);
            if (entry.parent >= 0) {
                int parentX = treeLeft() + 8 + (entry.depth - 1) * 20 + (int) Math.round(treePanX);
                int parentY = viewTop + 27 + entry.parent * 30 + (int) Math.round(treePanY);
                graphics.fill(parentX + 5, parentY + 18, parentX + 6, y + 10, 0xFF52687C);
                graphics.fill(parentX + 5, y + 9, x + 1, y + 10, 0xFF52687C);
            }
            int boxWidth = Math.max(30, Math.min(180, font.width(entry.name) + 18));
            boolean highlighted = entry.object == selectedRoot;
            boolean hovered = mouseX >= x && mouseX < x + boxWidth && mouseY >= y && mouseY < y + 19;
            graphics.fill(x, y, x + boxWidth, y + 19,
                    highlighted ? 0xFF246989 : hovered ? 0xFF375A70 : 0xFF293A49);
            graphics.fill(x, y, x + boxWidth, y + 1, highlighted ? 0xFF8BE2FF : 0xFF52687C);
            graphics.drawString(font, font.plainSubstrByWidth(entry.name, boxWidth - 12), x + 6, y + 5,
                    highlighted ? 0xFFFFFFFF : 0xFFCBD6E2);
        }
        graphics.disableScissor();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBorder(GuiGraphics graphics, int x1, int x2) {
        graphics.fill(x1, viewTop, x2, viewTop + 1, 0xFF52687C);
        graphics.fill(x1, viewBottom - 1, x2, viewBottom, 0xFF52687C);
        graphics.fill(x1, viewTop, x1 + 1, viewBottom, 0xFF52687C);
        graphics.fill(x2 - 1, viewTop, x2, viewBottom, 0xFF52687C);
        graphics.fill(x1 + 1, viewTop + 22, x2 - 1, viewTop + 23, 0xFF405165);
    }

    private record RootEntry(JsonObject root, Set<String> categories, String style) { }
    private record TreeEntry(JsonObject object, JsonObject root, int parent, int depth, String name) { }
}
