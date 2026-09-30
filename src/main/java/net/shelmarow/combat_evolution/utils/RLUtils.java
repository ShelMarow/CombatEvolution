package net.shelmarow.combat_evolution.utils;

import net.minecraft.resources.ResourceLocation;
import net.shelmarow.combat_evolution.CombatEvolution;

public class RLUtils {
    public static ResourceLocation getRL(String path){
        return ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, path);
    }

    public static ResourceLocation getRL(String nameSpace, String path){
        return ResourceLocation.fromNamespaceAndPath(nameSpace, path);
    }
}
