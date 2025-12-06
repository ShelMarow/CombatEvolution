package net.shelmarow.combat_evolution.client.execution.types;

import net.minecraft.resources.ResourceLocation;
import net.shelmarow.combat_evolution.CombatEvolution;

import java.util.List;

public class DefaultType extends HUDType {

    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/execution/common/sword_bg_full.png");
    private static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/execution/common/sword_progress.png");
    @Override
    public String getTypeName() {
        return "default";
    }

    @Override
    public ResourceLocation getBackground() {
        return BG;
    }

    @Override
    public ResourceLocation getProgress() {
        return PROGRESS;
    }

    @Override
    public List<ResourceLocation> getOverlays() {
        return List.of();
    }
}
