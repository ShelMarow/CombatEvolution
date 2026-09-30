package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.data.conditions.EpicFightConditions;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
final class CEBehaviorNodeScreen extends CEBehaviorTreeScreen {
    private final JsonObject node;
    private final List<String> parentPath;
    private final CEBehaviorRootScreen rootScreen;
    private final Set<String> collapsed = new HashSet<>(List.of("params", "events", "children"));
    private final Set<JsonObject> expandedItems = Collections.newSetFromMap(new IdentityHashMap<>());

    CEBehaviorNodeScreen(Screen parent, JsonObject node) {
        this(parent, node, parent instanceof CEBehaviorLayoutScreen layout ? layout.hierarchyPath() : List.of());
    }

    CEBehaviorNodeScreen(Screen parent, JsonObject node, List<String> parentPath) {
        super(parent, Component.translatable("config.combat_evolution.datapack_behavior_node_title"));
        this.node = node;
        this.parentPath = List.copyOf(parentPath);
        this.rootScreen = parent instanceof CEBehaviorRootScreen root ? root
                : parent instanceof CEBehaviorNodeScreen ancestor ? ancestor.rootScreen : null;
        if (!node.has("behaviorType")) node.addProperty("behaviorType", inferType());
    }

    private String type() {
        String current = node.get("behaviorType").getAsString().toLowerCase(java.util.Locale.ROOT);
        return current.equals("animation") || current.equals("defense") || current.equals("wander") ? current : inferType();
    }

    private String inferType() {
        if (node.has("animation")) return "animation";
        if (node.has("wanderTime") && !node.has("counterType")) return "wander";
        return "defense";
    }

    @Override
    protected JsonObject treeRoot() { return rootScreen == null ? null : rootScreen.root(); }

    @Override
    protected JsonObject treeCurrent() { return node; }

    @Override
    protected CEBehaviorRootScreen treeRootScreen() { return rootScreen; }

    @Override
    protected List<String> hierarchyPath() {
        List<String> path = new ArrayList<>(parentPath);
        path.add(node.has("behaviorName") ? node.get("behaviorName").getAsString() : type());
        return path;
    }

    void returnToRoot() {
        minecraft.setScreen(rootScreen == null ? parent : rootScreen);
    }

    @Override
    protected int buildContent(int y) {
        y = addHeading(Component.translatable("config.combat_evolution.datapack_behavior_type_title"), y);
        int gap = 4;
        int width = Math.max(1, (innerWidth - 2 * gap) / 3);
        String[] types = {"animation", "defense", "wander"};
        for (int index = 0; index < types.length; index++) {
            String value = types[index];
            CEDatapackButton button = new CEDatapackButton(innerX + index * (width + gap), y, width, 22,
                    Component.translatable("config.combat_evolution.datapack_behavior_type." + value),
                    value.equals(type()), () -> switchType(value));
            addContentWidget(button);
        }
        y += 30;
        y = addFields(node, List.of(Field.decimal("priority", 1), Field.decimal("weight", 1)), y);
        y = addFullWidthField(node, Field.text("behaviorName", type()), y);

        y = addCategoryGap(y);
        y = addHeading(Component.translatable("config.combat_evolution.datapack_behavior_conditions"), y);
        y = addConditions(y);

        y = addCategoryGap(y);
        y = addTypeFields(y);

        y = addCategoryGap(y);
        y = addFoldHeader(Component.translatable("config.combat_evolution.datapack_behavior_parameters"),
                collapsed.contains("params"), () -> toggle("params"), y);
        if (!collapsed.contains("params")) {
            int start = y;
            y += 5;
            y = addFields(node, List.of(
                    Field.integer("stopByStun", 1), Field.integer("waitTime", 0),
                    Field.integer("addCoolDown", 0), Field.integer("setCoolDown", 0),
                    Field.decimal("addStamina", 0), Field.decimal("setStamina", 0),
                    Field.bool("canInterruptParent", false), Field.integer("setPhase", 0),
                    Field.integer("addPhase", 0), Field.decimal("jump", 0)), y);
            y = addFullWidthField(node, Field.cycle("interruptType", "NONE", "NONE", "TIME", "LEVEL"), y);
            if (node.has("interruptType") && !node.get("interruptType").getAsString().equals("NONE"))
                y = addNumberPair(node, "interruptedWindow", y);
            y = addFullWidthField(node, Field.bool("canInsertGlobalBehavior", false), y);
            if (node.has("canInsertGlobalBehavior") && node.get("canInsertGlobalBehavior").getAsBoolean())
                y = addAction(Component.translatable("config.combat_evolution.datapack_field.allowedGlobalNameList"),
                        y, innerWidth, false, () -> minecraft.setScreen(new CEBehaviorGlobalSelectionScreen(this,
                                node, rootScreen == null ? null : rootScreen.document())));
            y += 5;
            addBorder(start, y);
        }

        y += 13;
        y = addFoldHeader(Component.translatable("config.combat_evolution.datapack_behavior_event_list"),
                collapsed.contains("events"), () -> toggle("events"), y);
        if (!collapsed.contains("events")) {
            int start = y;
            y += 5;
            y = addEvents(y);
            y += 5;
            addBorder(start, y);
        }

        y += 13;
        y = addFoldHeader(Component.translatable("config.combat_evolution.datapack_behavior_child_list"),
                collapsed.contains("children"), () -> toggle("children"), y);
        if (!collapsed.contains("children")) {
            int start = y;
            y += 5;
            y = addChildren(array(node, "nextBehaviors"), y);
            y += 5;
            addBorder(start, y);
        }
        return y;
    }

    private void toggle(String key) {
        if (!collapsed.add(key)) collapsed.remove(key);
        minecraft.setScreen(this);
    }

    private void switchType(String nextType) {
        if (nextType.equals(type())) return;
        for (String key : List.of("animation", "animationParams", "counterAnimation", "counterType", "counterChance",
                "maxGuardHit", "guardCost", "resetGuardTime", "guardTime", "guardWithWander",
                "guardAnimation", "wanderTime", "forward", "strafe")) node.remove(key);
        if (!nextType.equals("animation")) {
            node.remove("timeEvents");
            node.remove("blockedEvents");
        }
        if (nextType.equals("wander")) node.remove("hitEvents");
        node.addProperty("behaviorType", nextType);
        if (nextType.equals("defense")) {
            node.addProperty("counterType", "RANDOM");
            node.addProperty("guardTime", 60);
        } else if (nextType.equals("animation")) {
            node.addProperty("animation", "epicfight:biped/combat/longsword_auto1");
        } else if (nextType.equals("wander")) {
            node.addProperty("wanderTime", 40);
        }
        minecraft.setScreen(this);
    }

    private int addConditions(int y) {
        JsonArray conditions = array(node, "conditions");
        for (int index = 0; index < conditions.size(); index++) {
            if (!conditions.get(index).isJsonObject()) continue;
            JsonObject condition = conditions.get(index).getAsJsonObject();
            String name = condition.has("type") ? condition.get("type").getAsString() : "?";
            int itemTop = y;
            y += 4;
            y = addInlineHeader(Component.literal(name), condition, y,
                    () -> { conditions.remove(condition); minecraft.setScreen(this); });
            if (expandedItems.contains(condition)) {
                y += 7;
                y = addInsetFields(condition, conditionFields(name, condition), y);
            }
            y += 4;
            addItemBorder(itemTop, y);
            y += 3;
        }
        return addAction(Component.translatable("config.combat_evolution.datapack_add_condition"), y, 180,
                true, () -> minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        Component.translatable("config.combat_evolution.datapack_add_condition"),
                        EpicFightConditions.REGISTRY.get().getKeys().stream().map(Object::toString).sorted().toList(), "",
                        selected -> conditions.add(newCondition(selected)), Component::literal,
                        Component.translatable("config.combat_evolution.datapack_search_condition_hint"))));
    }

    private JsonObject newCondition(String type) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", type);
        if (type.endsWith("target_in_distance") || type.endsWith("within_distance")
                || type.endsWith("within_angle") || type.endsWith("within_angle_horizontal")) {
            condition.addProperty("min", 0);
            condition.addProperty("max", type.contains("angle") ? 180 : 4);
        } else if (type.endsWith("phase_between") || type.endsWith("attack_level")) {
            condition.addProperty("min", 0);
            condition.addProperty("max", 1);
        } else if (type.endsWith("phase_contain") || type.endsWith("attack_level_contain")) {
            JsonArray values = new JsonArray();
            values.add(0);
            condition.add(type.endsWith("phase_contain") ? "phases" : "levels", values);
        } else if (type.endsWith("current_angle")) {
            condition.addProperty("side", "left");
            condition.addProperty("first", 0);
            condition.addProperty("second", 180);
        } else if (type.endsWith("health_check") || type.endsWith("stamina_check")) {
            condition.addProperty(type.endsWith("health_check") ? "health" : "stamina", 0.75);
            condition.addProperty("comparator", "greater_ratio_contain");
        } else if (type.endsWith("entity_tag")) condition.addProperty("tag", "");
        else if (type.equals("epicfight:random_chance")) condition.addProperty("chance", 0.5);
        else if (type.equals("epicfight:health")) {
            condition.addProperty("health", 0.5);
            condition.addProperty("comparator", "greater_ratio");
        } else if (type.equals("epicfight:offhand_item_category")) condition.addProperty("category", "fist");
        else if (type.equals("epicfight:skill_active")) condition.addProperty("skill", "");
        else if (type.equals("epicfight:player_name")) condition.addProperty("name", "");
        else if (type.equals("epicfight:tag_value")) {
            condition.addProperty("key", "");
            condition.addProperty("value", "");
        }
        return condition;
    }

    private List<Field> conditionFields(String type, JsonObject condition) {
        List<Field> fields = new ArrayList<>();
        if (type.endsWith("target_in_distance") || type.endsWith("within_distance")
                || type.endsWith("phase_between") || type.endsWith("attack_level")
                || type.endsWith("within_angle") || type.endsWith("within_angle_horizontal")) {
            fields.add(Field.decimal("min", 0));
            fields.add(Field.decimal("max", type.contains("angle") ? 180 : 4));
        } else if (type.endsWith("phase_contain")) fields.add(Field.numberList("phases"));
        else if (type.endsWith("attack_level_contain")) fields.add(Field.numberList("levels"));
        else if (type.endsWith("entity_tag")) fields.add(Field.text("tag", ""));
        else if (type.endsWith("current_angle")) {
            fields.add(Field.cycle("side", "left", "left", "right"));
            fields.add(Field.decimal("first", 0));
            fields.add(Field.decimal("second", 180));
        } else if (type.endsWith("health_check") || type.endsWith("stamina_check")) {
            fields.add(Field.decimal(type.endsWith("health_check") ? "health" : "stamina", 0.75));
            fields.add(Field.cycle("comparator", "greater_ratio_contain",
                    "greater_absolute", "greater_absolute_contain", "greater_ratio", "greater_ratio_contain",
                    "less_absolute", "less_absolute_contain", "less_ratio", "less_ratio_contain"));
        } else if (type.equals("epicfight:random_chance")) fields.add(Field.decimal("chance", 0.5));
        else if (type.equals("epicfight:health")) {
            fields.add(Field.decimal("health", 0.5));
            fields.add(Field.cycle("comparator", "greater_ratio",
                    "greater_absolute", "less_absolute", "greater_ratio", "less_ratio"));
        } else if (type.equals("epicfight:offhand_item_category"))
            fields.add(Field.cycle("category", "fist", WeaponCategory.ENUM_MANAGER.universalValues()
                    .stream().map(Object::toString).sorted().toArray(String[]::new)));
        else if (type.equals("epicfight:skill_active")) fields.add(Field.text("skill", ""));
        else if (type.equals("epicfight:player_name")) fields.add(Field.text("name", ""));
        else if (type.equals("epicfight:tag_value")) {
            fields.add(Field.text("key", ""));
            fields.add(Field.text("value", ""));
        } else {
            for (var entry : condition.entrySet()) {
                if (entry.getKey().equals("type") || !entry.getValue().isJsonPrimitive()) continue;
                if (entry.getValue().getAsJsonPrimitive().isBoolean()) fields.add(Field.bool(entry.getKey(), false));
                else if (entry.getValue().getAsJsonPrimitive().isNumber()) fields.add(Field.decimal(entry.getKey(), 0));
                else fields.add(Field.text(entry.getKey(), ""));
            }
        }
        return fields;
    }

    private int addTypeFields(int y) {
        String type = type();
        if (!type.equals("wander")) {
            y = addHeading(Component.translatable("config.combat_evolution.datapack_"
                    + (type.equals("animation") ? "animation_behavior_settings" : "counter_animation_settings")), y);
            String key = type.equals("animation") ? "animation" : "counterAnimation";
            y = addAnimationPicker(key, y);
            y = addFields(node, List.of(Field.decimal("animationParams.transitionTime", 0),
                    Field.decimal("animationParams.playSpeed", 1)), y);
            JsonObject params = node.has("animationParams") && node.get("animationParams").isJsonObject()
                    ? node.getAsJsonObject("animationParams") : new JsonObject();
            if (!node.has("animationParams") || !node.get("animationParams").isJsonObject()) node.add("animationParams", params);
            JsonArray phases = array(params, "phaseParams");
            y += 6;
            for (int index = 0; index < phases.size(); index++) {
                if (!phases.get(index).isJsonObject()) continue;
                JsonObject phase = phases.get(index).getAsJsonObject();
                String phaseName = phase.has("phase") ? phase.get("phase").getAsString() : "-1";
                y = addObjectRow(Component.translatable("config.combat_evolution.datapack_phase_entry", phaseName), y,
                        () -> editPhase(phase), () -> { phases.remove(phase); minecraft.setScreen(this); });
            }
            y = addAction(Component.translatable("config.combat_evolution.datapack_add_phase"), y, 180, true, () -> {
                JsonObject phase = new JsonObject();
                phase.addProperty("phase", -1);
                phases.add(phase);
                editPhase(phase);
            });
        }
        if (type.equals("defense")) {
            y = addCategoryGap(y);
            y = addHeading(Component.translatable("config.combat_evolution.datapack_defense_settings"), y);
            y = addFullWidthField(node, Field.cycle("counterType", "RANDOM", "RANDOM", "END", "NEVER"), y);
            String counterType = node.has("counterType") ? node.get("counterType").getAsString() : "RANDOM";
            if (counterType.equals("RANDOM")) y = addFullWidthField(node, Field.decimal("counterChance", 1), y);
            else if (counterType.equals("END")) y = addFullWidthField(node, Field.integer("maxGuardHit", 3), y);
            y = addFields(node, List.of(Field.decimal("guardCost", 0), Field.bool("resetGuardTime", false)), y);
            boolean wandering = node.has("guardWithWander");
            CEDatapackButton mode = new CEDatapackButton(innerX, y, innerWidth, 22,
                    Component.translatable("config.combat_evolution.datapack_guard_wander." + wandering),
                    wandering, () -> {
                int duration = node.has(wandering ? "guardWithWander" : "guardTime")
                        ? node.get(wandering ? "guardWithWander" : "guardTime").getAsInt() : 60;
                node.remove(wandering ? "guardWithWander" : "guardTime");
                node.addProperty(wandering ? "guardTime" : "guardWithWander", duration);
                if (wandering) {
                    node.remove("guardAnimation");
                    node.remove("forward");
                    node.remove("strafe");
                }
                minecraft.setScreen(this);
            });
            addContentWidget(mode);
            y += 28;
            y = addFullWidthField(node, Field.integer(wandering ? "guardWithWander" : "guardTime", 60), y);
            if (wandering) y = addFields(node, List.of(Field.bool("guardAnimation", false),
                    Field.decimal("forward", 0), Field.decimal("strafe", 0)), y);
        } else if (type.equals("wander")) {
            y = addHeading(Component.translatable("config.combat_evolution.datapack_wander_settings"), y);
            y = addFields(node, List.of(Field.integer("wanderTime", 40),
                    Field.decimal("forward", 0), Field.decimal("strafe", 0)), y);
        }
        return y;
    }

    private int addAnimationPicker(String key, int y) {
        addText(Component.translatable("config.combat_evolution.datapack_field." + key), innerX + 2, y + 2, 0xFFCBD6E2);
        String value = node.has(key) ? node.get(key).getAsString() : "";
        CEDatapackButton choose = new CEDatapackButton(innerX, y + 17, innerWidth, 20,
                Component.literal(value.isBlank() ? "-" : value), false, () -> {
            ResourceLocation id = ResourceLocation.tryParse(value);
            AssetAccessor<? extends StaticAnimation> current = id == null ? null : AnimationManager.byKey(id);
            minecraft.setScreen(new CEAnimationPickerScreen(this, current,
                    selected -> node.addProperty(key, selected.registryName().toString())));
        });
        addContentWidget(choose);
        return y + 42;
    }

    private void editPhase(JsonObject phase) {
        minecraft.setScreen(new CEDatapackFormScreen(this, phase,
                Component.translatable("config.combat_evolution.datapack_phase_parameters"), List.of(
                CEDatapackFormScreen.Field.integer("phase", -1),
                CEDatapackFormScreen.Field.decimal("damageMultiplier", 1),
                CEDatapackFormScreen.Field.decimal("impactMultiplier", 1),
                CEDatapackFormScreen.Field.decimal("armorNegationMultiplier", 1),
                CEDatapackFormScreen.Field.select("stunType", "default",
                        "default", "short", "long", "hold", "fall", "knockdown", "neutralize"),
                CEDatapackFormScreen.Field.stringList("damage_tags"))));
    }

    private List<String> availableEvents() {
        List<String> keys = new ArrayList<>(List.of("onBehaviorStart", "onHurtEvent",
                "onGuardHit", "beforeCounter", "onCounterStart"));
        if (type().equals("animation")) keys.addAll(List.of("timeEvents", "blockedEvents"));
        if (!type().equals("wander")) keys.add("hitEvents");
        keys.removeIf(key -> (key.equals("onHurtEvent") || key.equals("beforeCounter")) && node.has(key));
        return keys;
    }

    private int addEvents(int y) {
        for (String key : List.of("onBehaviorStart", "exBehavior", "onHurtEvent", "timeEvents", "hitEvents",
                "blockedEvents", "onGuardHit", "beforeCounter", "onCounterStart")) {
            if (!node.has(key)) continue;
            JsonElement value = node.get(key);
            if (value.isJsonObject()) {
                JsonObject event = value.getAsJsonObject();
                y = addEventRow(key, event, 0, y, () -> { node.remove(key); minecraft.setScreen(this); });
            } else if (value.isJsonArray()) {
                JsonArray events = value.getAsJsonArray();
                for (int index = 0; index < events.size(); index++) {
                    if (!events.get(index).isJsonObject()) continue;
                    JsonObject event = events.get(index).getAsJsonObject();
                    y = addEventRow(key, event, index + 1, y,
                            () -> { events.remove(event); minecraft.setScreen(this); });
                }
            }
        }
        return addAction(Component.translatable("config.combat_evolution.datapack_add_event"), y, 180, true,
                () -> minecraft.setScreen(new CEDatapackOptionSelectionScreen(this,
                        Component.translatable("config.combat_evolution.datapack_add_event"), availableEvents(), "",
                        this::addEvent, key -> Component.translatable("config.combat_evolution.datapack_event_key." + key)
                                .append("  [" + key + "]"),
                        Component.translatable("config.combat_evolution.datapack_search_event_hint"))));
    }

    private int addEventRow(String key, JsonObject event, int number, int y, Runnable remove) {
        Component label = Component.translatable("config.combat_evolution.datapack_event_key." + key)
                .append(number == 0 ? "" : " " + number);
        int itemTop = y;
        y += 4;
        y = addInlineHeader(label, event, y, remove);
        if (expandedItems.contains(event)) {
            y += 7;
            y = addInsetFields(event, eventFields(key, event), y);
            y = addInsetFullWidthField(event, Field.text("command", ""), y);
        }
        y += 4;
        addItemBorder(itemTop, y);
        return y + 3;
    }

    private void addEvent(String key) {
        JsonObject event = new JsonObject();
        event.addProperty("onTarget", false);
        event.addProperty("command", "");
        switch (key) {
            case "timeEvents" -> { event.addProperty("type", "IN_TIME"); event.addProperty("timeStart", 0.0); }
            case "hitEvents" -> { event.addProperty("type", "SUCCESS"); event.addProperty("phase", -1); }
            case "blockedEvents" -> { event.addProperty("phase", -1); event.addProperty("parried", false); }
            case "onHurtEvent" -> { event.addProperty("returnResult", "DEFAULT"); event.addProperty("damage", -1.0); }
            case "beforeCounter" -> event.addProperty("cancelHitAnimation", false);
        }
        if (key.equals("onHurtEvent") || key.equals("beforeCounter")) node.add(key, event);
        else array(node, key).add(event);
    }

    private List<Field> eventFields(String key, JsonObject event) {
        List<Field> fields = new ArrayList<>();
        if (key.equals("timeEvents")) {
            fields.add(Field.cycle("type", "IN_TIME", "TICK", "IN_TIME", "BETWEEN_TIMES"));
            String timeType = event.has("type") ? event.get("type").getAsString().toUpperCase(java.util.Locale.ROOT) : "IN_TIME";
            if (!timeType.equals("TICK")) fields.add(Field.decimal("timeStart", 0));
            if (timeType.equals("BETWEEN_TIMES")) fields.add(Field.decimal("timeEnd", 0));
        } else if (key.equals("hitEvents")) {
            fields.add(Field.cycle("type", "SUCCESS", "SUCCESS", "BLOCKED", "MISSED"));
            fields.add(Field.integer("phase", -1));
        } else if (key.equals("blockedEvents")) {
            fields.add(Field.integer("phase", -1));
            fields.add(Field.bool("parried", false));
        } else if (key.equals("onHurtEvent")) {
            fields.add(Field.cycle("returnResult", "DEFAULT", "DEFAULT", "SUCCESS", "BLOCKED", "MISSED"));
            fields.add(Field.decimal("damage", -1));
        } else if (key.equals("beforeCounter")) fields.add(Field.bool("cancelHitAnimation", false));
        fields.add(Field.bool("onTarget", false));
        return fields;
    }

    private int addInlineHeader(Component title, JsonObject item, int y, Runnable remove) {
        boolean expanded = expandedItems.contains(item);
        CEDatapackButton toggle = new CEDatapackButton(innerX + 12, y,
                Math.max(1, innerWidth - 56), 22,
                Component.literal(expanded ? "v  " : ">  ").append(title), expanded, () -> {
            if (!expandedItems.add(item)) expandedItems.remove(item);
            minecraft.setScreen(this);
        });
        CEDatapackButton button = new CEDatapackButton(innerX + innerWidth - 40, y, 28, 22,
                Component.literal("x"), false, remove);
        addContentWidget(toggle);
        addContentWidget(button);
        return y + 22;
    }

    private int addObjectRow(Component title, int y, Runnable edit, Runnable remove) {
        int removeWidth = Math.min(28, innerWidth);
        CEDatapackButton editButton = new CEDatapackButton(innerX, y, Math.max(1, innerWidth - removeWidth - 4),
                22, title, false, edit);
        CEDatapackButton removeButton = new CEDatapackButton(innerX + innerWidth - removeWidth, y,
                removeWidth, 22, Component.literal("x"), false, remove);
        addContentWidget(editButton);
        addContentWidget(removeButton);
        return y + 27;
    }

}
