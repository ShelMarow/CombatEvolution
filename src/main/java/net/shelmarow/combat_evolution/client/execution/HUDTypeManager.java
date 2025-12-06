package net.shelmarow.combat_evolution.client.execution;

import net.minecraft.resources.ResourceLocation;
import net.shelmarow.combat_evolution.client.execution.types.HUDType;
import net.shelmarow.combat_evolution.config.ClientConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUDTypeManager {
    private static final Map<ResourceLocation, HUDType> HUD_TYPE_MAP = new HashMap<>();

    public static void registerHUDType(String modID,HUDType hudType) {
        HUD_TYPE_MAP.put(ResourceLocation.fromNamespaceAndPath(modID,hudType.getTypeName()), hudType);
    }

    public static void setHUDType(String newType) {
        ClientConfig.HUD_TYPE.set(newType);
        ClientConfig.CLIENT_SPEC.save();
    }

    public static HUDType getHUDType(String typeName) {
        return HUD_TYPE_MAP.get(ResourceLocation.tryParse(typeName));
    }

    public static List<ResourceLocation> getAllHUDTypes() {
        return new ArrayList<>(HUD_TYPE_MAP.keySet());
    }


}
