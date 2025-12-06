package net.shelmarow.combat_evolution.damage_source;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.shelmarow.combat_evolution.CombatEvolution;

public interface CEDamageTypeTags {

    TagKey<DamageType> EXECUTION = create("execution");

    private static TagKey<DamageType> create(String tagName) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, tagName));
    }
}
