package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.StunType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CEDatapackEditorScreen extends Screen {
    private static final String PACK_META = "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"Combat Evolution mob patches\"\n  }\n}\n";
    private static final List<Field> ENTITY_FIELDS = List.of(
            Field.select("faction", "neutral"), Field.select("renderer", "zombie"), Field.select("model", "zombie"),
            Field.select("armature", "epicfight:entity/biped"), Field.bool("humanoid", true), Field.bool("canBeAssassinate", true));
    private static final List<Field> BASIC_FIELDS = List.of(
            Field.decimal("scale", 1.0), Field.decimal("chasingSpeed", 1.25), Field.integer("breakTime", 40),
            Field.integer("recoverTime", 60), Field.integer("staminaRegenDelay", 60), Field.decimal("beParriedDamage", 1.0),
            Field.decimal("guardHitImpact", 1.0), Field.decimal("hurtImpact", 0.35));
    private static final List<Field> BOSS_BAR_FIELDS = List.of(
            Field.bool("ceBossBar.enableBossBar", false), Field.text("ceBossBar.bossBarName", ""),
            Field.text("ceBossBar.bossBarType", "[CE:DefaultType]"), Field.text("ceBossBar.bossBarTextures", ""));
    private static final List<Field> BGM_FIELDS = List.of(
            Field.bool("ceBossMusic.playBGM", false), Field.text("ceBossMusic.bgm", "combat_evolution:music.test_bgm"),
            Field.bool("ceBossMusic.loop", true), Field.integer("ceBossMusic.duration", Integer.MAX_VALUE),
            Field.decimal("ceBossMusic.volume", 1.0), Field.integer("ceBossMusic.fadeIn", 0),
            Field.integer("ceBossMusic.fadeOut", 0));
    private static final String[][] DEFAULT_STUN_ANIMATIONS = {
            {"short", "epicfight:biped/combat/hit_short"},
            {"long", "epicfight:biped/combat/hit_long"},
            {"hold", "epicfight:biped/combat/hit_short"},
            {"fall", "epicfight:biped/living/landing"},
            {"knockdown", "epicfight:biped/combat/knockdown"},
            {"neutralize", "epicfight:biped/skill/guard_break1"}
    };

    private final Screen parent;
    private final JsonObject document = new JsonObject();
    private final List<Control> controls = new ArrayList<>();
    private final List<SectionRegion> sectionRegions = new ArrayList<>();
    private final List<AbstractWidget> fixedButtons = new ArrayList<>();
    private final java.util.Set<String> collapsedSections = new java.util.HashSet<>(List.of(
            "datapack_entity_advanced_title", "datapack_basic_title", "datapack_boss_bar_title", "datapack_bgm_title",
            "datapack_advanced.attributes", "datapack_advanced.stunAnimations",
            "datapack_advanced.weaponLivingMotions", "datapack_advanced.guardHitAnimation",
            "datapack_behavior_category"));
    private String entityId = "";
    private Component status = Component.empty();
    private int panelLeft, panelTop, panelWidth, panelBottom, footerTop, footerHeight, viewportTop, viewportBottom, scroll;
    private int contentHeight;
    private boolean compact;
    private boolean draggingScrollbar;
    private int scrollbarDragOffset;
    private int halfWidth;
    private String tooltipKey;
    private Component tooltipTitle;
    private Component tooltipText;
    private double tooltipX, tooltipY, tooltipTargetX, tooltipTargetY;
    private float tooltipAlpha;
    private long tooltipLastUpdate;
    private boolean tooltipPositionInitialized;
    private CEDatapackButton entityButton;

    public CEDatapackEditorScreen(Screen parent) {
        super(Component.translatable("config.combat_evolution.mob_datapack_editor_title"));
        this.parent = parent;
        resetTemplate();
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.max(1, Math.min(760, width - 12));
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.min(8, Math.max(0, height / 12));
        panelBottom = Math.max(panelTop + 1, height - 8);
        footerHeight = Math.min(52, Math.max(38, height / 8));
        footerTop = Math.max(panelTop, panelBottom - footerHeight);
        int inset = Math.min(18, Math.max(5, panelWidth / 24));
        int innerWidth = Math.max(1, panelWidth - inset * 2 - 12);
        compact = innerWidth < 380;
        halfWidth = compact ? innerWidth : (innerWidth - 10) / 2;
        boolean stackedHeader = innerWidth < 340;
        viewportTop = panelTop + (stackedHeader ? 80 : 54);
        viewportBottom = Math.max(viewportTop, footerTop - 5);

        int entityY = panelTop + 27;
        int sliderWidth = stackedHeader ? innerWidth : Math.min(176, Math.max(100, innerWidth / 3));
        int entityWidth = Math.max(1, innerWidth - (stackedHeader ? 0 : sliderWidth + 6));
        entityButton = button(entityLabel(), panelLeft + inset, entityY, entityWidth, 20, true, this::openEntitySelector);
        fixedButtons.clear();
        addFixedButton(entityButton);
        CEDatapackGuiScaleSlider guiScaleSlider = new CEDatapackGuiScaleSlider(
                panelLeft + inset + (stackedHeader ? 0 : entityWidth + 6),
                stackedHeader ? entityY + 26 : entityY, sliderWidth, 20);
        fixedButtons.add(guiScaleSlider);
        addRenderableWidget(guiScaleSlider);

        controls.clear();
        sectionRegions.clear();
        int y = viewportTop;
        if (hasEntity()) {
            int sectionTop;
            y = addSection("datapack_entity_advanced_title", y);
            if (!isCollapsed("datapack_entity_advanced_title")) {
                sectionTop = y;
                y = addFields(ENTITY_FIELDS, y, true, inset, innerWidth);
                addSectionRegion(sectionTop, y, inset);
            }
            y += 4;
            y = addSection("datapack_basic_title", y);
            if (!isCollapsed("datapack_basic_title")) {
                sectionTop = y;
                y = addFields(BASIC_FIELDS, y, true, inset, innerWidth);
                addSectionRegion(sectionTop, y, inset);
            }
            y += 4;
            y = addSection("datapack_boss_bar_title", y);
            if (!isCollapsed("datapack_boss_bar_title")) {
                sectionTop = y;
                y = addFields(BOSS_BAR_FIELDS, y, true, inset, innerWidth);
                addSectionRegion(sectionTop, y, inset);
            }
            y += 4;
            y = addSection("datapack_bgm_title", y);
            if (!isCollapsed("datapack_bgm_title")) {
                sectionTop = y;
                y = addFields(BGM_FIELDS, y, true, inset, innerWidth);
                addSectionRegion(sectionTop, y, inset);
            }
            for (String key : advancedConfigKeys()) {
                y += 4;
                y = addSection("datapack_advanced." + key, y);
                if (!isCollapsed("datapack_advanced." + key)) {
                    sectionTop = y;
                    y = switch (key) {
                        case "attributes" -> addAttributeFields(y, inset, innerWidth);
                        case "stunAnimations" -> addStunAnimationFields(y, inset, innerWidth);
                        case "weaponLivingMotions" -> addWeaponLivingMotionFields(y, inset, innerWidth);
                        case "guardHitAnimation" -> addGuardHitAnimationFields(y, inset, innerWidth);
                        default -> throw new IllegalArgumentException("Unknown advanced section: " + key);
                    };
                    addSectionRegion(sectionTop, y, inset);
                }
            }
            y += 4;
            y = addSection("datapack_behavior_category", y);
            if (!isCollapsed("datapack_behavior_category")) {
                sectionTop = y;
                y = addBehaviorRootFields(y, inset, innerWidth);
                addSectionRegion(sectionTop, y, inset);
            }
        } else {
        }
        contentHeight = y - viewportTop;
        updateScroll();

        int footerY = panelBottom - 28;
        int buttonGap = 5;
        int footerButtons = hasEntity() ? 3 : 1;
        int buttonWidth = Math.max(1, (panelWidth - inset * 2 - buttonGap * (footerButtons - 1)) / footerButtons);
        int right = panelLeft + panelWidth - inset;
        if (hasEntity()) {
            addFixedButton(button(Component.translatable("config.combat_evolution.save_datapack"),
                    right - buttonWidth, footerY, buttonWidth, 20, true, this::saveDatapack));
            right -= buttonWidth + buttonGap;
            addFixedButton(button(Component.translatable("config.combat_evolution.cancel"),
                    right - buttonWidth, footerY, buttonWidth, 20, false, this::onClose));
            right -= buttonWidth + buttonGap;
            addFixedButton(button(Component.translatable("config.combat_evolution.datapack_json_preview"),
                    right - buttonWidth, footerY, buttonWidth, 20, false, this::openJsonPreview));
            right -= buttonWidth + buttonGap;
        } else {
            addFixedButton(button(Component.translatable("config.combat_evolution.cancel"),
                    right - buttonWidth, footerY, buttonWidth, 20, false, this::onClose));
        }
    }

    private void openJsonPreview() {
        String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(document);
        if (minecraft != null) minecraft.setScreen(new CEDatapackJsonPreviewScreen(this, json));
    }

    private int addSection(String key, int y) {
        Component title = Component.translatable("config.combat_evolution." + key);
        CEDatapackButton header = button(Component.literal(isCollapsed(key) ? ">  " : "v  ").append(title),
                panelLeft + 10, y, panelWidth - 28, 20, false, () -> toggleSection(key));
        header.setPrimary(false);
        header.setSectionHeader(true);
        header.setCustomTooltip(Component.translatable("config.combat_evolution.datapack_section_tooltip." +
                (isCollapsed(key) ? "expand" : "collapse")));
        controls.add(new Control(header, y, 20, title, panelLeft + 12, panelWidth - 34, key));
        addRenderableWidget(header);
        return y + 22;
    }

    private String[] advancedConfigKeys() {
        return new String[]{"attributes", "stunAnimations", "weaponLivingMotions", "guardHitAnimation"};
    }

    private int addBehaviorRootFields(int y, int inset, int innerWidth) {
        y += 4;
        int x = panelLeft + inset;
        CEDatapackButton overview = button(Component.translatable("config.combat_evolution.datapack_all_behavior_roots"),
                x, y, Math.min(innerWidth, 200), 22, false,
                () -> minecraft.setScreen(new CEBehaviorRootsOverviewScreen(this, document)));
        controls.add(new Control(overview, y, 22));
        addRenderableWidget(overview);
        y += 30;
        JsonArray sets = document.has("combatBehaviors") && document.get("combatBehaviors").isJsonArray()
                ? document.getAsJsonArray("combatBehaviors") : new JsonArray();
        for (JsonElement element : sets) {
            if (!element.isJsonObject()) continue;
            JsonObject set = element.getAsJsonObject();
            JsonArray roots = set.has("behaviorRoots") && set.get("behaviorRoots").isJsonArray()
                    ? set.getAsJsonArray("behaviorRoots") : new JsonArray();
            if (!set.has("behaviorRoots") || !set.get("behaviorRoots").isJsonArray()) set.add("behaviorRoots", roots);
            JsonArray categories = weaponCategories(set);
            String style = set.has("style") ? set.get("style").getAsString() : CapabilityItem.Styles.COMMON.toString();
            int gap = 4;
            int removeWidth = Math.min(28, innerWidth);
            int selectWidth = Math.max(1, (innerWidth - removeWidth - gap * 2) / 2);
            controls.add(new Control(null, y, 16,
                    Component.translatable("config.combat_evolution.datapack_weapon_category"), x, selectWidth));
            controls.add(new Control(null, y, 16,
                    Component.translatable("config.combat_evolution.datapack_style"),
                    x + selectWidth + gap, selectWidth));
            y += 16;
            CEDatapackButton categoryButton = button(weaponCategorySummary(categories), x, y, selectWidth, 22,
                    false, () -> openWeaponCategoryPicker(categories));
            CEDatapackButton styleButton = button(Component.literal(style), x + selectWidth + gap, y, selectWidth, 22, false,
                    () -> minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                            Component.translatable("config.combat_evolution.datapack_style"),
                            Style.ENUM_MANAGER.universalValues().stream().map(Object::toString).sorted().toList(),
                            style, selected -> { set.addProperty("style", selected); minecraft.setScreen(this); })));
            CEDatapackButton removeSet = button(Component.literal("x"), x + innerWidth - removeWidth, y,
                    removeWidth, 22, false, () -> { sets.remove(set); minecraft.setScreen(this); });
            for (CEDatapackButton control : List.of(categoryButton, styleButton, removeSet)) {
                controls.add(new Control(control, y, 22)); addRenderableWidget(control);
            }
            y += 27;
            for (int index = 0; index < roots.size(); index++) {
                if (!roots.get(index).isJsonObject()) continue;
                JsonObject root = roots.get(index).getAsJsonObject();
                String name = root.has("rootName") ? root.get("rootName").getAsString() : "behavior_root_" + (index + 1);
                int nameWidth = Math.max(1, innerWidth - removeWidth - 4);
                CEDatapackButton edit = button(Component.literal(name), x, y, nameWidth, 22, false,
                        () -> minecraft.setScreen(new CEBehaviorRootScreen(this, root, document)));
                CEDatapackButton remove = button(Component.literal("x"), x + innerWidth - removeWidth, y,
                        removeWidth, 22, false, () -> {
                    roots.remove(root);
                    if (minecraft != null) minecraft.setScreen(this);
                });
                controls.add(new Control(edit, y, 22));
                controls.add(new Control(remove, y, 22));
                addRenderableWidget(edit);
                addRenderableWidget(remove);
                y += 26;
            }
            CEDatapackButton addRoot = button(Component.translatable("config.combat_evolution.datapack_add_root"),
                    x, y, Math.min(innerWidth, 180), 22, true, () -> addBehaviorRoot(set));
            controls.add(new Control(addRoot, y, 22));
            addRenderableWidget(addRoot);
            y += 30;
        }
        CEDatapackButton addSet = button(Component.translatable("config.combat_evolution.datapack_add_behavior_set"),
                x, y, Math.min(innerWidth, 180), 22, true, this::addBehaviorSet);
        controls.add(new Control(addSet, y, 22));
        addRenderableWidget(addSet);
        y += 27;
        return y;
    }

    private void addBehaviorSet() {
        JsonObject set = new JsonObject();
        JsonArray categories = new JsonArray();
        String category = WeaponCategory.ENUM_MANAGER.universalValues().stream().map(Object::toString)
                .sorted().findFirst().orElse("fist");
        categories.add(category);
        set.add("weaponCategories", categories);
        set.addProperty("style", CapabilityItem.Styles.COMMON.toString());
        set.add("behaviorRoots", new JsonArray());
        if (!document.has("combatBehaviors") || !document.get("combatBehaviors").isJsonArray())
            document.add("combatBehaviors", new JsonArray());
        document.getAsJsonArray("combatBehaviors").add(set);
        minecraft.setScreen(this);
    }

    private void addBehaviorRoot(JsonObject set) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (JsonElement element : set.getAsJsonArray("behaviorRoots")) {
            if (element.isJsonObject() && element.getAsJsonObject().has("rootName")) {
                names.add(element.getAsJsonObject().get("rootName").getAsString());
            }
        }
        int number = 1;
        while (names.contains("behavior_root_" + number)) number++;
        JsonObject root = new JsonObject();
        root.addProperty("rootName", "behavior_root_" + number);
        root.add("firstBehaviors", new JsonArray());
        set.getAsJsonArray("behaviorRoots").add(root);
        minecraft.setScreen(new CEBehaviorRootScreen(this, root, document));
    }

    private JsonArray weaponCategories(JsonObject entry) {
        if (!entry.has("weaponCategories") || !entry.get("weaponCategories").isJsonArray())
            entry.add("weaponCategories", new JsonArray());
        return entry.getAsJsonArray("weaponCategories");
    }

    private Component weaponCategorySummary(JsonArray categories) {
        if (categories.isEmpty()) return Component.translatable("config.combat_evolution.datapack_no_weapon_category");
        List<String> names = new ArrayList<>();
        for (JsonElement category : categories)
            if (category.isJsonPrimitive()) names.add(category.getAsString());
        return Component.literal(String.join(", ", names));
    }

    private void openWeaponCategoryPicker(JsonArray categories) {
        minecraft.setScreen(new CEDatapackWeaponCategorySelectionScreen(this, categories));
    }

    private int addWeaponLivingMotionFields(int y, int inset, int innerWidth) {
        y += 4;
        JsonArray entries = weaponLivingMotionEntries();
        int x = panelLeft + inset;
        int gap = innerWidth >= 40 ? 4 : 0;
        int removeWidth = Math.min(28, innerWidth);
        int selectorWidth = Math.max(1, innerWidth - removeWidth - gap * 2);
        int categoryWidth = Math.max(1, selectorWidth * 3 / 5);
        int styleWidth = Math.max(1, selectorWidth - categoryWidth - gap);

        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            JsonArray categories = weaponCategories(entry);
            String style = entry.has("style") ? entry.get("style").getAsString() : CapabilityItem.Styles.COMMON.toString();
            if (!entry.has("style")) entry.addProperty("style", style);
            JsonObject motions = entry.has("livingMotions") && entry.get("livingMotions").isJsonObject()
                    ? entry.getAsJsonObject("livingMotions") : new JsonObject();
            entry.add("livingMotions", motions);

            controls.add(new Control(null, y, 16, Component.translatable("config.combat_evolution.datapack_weapon_category"), x, categoryWidth));
            int styleX = x + categoryWidth + gap;
            controls.add(new Control(null, y, 16, Component.translatable("config.combat_evolution.datapack_style"), styleX, styleWidth));
            y += 16;

            CEDatapackButton categoryButton = button(weaponCategorySummary(categories), x, y, categoryWidth, 22,
                    false, () -> openWeaponCategoryPicker(categories));
            CEDatapackButton styleButton = button(Component.literal(style), styleX, y, styleWidth, 22, false, () -> {});
            styleButton.setAction(() -> {
                List<String> options = Style.ENUM_MANAGER.universalValues().stream().map(Object::toString)
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList();
                minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        Component.translatable("config.combat_evolution.datapack_style"), options, style, selected -> {
                    entry.addProperty("style", selected);
                    styleButton.setMessage(Component.literal(selected));
                }));
            });
            CEDatapackButton removeEntry = button(Component.literal("x"), x + innerWidth - removeWidth, y, removeWidth, 22,
                    false, () -> { entries.remove(entry); if (minecraft != null) minecraft.setScreen(this); });
            controls.add(new Control(categoryButton, y, 22));
            controls.add(new Control(styleButton, y, 22));
            controls.add(new Control(removeEntry, y, 22));
            addRenderableWidget(categoryButton);
            addRenderableWidget(styleButton);
            addRenderableWidget(removeEntry);
            y += 26;

            int clearWidth = Math.min(28, innerWidth);
            int labelWidth = Math.min(190, Math.max(80, innerWidth / 3));
            int motionGap = innerWidth > labelWidth + clearWidth + 1 ? 4 : 0;
            int animationWidth = Math.max(1, innerWidth - labelWidth - clearWidth - motionGap * 2);
            int animationX = x + labelWidth + motionGap;
            int clearX = x + innerWidth - clearWidth;
            for (var motionEntry : motions.entrySet()) {
                String key = motionEntry.getKey();
                String animationId = motionEntry.getValue().isJsonPrimitive() ? motionEntry.getValue().getAsString() : "";
                controls.add(new Control(null, y, 22, Component.literal(key), x, labelWidth));
                CEDatapackButton animation = button(Component.literal(animationId.isBlank() ? "-" : animationId),
                        animationX, y, animationWidth, 22, false, () -> {});
                animation.setAction(() -> {
                    AssetAccessor<? extends StaticAnimation> initial = animationId.isBlank() ? null : AnimationManager.byKey(animationId);
                    minecraft.setScreen(new CEAnimationPickerScreen(this, initial, selected -> {
                        String selectedId = selected.registryName().toString();
                        motions.addProperty(key, selectedId);
                        animation.setMessage(Component.literal(selectedId));
                    }));
                });
                CEDatapackButton clear = button(Component.literal("x"), clearX, y, clearWidth, 22, false, () -> {
                    motions.remove(key);
                    animation.setMessage(Component.literal("-"));
                });
                controls.add(new Control(animation, y, 22));
                controls.add(new Control(clear, y, 22));
                addRenderableWidget(animation);
                addRenderableWidget(clear);
                y += 25;
            }

            List<String> availableMotions = java.util.Arrays.stream(LivingMotions.values())
                    .map(motion -> motion.toString().toLowerCase(java.util.Locale.ROOT))
                    .filter(key -> !motions.has(key)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            CEDatapackButton addMotion = button(Component.translatable("config.combat_evolution.datapack_add_living_motion"),
                    x, y, Math.min(innerWidth, 180), 22, true, () -> {
                if (availableMotions.isEmpty()) return;
                minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        Component.translatable("config.combat_evolution.datapack_living_motion"),
                        availableMotions, "", selected -> {
                    motions.addProperty(selected, "");
                }));
            });
            addMotion.active = !availableMotions.isEmpty();
            controls.add(new Control(addMotion, y, 22));
            addRenderableWidget(addMotion);
            y += 28;
        }

        CEDatapackButton add = button(Component.translatable("config.combat_evolution.datapack_add_weapon_motion_category"),
                x, y, Math.min(innerWidth, 180), 22, true, this::addWeaponLivingMotionEntry);
        controls.add(new Control(add, y, 22));
        addRenderableWidget(add);
        return y + 27;
    }

    private JsonArray weaponLivingMotionEntries() {
        if (!document.has("weaponLivingMotions") || !document.get("weaponLivingMotions").isJsonArray()) {
            document.add("weaponLivingMotions", new JsonArray());
        }
        return document.getAsJsonArray("weaponLivingMotions");
    }

    private int addGuardHitAnimationFields(int y, int inset, int innerWidth) {
        y += 4;
        JsonArray entries = guardHitAnimationEntries();
        int x = panelLeft + inset;
        int gap = innerWidth >= 40 ? 4 : 0;
        int removeWidth = Math.min(28, innerWidth);
        int selectorWidth = Math.max(1, innerWidth - removeWidth - gap * 2);
        int categoryWidth = Math.max(1, selectorWidth * 3 / 5);
        int styleWidth = Math.max(1, selectorWidth - categoryWidth - gap);

        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            JsonArray categories = weaponCategories(entry);
            String style = entry.has("style") ? entry.get("style").getAsString() : CapabilityItem.Styles.COMMON.toString();
            if (!entry.has("style")) entry.addProperty("style", style);
            JsonArray animations = entry.has("animations") && entry.get("animations").isJsonArray()
                    ? entry.getAsJsonArray("animations") : new JsonArray();
            entry.add("animations", animations);

            controls.add(new Control(null, y, 16, Component.translatable("config.combat_evolution.datapack_weapon_category"), x, categoryWidth));
            int styleX = x + categoryWidth + gap;
            controls.add(new Control(null, y, 16, Component.translatable("config.combat_evolution.datapack_style"), styleX, styleWidth));
            y += 16;

            CEDatapackButton categoryButton = button(weaponCategorySummary(categories), x, y, categoryWidth, 22,
                    false, () -> openWeaponCategoryPicker(categories));
            CEDatapackButton styleButton = button(Component.literal(style), styleX, y, styleWidth, 22, false, () -> {});
            styleButton.setAction(() -> {
                List<String> options = Style.ENUM_MANAGER.universalValues().stream().map(Object::toString)
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList();
                minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        Component.translatable("config.combat_evolution.datapack_style"), options, style, selected -> {
                    entry.addProperty("style", selected);
                    styleButton.setMessage(Component.literal(selected));
                }));
            });
            CEDatapackButton removeEntry = button(Component.literal("x"), x + innerWidth - removeWidth, y, removeWidth, 22,
                    false, () -> { entries.remove(entry); if (minecraft != null) minecraft.setScreen(this); });
            controls.add(new Control(categoryButton, y, 22));
            controls.add(new Control(styleButton, y, 22));
            controls.add(new Control(removeEntry, y, 22));
            addRenderableWidget(categoryButton);
            addRenderableWidget(styleButton);
            addRenderableWidget(removeEntry);
            y += 26;

            int clearWidth = Math.min(28, innerWidth);
            int labelWidth = Math.min(140, Math.max(70, innerWidth / 4));
            int animationGap = innerWidth > labelWidth + clearWidth + 1 ? 4 : 0;
            int animationWidth = Math.max(1, innerWidth - labelWidth - clearWidth - animationGap * 2);
            int animationX = x + labelWidth + animationGap;
            int clearX = x + innerWidth - clearWidth;
            for (int index = 0; index < animations.size(); index++) {
                int animationIndex = index;
                String animationId = animations.get(index).isJsonPrimitive() ? animations.get(index).getAsString() : "";
                controls.add(new Control(null, y, 22,
                        Component.translatable("config.combat_evolution.datapack_animation_entry", index + 1), x, labelWidth));
                CEDatapackButton animation = button(Component.literal(animationId.isBlank() ? "-" : animationId),
                        animationX, y, animationWidth, 22, false, () -> {});
                animation.setAction(() -> openGuardAnimationPicker(animationId, selected -> {
                    animations.set(animationIndex, new JsonPrimitive(selected));
                    animation.setMessage(Component.literal(selected));
                }));
                CEDatapackButton clear = button(Component.literal("x"), clearX, y, clearWidth, 22, false,
                        () -> { animations.remove(animationIndex); if (minecraft != null) minecraft.setScreen(this); });
                controls.add(new Control(animation, y, 22));
                controls.add(new Control(clear, y, 22));
                addRenderableWidget(animation);
                addRenderableWidget(clear);
                y += 25;
            }

            CEDatapackButton addAnimation = button(Component.translatable("config.combat_evolution.datapack_add_animation"),
                    x, y, Math.min(innerWidth, 180), 22, true,
                    () -> openGuardAnimationPicker("", selected -> {
                        animations.add(selected);
                        if (minecraft != null) minecraft.setScreen(this);
                    }));
            controls.add(new Control(addAnimation, y, 22));
            addRenderableWidget(addAnimation);
            y += 28;
        }

        CEDatapackButton addEntry = button(Component.translatable("config.combat_evolution.datapack_add_guard_animation_category"),
                x, y, Math.min(innerWidth, 220), 22, true, this::addGuardHitAnimationEntry);
        controls.add(new Control(addEntry, y, 22));
        addRenderableWidget(addEntry);
        return y + 27;
    }

    private JsonArray guardHitAnimationEntries() {
        if (!document.has("guardHitAnimation") || !document.get("guardHitAnimation").isJsonArray()) {
            document.add("guardHitAnimation", new JsonArray());
        }
        return document.getAsJsonArray("guardHitAnimation");
    }

    private void addGuardHitAnimationEntry() {
        JsonArray categories = new JsonArray();
        categories.add("fist");
        JsonObject entry = new JsonObject();
        entry.add("weaponCategories", categories);
        entry.addProperty("style", CapabilityItem.Styles.COMMON.toString());
        entry.add("animations", new JsonArray());
        guardHitAnimationEntries().add(entry);
        if (minecraft != null) minecraft.setScreen(this);
    }

    private void openGuardAnimationPicker(String initial, java.util.function.Consumer<String> consumer) {
        AssetAccessor<? extends StaticAnimation> selected = initial.isBlank() ? null : AnimationManager.byKey(initial);
        minecraft.setScreen(new CEAnimationPickerScreen(this, selected, animation ->
                consumer.accept(animation.registryName().toString())));
    }

    private void addWeaponLivingMotionEntry() {
        JsonArray weaponCategories = new JsonArray();
        weaponCategories.add("fist");
        JsonObject entry = new JsonObject();
        entry.add("weaponCategories", weaponCategories);
        entry.addProperty("style", CapabilityItem.Styles.COMMON.toString());
        entry.add("livingMotions", new JsonObject());
        weaponLivingMotionEntries().add(entry);
        if (minecraft != null) minecraft.setScreen(this);
    }

    private int addAttributeFields(int y, int inset, int innerWidth) {
        int x = panelLeft + inset;
        int removeWidth = Math.min(28, innerWidth);
        int gap = innerWidth >= 3 ? 4 : 0;
        int valueWidth = Math.max(1, Math.min(160, (innerWidth - removeWidth - gap * 2) / 3));
        int attributeWidth = Math.max(1, innerWidth - removeWidth - valueWidth - gap * 2);
        int valueX = x + attributeWidth + gap;
        int removeX = x + innerWidth - removeWidth;

        controls.add(new Control(null, y, 18, fieldLabel("attribute"), x, attributeWidth));
        controls.add(new Control(null, y, 18, fieldLabel("value"), valueX, valueWidth));
        y += 18;

        JsonArray attributes = document.has("attributes") && document.get("attributes").isJsonArray()
                ? document.getAsJsonArray("attributes") : new JsonArray();
        List<String> options = ForgeRegistries.ATTRIBUTES.getKeys().stream().map(ResourceLocation::toString)
                .sorted(String.CASE_INSENSITIVE_ORDER).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (JsonElement element : attributes) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String attributeId = entry.has("attribute") ? entry.get("attribute").getAsString() : "";
            if (!attributeId.isBlank() && options.stream().noneMatch(option -> option.equalsIgnoreCase(attributeId))) {
                options.add(attributeId);
            }
            CEDatapackButton select = button(Component.literal(attributeId), x, y, attributeWidth, 22, false, () -> {});
            select.setAction(() -> minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                    fieldLabel("attribute"), options, attributeId, selected -> {
                entry.addProperty("attribute", selected);
                select.setMessage(Component.literal(selected));
            })));
            EditBox value = new EditBox(font, valueX, y, valueWidth, 22, fieldLabel("value"));
            value.setMaxLength(32);
            value.setValue(entry.has("value") ? entry.get("value").getAsString() : "0");
            value.setResponder(text -> {
                try {
                    double number = Double.parseDouble(text);
                    if (Double.isFinite(number)) entry.addProperty("value", number);
                } catch (NumberFormatException ignored) {
                }
            });
            CEDatapackButton remove = button(Component.literal("x"), removeX, y, removeWidth, 22, false,
                    () -> { attributes.remove(entry); if (minecraft != null) minecraft.setScreen(this); });
            controls.add(new Control(select, y, 22));
            controls.add(new Control(value, y, 22));
            controls.add(new Control(remove, y, 22));
            addRenderableWidget(select);
            addRenderableWidget(value);
            addRenderableWidget(remove);
            y += 27;
        }

        CEDatapackButton add = button(Component.translatable("config.combat_evolution.datapack_add_entry"),
                x, y, Math.min(innerWidth, 180), 22, true, this::addAttributeEntry);
        controls.add(new Control(add, y, 22));
        addRenderableWidget(add);
        return y + 27;
    }

    private void addAttributeEntry() {
        JsonArray attributes;
        if (document.has("attributes") && document.get("attributes").isJsonArray()) {
            attributes = document.getAsJsonArray("attributes");
        } else {
            attributes = new JsonArray();
            document.add("attributes", attributes);
        }
        String attribute = ForgeRegistries.ATTRIBUTES.getKeys().stream().map(ResourceLocation::toString)
                .sorted(String.CASE_INSENSITIVE_ORDER).findFirst().orElse("minecraft:generic.max_health");
        JsonObject entry = new JsonObject();
        entry.addProperty("attribute", attribute);
        entry.addProperty("value", 0.0);
        attributes.add(entry);
        if (minecraft != null) minecraft.setScreen(this);
    }

    private int addStunAnimationFields(int y, int inset, int innerWidth) {
        ensureStunAnimationDefaults();
        JsonObject animations = document.getAsJsonObject("stunAnimations");
        int x = panelLeft + inset;
        int clearWidth = Math.min(28, innerWidth);
        int typeWidth = Math.min(140, Math.max(72, innerWidth / 4));
        int gap = innerWidth >= typeWidth + clearWidth + 2 ? 4 : 0;
        int animationWidth = Math.max(1, innerWidth - typeWidth - clearWidth - gap * 2);
        int animationX = x + typeWidth + gap;
        int clearX = x + innerWidth - clearWidth;

        controls.add(new Control(null, y, 18, Component.translatable("config.combat_evolution.datapack_stun_type"), x, typeWidth));
        controls.add(new Control(null, y, 18, Component.translatable("config.combat_evolution.datapack_animation"), animationX, animationWidth));
        y += 18;

        for (StunType type : StunType.values()) {
            if (type == StunType.NONE) continue;
            String key = type.name().toLowerCase(java.util.Locale.ROOT);
            String animationId = animations.has(key) ? animations.get(key).getAsString() : "";
            Component typeLabel = Component.literal(type.name());
            controls.add(new Control(null, y, 22, typeLabel, x, typeWidth));
            CEDatapackButton select = button(Component.literal(animationId.isBlank() ? "-" : animationId),
                    animationX, y, animationWidth, 22, false, () -> {});
            select.setAction(() -> {
                AssetAccessor<? extends StaticAnimation> initial = animationId.isBlank() ? null : AnimationManager.byKey(animationId);
                minecraft.setScreen(new CEAnimationPickerScreen(this, initial, selected -> {
                    String selectedId = selected.registryName().toString();
                    animations.addProperty(key, selectedId);
                    select.setMessage(Component.literal(selectedId));
                }));
            });
            CEDatapackButton clear = button(Component.literal("x"), clearX, y, clearWidth, 22, false, () -> {
                animations.addProperty(key, "");
                select.setMessage(Component.literal("-"));
            });
            controls.add(new Control(select, y, 22));
            controls.add(new Control(clear, y, 22));
            addRenderableWidget(select);
            addRenderableWidget(clear);
            y += 26;
        }
        return y + 4;
    }

    private void ensureStunAnimationDefaults() {
        if (!document.has("stunAnimations") || !document.get("stunAnimations").isJsonObject()) {
            document.add("stunAnimations", new JsonObject());
        }
        JsonObject animations = document.getAsJsonObject("stunAnimations");
        for (String[] entry : DEFAULT_STUN_ANIMATIONS) {
            if (!animations.has(entry[0])) animations.addProperty(entry[0], entry[1]);
        }
    }

    private void addSectionRegion(int contentTop, int contentBottom, int inset) {
        if (contentBottom > contentTop) {
            sectionRegions.add(new SectionRegion(panelLeft + inset - 6, panelLeft + panelWidth - inset - 6,
                    contentTop + 1, contentBottom - 2));
        }
    }

    private boolean isCollapsed(String key) {
        return collapsedSections.contains(key);
    }

    private void toggleSection(String key) {
        boolean expanding = collapsedSections.contains(key);
        int anchorScreenY = viewportTop;
        for (Control control : controls) {
            if (key.equals(control.sectionKey)) {
                anchorScreenY = Math.max(viewportTop, control.y - scroll);
                break;
            }
        }
        if (!collapsedSections.add(key)) collapsedSections.remove(key);
        if (minecraft != null) {
            minecraft.setScreen(this);
            for (Control control : controls) {
                if (key.equals(control.sectionKey)) {
                    scroll = Math.max(0, control.y - anchorScreenY);
                    if (expanding) scroll = Math.max(scroll, control.y - viewportTop - 16);
                    updateScroll();
                    break;
                }
            }
        }
    }

    private int addFields(List<Field> fields, int y, boolean twoColumns, int inset, int innerWidth) {
        boolean columns = twoColumns && !compact;
        int countPerRow = columns ? 2 : 1;
        int rowHeight = 44;
        for (int i = 0; i < fields.size(); i += countPerRow) {
            for (int column = 0; column < countPerRow && i + column < fields.size(); column++) {
                Field field = fields.get(i + column);
                int cellX = panelLeft + inset + (columns ? column * (halfWidth + 10) : 0);
                int cellWidth = columns ? halfWidth : innerWidth;
                Component label = fieldLabel(field.key);
                AbstractWidget widget;
                if (field.key.equals("ceBossBar.bossBarType")) {
                    CEDatapackButton select = button(Component.literal(readValue(field)), cellX, y + 20, cellWidth, 20, false, () -> {});
                    select.setAction(() -> minecraft.setScreen(new CEBossBarTypeSelectionScreen(this,
                            readValue(field), selected -> writeValue(field, selected))));
                    widget = select;
                } else if (field.type == Type.SELECT) {
                    CEDatapackButton select = button(Component.literal(readValue(field)), cellX, y + 20, cellWidth, 20, false, () -> {});
                    select.setAction(() -> openOptions(field, select));
                    widget = select;
                } else if (field.type == Type.BOOLEAN) {
                    widget = button(toggleLabel(readValue(field)), cellX, y + 20, cellWidth, 20, false, () -> {});
                } else {
                    EditBox input = new EditBox(font, cellX, y + 20, cellWidth, 20, label);
                    input.setMaxLength(256);
                    input.setValue(readValue(field));
                    input.setResponder(value -> writeValue(field, value));
                    widget = input;
                }
                if (field.type == Type.BOOLEAN) {
                    CEDatapackButton toggle = (CEDatapackButton) widget;
                    toggle.setAction(() -> {
                        boolean next = !Boolean.parseBoolean(readValue(field));
                        writeValue(field, Boolean.toString(next));
                        toggle.setMessage(toggleLabel(Boolean.toString(next)));
                    });
                }
                controls.add(new Control(widget, y, rowHeight, label, cellX, cellWidth, 20));
                addRenderableWidget(widget);
            }
            y += rowHeight;
        }
        return y;
    }

    private Component fieldLabel(String key) {
        return Component.translatable("config.combat_evolution.datapack_field." + key.replace('.', '_'));
    }

    private Component toggleLabel(String value) {
        return Boolean.parseBoolean(value) ? Component.translatable("config.combat_evolution.on")
                : Component.translatable("config.combat_evolution.off");
    }

    private String entityLabel() {
        return entityId.isEmpty() ? Component.translatable("config.combat_evolution.datapack_choose_entity").getString()
                : Component.translatable("config.combat_evolution.datapack_selected_entity", entityId).getString();
    }

    private boolean hasEntity() {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        return id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id);
    }

    private void openEntitySelector() {
        if (minecraft != null) minecraft.setScreen(new CEDatapackEntitySelectionScreen(this));
    }

    private void openOptions(Field field, CEDatapackButton target) {
        List<String> options = optionsFor(field);
        String current = readValue(field);
        if (!options.stream().anyMatch(option -> option.equalsIgnoreCase(current))) options.add(current);
        options.sort(String.CASE_INSENSITIVE_ORDER);
        if (minecraft != null) {
            minecraft.setScreen(new CEDatapackOptionSelectionScreen(this, fieldLabel(field.key), options, current, selected -> {
                writeValue(field, selected);
                target.setMessage(Component.literal(selected));
            }));
        }
    }

    private List<String> optionsFor(Field field) {
        return switch (field.key) {
            case "faction" -> java.util.Arrays.stream(Factions.values()).map(Enum::name)
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            case "renderer" -> ClientEngine.getInstance().renderEngine.getRendererEntries().stream()
                    .map(ResourceLocation::toString).sorted(String.CASE_INSENSITIVE_ORDER).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            case "model" -> Meshes.entry(SkinnedMesh.class).stream().map(mesh -> mesh.registryName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            case "armature" -> Armatures.entry().stream().map(entry -> entry.getFirst().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            default -> new ArrayList<>();
        };
    }

    void setEntity(ResourceLocation id, String json) {
        entityId = id.toString();
        try {
            JsonObject loaded = JsonParser.parseString(json).getAsJsonObject();
            document.entrySet().clear();
            loaded.entrySet().forEach(entry -> document.add(entry.getKey(), entry.getValue().deepCopy()));
            ensureStunAnimationDefaults();
            status = Component.translatable("config.combat_evolution.datapack_loaded");
        } catch (RuntimeException exception) {
            resetTemplate();
            status = Component.translatable("config.combat_evolution.datapack_invalid_json");
        }
        if (minecraft != null) minecraft.setScreen(this);
    }

    String getEntityId() { return entityId; }

    private void resetTemplate() {
        document.entrySet().clear();
        document.addProperty("faction", "neutral");
        document.addProperty("renderer", "zombie");
        document.addProperty("model", "zombie");
        document.addProperty("armature", "epicfight:entity/biped");
        document.addProperty("humanoid", true);
        document.addProperty("scale", 1.0);
        document.add("combatBehaviors", new JsonArray());
        ensureStunAnimationDefaults();
    }

    private String readValue(Field field) {
        JsonElement value = getPath(document, field.key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : field.defaultValue;
    }

    private void writeValue(Field field, String text) {
        String value = text.trim();
        if (value.isEmpty()) {
            removePath(document, field.key);
            return;
        }
        try {
            JsonPrimitive primitive = switch (field.type) {
                case STRING, SELECT -> new JsonPrimitive(value);
                case BOOLEAN -> new JsonPrimitive(Boolean.parseBoolean(value));
                case INTEGER -> new JsonPrimitive(Integer.parseInt(value));
                case DECIMAL -> {
                    double number = Double.parseDouble(value);
                    yield Double.isFinite(number) ? new JsonPrimitive(number) : null;
                }
            };
            if (primitive != null) setPath(document, field.key, primitive);
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyJsonFields() {
        if (document.has("weaponLivingMotions") && document.get("weaponLivingMotions").isJsonArray()) {
            for (JsonElement element : document.getAsJsonArray("weaponLivingMotions")) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("livingMotions") || !entry.get("livingMotions").isJsonObject()) continue;
                JsonObject motions = entry.getAsJsonObject("livingMotions");
                motions.entrySet().removeIf(motion -> motion.getValue().isJsonNull()
                        || (motion.getValue().isJsonPrimitive() && motion.getValue().getAsString().isBlank()));
            }
        }
        if (document.has("guardHitAnimation") && document.get("guardHitAnimation").isJsonArray()) {
            for (JsonElement element : document.getAsJsonArray("guardHitAnimation")) {
                if (!element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                if (entry.has("animations") && entry.get("animations").isJsonArray()) {
                    JsonArray animations = entry.getAsJsonArray("animations");
                    for (int index = animations.size() - 1; index >= 0; index--) {
                        JsonElement animation = animations.get(index);
                        if (animation.isJsonNull() || (animation.isJsonPrimitive() && animation.getAsString().isBlank())) {
                            animations.remove(index);
                        }
                    }
                }
            }
        }
        status = Component.empty();
    }

    private void saveDatapack() {
        applyJsonFields();
        if (!status.getString().isEmpty()) return;
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            status = Component.translatable("config.combat_evolution.datapack_invalid_entity");
            return;
        }
        if (!document.has("armature") || !document.has("renderer") || !document.has("model")) {
            status = Component.translatable("config.combat_evolution.datapack_required_fields");
            return;
        }
        try {
            Path pack = getOutputDirectory();
            Path target = pack.resolve(Path.of("data", id.getNamespace(), "ce_mobpatch", id.getPath() + ".json"));
            Files.createDirectories(target.getParent());
            Files.writeString(pack.resolve("pack.mcmeta"), PACK_META, StandardCharsets.UTF_8);
            Files.writeString(target, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(document), StandardCharsets.UTF_8);
            status = Component.translatable("config.combat_evolution.datapack_saved", target.toAbsolutePath().toString());
            Util.getPlatform().openFile(pack.toFile());
        } catch (IOException | SecurityException exception) {
            status = Component.translatable("config.combat_evolution.datapack_save_error", exception.getMessage());
        }
    }

    private Path getOutputDirectory() {
        MinecraftServer server = minecraft == null ? null : minecraft.getSingleplayerServer();
        if (server != null) return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("CombatEvolutionCE");
        return Minecraft.getInstance().gameDirectory.toPath().resolve("CombatEvolutionDatapack");
    }

    private void updateScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - (viewportBottom - viewportTop))));
        for (Control control : controls) {
            if (control.widget != null) {
                int screenY = control.y + control.widgetOffset - scroll;
                control.widget.setY(screenY);
                control.widget.visible = screenY + control.widget.getHeight() > viewportTop && screenY < viewportBottom;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            scroll = Math.max(0, Math.min(Math.max(0, contentHeight - (viewportBottom - viewportTop)),
                    scroll - (int) Math.signum(delta) * 8));
            updateScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouse(mouseY);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasScrollbar() && mouseX >= scrollbarX() - 4 && mouseX <= scrollbarX() + 7
                && mouseY >= viewportTop && mouseY < viewportBottom) {
            int thumbTop = scrollbarThumbTop();
            int thumbHeight = scrollbarThumbHeight();
            scrollbarDragOffset = mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight
                    ? (int) mouseY - thumbTop : thumbHeight / 2;
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }
        if (button == 0 && (mouseY < viewportTop || mouseY >= viewportBottom)) {
            for (Control control : controls) {
                AbstractWidget widget = control.widget;
                if (widget != null && widget.visible && mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, 0xFFF1F5F9);
        graphics.enableScissor(panelLeft + 4, viewportTop, panelLeft + panelWidth - 4, viewportBottom);
        for (SectionRegion region : sectionRegions) {
            int regionTop = region.top - scroll;
            int regionBottom = region.bottom - scroll;
            if (regionBottom > viewportTop && regionTop < viewportBottom)
                graphics.fill(region.left + 1, regionTop + 1, region.right, regionBottom, 0xFF1B2835);
        }
        for (Control control : controls) {
            int screenY = control.y - scroll;
            if (screenY + control.height <= viewportTop || screenY >= viewportBottom) continue;
            if (control.sectionKey != null) {
                graphics.fill(panelLeft + 12, screenY + 18, panelLeft + panelWidth - 18, screenY + 19, 0xFF344658);
            } else if (control.label != null) {
                graphics.drawString(font, control.label, control.x, screenY + 4, 0xFFCBD6E2);
            }
        }
        for (SectionRegion region : sectionRegions) {
            int top = region.top - scroll;
            int bottom = region.bottom - scroll;
            if (bottom <= viewportTop || top >= viewportBottom) continue;
            int left = region.left;
            int right = region.right;
            graphics.fill(left, top, right + 1, top + 1, 0xFF405165);
            graphics.fill(left, bottom, right + 1, bottom + 1, 0xFF405165);
            graphics.fill(left, top, left + 1, bottom + 1, 0xFF405165);
            graphics.fill(right, top, right + 1, bottom + 1, 0xFF405165);
        }
        List<Boolean> buttonVisibility = new ArrayList<>(fixedButtons.size());
        for (AbstractWidget button : fixedButtons) {
            buttonVisibility.add(button.visible);
            button.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        CEDatapackViewport.drawDividers(graphics, panelLeft + 10, panelLeft + panelWidth - 10,
                viewportTop, viewportBottom);
        for (int i = 0; i < fixedButtons.size(); i++) {
            AbstractWidget button = fixedButtons.get(i);
            button.visible = buttonVisibility.get(i);
            if (button.visible) button.render(graphics, mouseX, mouseY, partialTick);
        }
        if (hasScrollbar()) {
            int x = scrollbarX();
            graphics.fill(x, viewportTop, x + 5, viewportBottom, 0xFF1B2530);
            graphics.fill(x, scrollbarThumbTop(), x + 5, scrollbarThumbTop() + scrollbarThumbHeight(), 0xFF71869A);
        }
        graphics.fill(panelLeft + 10, footerTop, panelLeft + panelWidth - 10, footerTop + 1, 0xFF46576B);
        if (!status.getString().isEmpty()) {
            if (footerHeight >= 46) {
                String statusText = font.plainSubstrByWidth(status.getString(), Math.max(1, panelWidth - 24));
                graphics.drawCenteredString(font, statusText, panelLeft + panelWidth / 2, footerTop + 6, 0xFFFFCF79);
            }
        }
        drawSectionTooltip(graphics, mouseX, mouseY);
    }

    private void drawSectionTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Control hoveredControl = null;
        if (mouseY >= viewportTop && mouseY < viewportBottom) {
            for (Control control : controls) {
                if (!(control.widget instanceof CEDatapackButton button) || button.getCustomTooltip() == null || !button.visible) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight()) {
                    hoveredControl = control;
                    break;
                }
            }
        }

        long now = System.nanoTime() / 1_000_000L;
        long elapsed = tooltipLastUpdate == 0 ? 16 : Math.min(50, Math.max(0, now - tooltipLastUpdate));
        tooltipLastUpdate = now;
        float fadeStep = elapsed / 110.0F;
        float moveStep = 1.0F - (float) Math.exp(-elapsed / 85.0F);
        if (hoveredControl != null) {
            String nextKey = hoveredControl.sectionKey;
            if (!nextKey.equals(tooltipKey)) {
                tooltipKey = nextKey;
                tooltipTitle = hoveredControl.label;
                tooltipText = ((CEDatapackButton) hoveredControl.widget).getCustomTooltip();
            }

            List<net.minecraft.util.FormattedCharSequence> previewLines = font.split(tooltipText, Math.max(1, Math.min(220, width - 24)));
            int textWidth = font.width(tooltipTitle);
            for (var line : previewLines) textWidth = Math.max(textWidth, font.width(line));
            int boxWidth = Math.max(1, Math.min(width - 8, textWidth + 20));
            int boxHeight = 28 + previewLines.size() * 10;
            int targetX = mouseX + 14;
            if (targetX + boxWidth > width - 4) targetX = mouseX - boxWidth - 14;
            tooltipTargetX = Math.max(4, Math.min(targetX, width - boxWidth - 4));
            int targetY = mouseY + 14;
            if (targetY + boxHeight > height - 4) targetY = mouseY - boxHeight - 10;
            tooltipTargetY = Math.max(4, Math.min(targetY, height - boxHeight - 4));

            if (!tooltipPositionInitialized) {
                tooltipX = tooltipTargetX;
                tooltipY = tooltipTargetY;
                tooltipPositionInitialized = true;
            } else {
                tooltipX += (tooltipTargetX - tooltipX) * moveStep;
                tooltipY += (tooltipTargetY - tooltipY) * moveStep;
            }
            tooltipAlpha = Math.min(1.0F, tooltipAlpha + fadeStep);
        } else {
            tooltipAlpha = Math.max(0.0F, tooltipAlpha - fadeStep);
        }
        if (tooltipAlpha <= 0.0F || tooltipText == null || tooltipTitle == null) return;

        List<net.minecraft.util.FormattedCharSequence> lines = font.split(tooltipText, Math.max(1, Math.min(220, width - 24)));
        int textWidth = font.width(tooltipTitle);
        for (var line : lines) textWidth = Math.max(textWidth, font.width(line));
        int boxWidth = Math.max(1, Math.min(width - 8, textWidth + 20));
        int boxHeight = 28 + lines.size() * 10;
        int x = Math.max(4, Math.min((int) tooltipX, width - boxWidth - 4));
        int y = Math.max(4, Math.min((int) tooltipY, height - boxHeight - 4));
        int alpha = Math.round(tooltipAlpha * 255.0F);
        int offsetY = Math.round((1.0F - tooltipAlpha) * 3.0F);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, 0.0D, 400.0D);
        graphics.pose().translate(0.0D, offsetY, 0.0D);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, withAlpha(0xF0161D27, alpha));
        graphics.fill(x, y, x + boxWidth, y + 1, withAlpha(0xFF64D2FF, alpha));
        graphics.fill(x, y, x + 2, y + boxHeight, withAlpha(0xFF64D2FF, alpha));
        graphics.fill(x + 2, y + boxHeight - 1, x + boxWidth, y + boxHeight, withAlpha(0xFF405165, alpha));
        graphics.drawString(font, tooltipTitle, x + 10, y + 5, withAlpha(0xFF8BE2FF, alpha));
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x + 10, y + 16 + index * 10, withAlpha(0xFFEAF7FC, alpha));
        }
        graphics.pose().popPose();
    }

    private int withAlpha(int color, int alpha) {
        int colorAlpha = color >>> 24;
        return ((colorAlpha * alpha / 255) << 24) | (color & 0x00FFFFFF);
    }

    private void addFixedButton(CEDatapackButton button) {
        fixedButtons.add(button);
        addRenderableWidget(button);
    }

    private boolean hasScrollbar() {
        return contentHeight > viewportBottom - viewportTop && viewportBottom > viewportTop;
    }

    private int scrollbarX() {
        return panelLeft + panelWidth - 9;
    }

    private int scrollbarThumbHeight() {
        int trackHeight = viewportBottom - viewportTop;
        return Math.min(trackHeight, Math.max(24, trackHeight * trackHeight / Math.max(1, contentHeight)));
    }

    private int scrollbarThumbTop() {
        int trackTravel = Math.max(0, viewportBottom - viewportTop - scrollbarThumbHeight());
        int maxScroll = Math.max(1, contentHeight - (viewportBottom - viewportTop));
        return viewportTop + (int) ((long) trackTravel * scroll / maxScroll);
    }

    private void updateScrollFromMouse(double mouseY) {
        int trackTravel = Math.max(0, viewportBottom - viewportTop - scrollbarThumbHeight());
        int maxScroll = Math.max(0, contentHeight - (viewportBottom - viewportTop));
        if (trackTravel == 0 || maxScroll == 0) return;
        int thumbTop = Math.max(viewportTop, Math.min(viewportTop + trackTravel, (int) mouseY - scrollbarDragOffset));
        scroll = (int) ((long) (thumbTop - viewportTop) * maxScroll / trackTravel);
        updateScroll();
    }

    private CEDatapackButton button(Component text, int x, int y, int width, int height, boolean primary, Runnable action) {
        return new CEDatapackButton(x, y, width, height, text, primary, action);
    }

    private CEDatapackButton button(String text, int x, int y, int width, int height, boolean primary, Runnable action) {
        return button(Component.literal(text), x, y, width, height, primary, action);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xE6161D27);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF3A4656);
        graphics.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0xFF3A4656);
        graphics.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelBottom, 0xFF3A4656);
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

    private enum Type { STRING, SELECT, BOOLEAN, INTEGER, DECIMAL }

    private static final class Field {
        private final String key;
        private final Type type;
        private final String defaultValue;
        private Field(String key, Type type, String defaultValue) { this.key = key; this.type = type; this.defaultValue = defaultValue; }
        static Field text(String key, String value) { return new Field(key, Type.STRING, value); }
        static Field select(String key, String value) { return new Field(key, Type.SELECT, value); }
        static Field bool(String key, boolean value) { return new Field(key, Type.BOOLEAN, Boolean.toString(value)); }
        static Field integer(String key, int value) { return new Field(key, Type.INTEGER, Integer.toString(value)); }
        static Field decimal(String key, double value) { return new Field(key, Type.DECIMAL, Double.toString(value)); }
    }

    private static final class Control {
        private final AbstractWidget widget;
        private final int y, height, x, widgetOffset;
        private final Component section, label;
        private final String sectionKey;
        private Control(AbstractWidget widget, int y, int height) { this(widget, y, height, null, 0, 0, null, 0, null); }
        private Control(AbstractWidget widget, int y, int height, Component section) { this(widget, y, height, null, 0, 0, section, 0, null); }
        private Control(AbstractWidget widget, int y, int height, Component label, int x, int width) { this(widget, y, height, label, x, width, null, 0); }
        private Control(AbstractWidget widget, int y, int height, Component label, int x, int width, int widgetOffset) { this(widget, y, height, label, x, width, null, widgetOffset); }
        private Control(AbstractWidget widget, int y, int height, Component label, int x, int width, String sectionKey) { this(widget, y, height, label, x, width, null, 0, sectionKey); }
        private Control(AbstractWidget widget, int y, int height, Component label, int x, int width, Component section, int widgetOffset) { this(widget, y, height, label, x, width, section, widgetOffset, null); }
        private Control(AbstractWidget widget, int y, int height, Component label, int x, int width, Component section, int widgetOffset, String sectionKey) {
            this.widget = widget; this.y = y; this.height = height; this.label = label; this.x = x; this.section = section; this.widgetOffset = widgetOffset; this.sectionKey = sectionKey;
        }
    }

    private static final class SectionRegion {
        private final int left, right, top, bottom;
        private SectionRegion(int left, int right, int top, int bottom) {
            this.left = left; this.right = right; this.top = top; this.bottom = bottom;
        }
    }
}
