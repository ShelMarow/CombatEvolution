package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
final class CEDatapackEntitySelectionScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private final CEDatapackEditorScreen editor;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private EditBox searchBox;
    private int left;
    private int top;
    private int widthBound;
    private int bottom;
    private int listTop;
    private final CEDatapackScrollbar scrollbar = new CEDatapackScrollbar();
    private int listBottom;
    private int scroll;

    CEDatapackEntitySelectionScreen(CEDatapackEditorScreen editor) {
        super(Component.translatable("config.combat_evolution.datapack_entity_picker_title"));
        this.editor = editor;
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id != null) entries.add(new Entry(id, type.getDescription()));
        }
        entries.sort(Comparator.comparing(entry -> entry.id.toString()));
        filtered.addAll(entries);
    }

    @Override
    protected void init() {
        super.init();
        widthBound = Math.max(1, Math.min(760, width - 16));
        left = (width - widthBound) / 2;
        top = Math.min(10, Math.max(0, height / 10));
        bottom = Math.max(top + 1, height - 10);
        searchBox = new EditBox(font, left + 16, top + 36, Math.max(1, widthBound - 32), 20,
                Component.translatable("config.combat_evolution.datapack_search"));
        searchBox.setHint(Component.translatable("config.combat_evolution.datapack_search_hint"));
        searchBox.setResponder(this::filter);
        addRenderableWidget(searchBox);
        if (!editor.getEntityId().isEmpty()) searchBox.setValue(editor.getEntityId());
        listTop = Math.min(top + 78, Math.max(top, bottom - 38));
        listBottom = Math.max(listTop, bottom - 38);
        addRenderableWidget(new CEDatapackButton(left + widthBound - 112, bottom - 29, 96, 20,
                Component.translatable("config.combat_evolution.cancel"), false, this::onClose));
    }

    private void filter(String value) {
        filtered.clear();
        String query = value.trim().toLowerCase(Locale.ROOT);
        for (Entry entry : entries) {
            if (query.isEmpty() || entry.id.toString().toLowerCase(Locale.ROOT).contains(query)
                    || entry.name.getString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(entry);
        }
        scroll = 0;
    }

    private int visibleRows() {
        return Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
    }

    private void select(Entry entry) {
        String content = findExisting(entry.id).map(this::read).orElseGet(this::template);
        editor.setEntity(entry.id, content);
    }

    private java.util.Optional<Path> findExisting(ResourceLocation id) {
        MinecraftServer server = minecraft == null ? null : minecraft.getSingleplayerServer();
        if (server == null) return java.util.Optional.empty();
        Path packs = server.getWorldPath(LevelResource.DATAPACK_DIR);
        Path relative = Path.of("data", id.getNamespace(), "ce_mobpatch", id.getPath() + ".json");
        Path own = packs.resolve("CombatEvolutionCE").resolve(relative);
        if (Files.isRegularFile(own)) return java.util.Optional.of(own);
        try (var directories = Files.list(packs)) {
            return directories.map(directory -> directory.resolve(relative)).filter(Files::isRegularFile).findFirst();
        } catch (IOException ignored) {
            return java.util.Optional.empty();
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return template();
        }
    }

    private String template() {
        JsonObject root = new JsonObject();
        root.addProperty("faction", "neutral");
        root.addProperty("renderer", "zombie");
        root.addProperty("model", "zombie");
        root.addProperty("armature", "epicfight:entity/biped");
        root.addProperty("humanoid", true);
        root.addProperty("scale", 1.0);
        root.add("combatBehaviors", new JsonArray());
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= listTop && mouseY < listBottom) {
            scroll = Math.max(0, Math.min(filtered.size() - visibleRows(), scroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.press(mouseX, mouseY, button, left + widthBound - 17, listTop, listBottom,
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
        if (minecraft != null) minecraft.setScreen(editor);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0090D13);
        drawPanel(graphics, left, top, widthBound, bottom - top);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFF1F5F9);
        graphics.drawString(font, Component.translatable("config.combat_evolution.datapack_entity_results", filtered.size()),
                left + 16, top + 61, 0xFF64D2FF);
        graphics.fill(left + 12, listTop, left + widthBound - 12, listBottom, 0xA91D2632);
        graphics.fill(left + 12, listTop, left + widthBound - 12, listTop + 1, 0xFF344658);
        int visible = visibleRows();
        graphics.enableScissor(left + 12, listTop, left + widthBound - 12, listBottom);
        for (int row = 0; row < visible && scroll + row < filtered.size(); row++) {
            int y = listTop + row * ROW_HEIGHT;
            Entry entry = filtered.get(scroll + row);
            boolean hovered = mouseX >= left + 12 && mouseX < left + widthBound - 12 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(left + 13, y, left + widthBound - 13, y + ROW_HEIGHT - 1,
                    hovered ? 0xFF34485C : 0xA91D2632);
            graphics.drawString(font, entry.id.toString(), left + 18, y + 6, 0xFFE7ECF2);
            int nameX = Math.min(left + widthBound - 16, left + 28 + font.width(entry.id.toString()));
            String name = font.plainSubstrByWidth(entry.name.getString(), Math.max(0, left + widthBound - 20 - nameX));
            graphics.drawString(font, name, nameX, y + 6, 0xFF9FB1C4);
        }
        graphics.disableScissor();
        scrollbar.render(graphics, left + widthBound - 17, listTop, listBottom,
                filtered.size(), visibleRows(), scroll);
        CEDatapackViewport.drawDividers(graphics, left + 12, left + widthBound - 12, listTop, listBottom);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int panelWidth, int panelHeight) {
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xE6161D27);
        graphics.fill(x, y, x + panelWidth, y + 1, 0xFF3A4656);
        graphics.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, 0xFF3A4656);
        graphics.fill(x, y, x + 1, y + panelHeight, 0xFF3A4656);
        graphics.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, 0xFF3A4656);
    }

    private record Entry(ResourceLocation id, Component name) {
    }
}
