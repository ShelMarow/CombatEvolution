package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
abstract class CEBehaviorTreeScreen extends CEBehaviorLayoutScreen {
    private boolean expandedTree;
    private boolean draggingTree;
    private boolean movedTree;
    private double pressX, pressY;
    private double treePanX, treePanY;

    CEBehaviorTreeScreen(Screen parent, Component title) {
        super(parent, title);
    }

    protected abstract JsonObject treeRoot();

    protected abstract JsonObject treeCurrent();

    protected abstract CEBehaviorRootScreen treeRootScreen();

    @Override
    protected int sidebarWidth() {
        int available = Math.max(1, panelWidth - 50);
        return Math.min(available, expandedTree ? Math.max(100, panelWidth * 2 / 3)
                : Math.max(85, Math.min(180, panelWidth / 4)));
    }

    private int treeLeft() { return innerX + innerWidth + 18; }

    private int treeRight() { return left + panelWidth - 15; }

    @Override
    protected void initSidebarControls() {
        int right = treeRight();
        addFixedWidget(new CEDatapackButton(right - 21, viewportTop + 3, 18, 18,
                Component.literal(expandedTree ? "<" : ">"), false, () -> {
            expandedTree = !expandedTree;
            minecraft.setScreen(this);
        }));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= treeLeft() - 7 && mouseX < treeRight() + 5
                && mouseY >= viewportTop + 24 && mouseY < viewportBottom) {
            draggingTree = true;
            movedTree = false;
            pressX = mouseX;
            pressY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingTree && button == 0) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) > 4) movedTree = true;
            if (movedTree) {
                treePanX += dragX;
                treePanY += dragY;
                clampPan(treeEntries());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingTree) {
            draggingTree = false;
            if (!movedTree && Math.hypot(mouseX - pressX, mouseY - pressY) <= 4)
                openTreeEntry(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= treeLeft() - 7 && mouseX < treeRight() + 5
                && mouseY >= viewportTop && mouseY < viewportBottom) {
            treePanY += (int) Math.signum(delta) * 36;
            clampPan(treeEntries());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int x1 = treeLeft() - 7;
        int x2 = treeRight() + 5;
        graphics.fill(x1, viewportTop, x2, viewportTop + 1, 0xFF52687C);
        graphics.fill(x1, viewportBottom - 1, x2, viewportBottom, 0xFF52687C);
        graphics.fill(x1, viewportTop, x1 + 1, viewportBottom, 0xFF52687C);
        graphics.fill(x2 - 1, viewportTop, x2, viewportBottom, 0xFF52687C);
        graphics.fill(x1 + 1, viewportTop + 23, x2 - 1, viewportTop + 24, 0xFF405165);
        graphics.drawString(font, font.plainSubstrByWidth(
                        Component.translatable("config.combat_evolution.datapack_behavior_hierarchy").getString(),
                        Math.max(1, treeRight() - treeLeft() - 29)),
                treeLeft() + 2, viewportTop + 8, 0xFF8BE2FF);

        List<TreeEntry> entries = treeEntries();
        clampPan(entries);
        graphics.enableScissor(x1 + 1, viewportTop + 24, x2 - 1, viewportBottom - 1);
        for (int index = 0; index < entries.size(); index++) {
            TreeEntry entry = entries.get(index);
            int x = treeLeft() + 8 + entry.depth * 20 + (int) Math.round(treePanX);
            int y = viewportTop + 30 + index * 30 + (int) Math.round(treePanY);
            if (entry.parent >= 0) {
                int parentX = treeLeft() + 8 + (entry.depth - 1) * 20 + (int) Math.round(treePanX);
                int parentY = viewportTop + 30 + entry.parent * 30 + (int) Math.round(treePanY);
                graphics.fill(parentX + 5, parentY + 18, parentX + 6, y + 10, 0xFF52687C);
                graphics.fill(parentX + 5, y + 9, x + 1, y + 10, 0xFF52687C);
            }
            int boxWidth = Math.max(28, Math.min(expandedTree ? 160 : 105, font.width(entry.name) + 18));
            boolean current = entry.object == treeCurrent();
            boolean hovered = mouseX >= x && mouseX < x + boxWidth && mouseY >= y && mouseY < y + 19;
            graphics.fill(x, y, x + boxWidth, y + 19,
                    current ? 0xFF246989 : hovered ? 0xFF375A70 : 0xFF293A49);
            graphics.fill(x, y, x + boxWidth, y + 1, current ? 0xFF8BE2FF : 0xFF52687C);
            graphics.fill(x, y, x + 1, y + 19, current ? 0xFF8BE2FF : 0xFF52687C);
            graphics.drawString(font, font.plainSubstrByWidth(entry.name, boxWidth - 12), x + 6, y + 5,
                    current ? 0xFFFFFFFF : 0xFFCBD6E2);
        }
        graphics.disableScissor();
    }

    private void openTreeEntry(double mouseX, double mouseY) {
        CEBehaviorRootScreen rootScreen = treeRootScreen();
        if (rootScreen == null) return;
        List<TreeEntry> entries = treeEntries();
        for (int index = entries.size() - 1; index >= 0; index--) {
            TreeEntry entry = entries.get(index);
            int x = treeLeft() + 8 + entry.depth * 20 + (int) Math.round(treePanX);
            int y = viewportTop + 30 + index * 30 + (int) Math.round(treePanY);
            int boxWidth = Math.max(28, Math.min(expandedTree ? 160 : 105, font.width(entry.name) + 18));
            if (mouseX < x || mouseX >= x + boxWidth || mouseY < y || mouseY >= y + 19) continue;
            if (entry.object == treeCurrent()) return;
            if (entry.object == treeRoot()) {
                minecraft.setScreen(rootScreen);
            } else {
                List<String> parentPath = new ArrayList<>();
                int parentIndex = entry.parent;
                while (parentIndex >= 0) {
                    TreeEntry ancestor = entries.get(parentIndex);
                    parentPath.add(0, ancestor.name);
                    parentIndex = ancestor.parent;
                }
                minecraft.setScreen(new CEBehaviorNodeScreen(this, entry.object, parentPath));
            }
            return;
        }
    }

    private List<TreeEntry> treeEntries() {
        List<TreeEntry> entries = new ArrayList<>();
        JsonObject root = treeRoot();
        if (root == null) return entries;
        entries.add(new TreeEntry(root, 0, -1,
                root.has("rootName") ? root.get("rootName").getAsString() : "root"));
        if (root.has("firstBehaviors") && root.get("firstBehaviors").isJsonArray())
            collect(root.getAsJsonArray("firstBehaviors"), 1, 0, entries);
        return entries;
    }

    private void collect(JsonArray children, int depth, int parentIndex, List<TreeEntry> entries) {
        for (JsonElement element : children) {
            if (!element.isJsonObject()) continue;
            JsonObject child = element.getAsJsonObject();
            int index = entries.size();
            String name = child.has("behaviorName") ? child.get("behaviorName").getAsString()
                    : child.has("behaviorType") ? child.get("behaviorType").getAsString() : "behavior";
            entries.add(new TreeEntry(child, depth, parentIndex, name));
            if (child.has("nextBehaviors") && child.get("nextBehaviors").isJsonArray())
                collect(child.getAsJsonArray("nextBehaviors"), depth + 1, index, entries);
        }
    }

    private void clampPan(List<TreeEntry> entries) {
        int widest = 0;
        for (TreeEntry entry : entries)
            widest = Math.max(widest, entry.depth * 20 + Math.min(expandedTree ? 160 : 105,
                    Math.max(28, font.width(entry.name) + 18)) + 8);
        int visibleWidth = Math.max(1, treeRight() - treeLeft());
        int horizontalSlack = Math.min(60, visibleWidth / 3);
        treePanX = Math.max(Math.min(0, visibleWidth - widest) - horizontalSlack,
                Math.min(horizontalSlack, treePanX));
        int visibleHeight = Math.max(1, viewportBottom - viewportTop - 24);
        treePanY = Math.max(Math.min(0, visibleHeight - entries.size() * 30 - 10),
                Math.min(0, treePanY));
    }

    private record TreeEntry(JsonObject object, int depth, int parent, String name) { }
}
