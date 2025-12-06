package net.shelmarow.combat_evolution.client.execution.types;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public abstract class HUDType {
    public abstract String getTypeName();
    public abstract ResourceLocation getBackground();
    public abstract ResourceLocation getProgress();
    public abstract List<ResourceLocation> getOverlays();
}
