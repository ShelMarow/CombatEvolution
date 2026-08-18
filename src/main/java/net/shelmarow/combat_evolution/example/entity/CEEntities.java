package net.shelmarow.combat_evolution.example.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarow;

public class CEEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;

    public static final DeferredHolder<EntityType<?>, EntityType<ShelMarow>> SHELMAROW;

    static {
        ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CombatEvolution.MOD_ID);

        SHELMAROW = ENTITY_TYPES.register("shelmarow",()->EntityType.Builder.of(ShelMarow::new, MobCategory.MONSTER)
                .sized(1f,2f).build("shelmarow"));

    }

}
