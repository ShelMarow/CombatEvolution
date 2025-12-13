package net.shelmarow.combat_evolution.example.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ShelMarow;

public class CEEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;

    public static final RegistryObject<EntityType<ShelMarow>> SHELMAROW;

    static {
        ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CombatEvolution.MOD_ID);

        SHELMAROW = ENTITY_TYPES.register("shelmarow",()->EntityType.Builder.of(ShelMarow::new, MobCategory.MONSTER)
                .sized(1f,2f).build("shelmarow"));

    }

}

