package net.shelmarow.combat_evolution.config.screen;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
final class CEBehaviorRootScreen extends CEBehaviorTreeScreen {
    private final JsonObject root;
    private final JsonObject document;

    CEBehaviorRootScreen(Screen parent, JsonObject root, JsonObject document) {
        super(parent, Component.translatable("config.combat_evolution.datapack_behavior_root_title"));
        this.root = root;
        this.document = document;
    }

    JsonObject root() { return root; }
    JsonObject document() { return document; }

    @Override
    protected JsonObject treeRoot() { return root; }

    @Override
    protected JsonObject treeCurrent() { return root; }

    @Override
    protected CEBehaviorRootScreen treeRootScreen() { return this; }

    @Override
    protected int buildContent(int y) {
        y = addHeading(Component.translatable("config.combat_evolution.datapack_root_properties"), y);
        y = addFields(root, List.of(
                Field.text("rootName", "behavior_root"), Field.decimal("priority", 1),
                Field.decimal("weight", 1), Field.integer("cooldown", 0),
                Field.integer("maxCooldown", 0), Field.bool("isGlobal", false),
                Field.bool("backAfterFinished", false)), y);
        y = addCategoryGap(y);
        y = addHeading(Component.translatable("config.combat_evolution.datapack_root_children"), y);
        return addChildren(array(root, "firstBehaviors"), y);
    }

    @Override
    protected List<String> hierarchyPath() {
        String name = root.has("rootName") ? root.get("rootName").getAsString() : "behavior_root";
        return List.of(name);
    }
}
