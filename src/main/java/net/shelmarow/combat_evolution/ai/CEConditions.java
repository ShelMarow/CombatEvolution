package net.shelmarow.combat_evolution.ai;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.condition.*;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.main.EpicFightMod;

import java.util.function.Supplier;

public class CEConditions {

    public static final DeferredRegister<Supplier<Condition<?>>> CONDITIONS =
            DeferredRegister.create(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID,"conditions"), CombatEvolution.MOD_ID);

    public static final RegistryObject<Supplier<Condition<?>>> TARGET_IN_DISTANCE =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"target_in_distance").getPath(),() -> TargetInDistance::new);

    public static final RegistryObject<Supplier<Condition<?>>> ENTITY_TAG =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"entity_tag").getPath(),() -> EntityTag::new);

    public static final RegistryObject<Supplier<Condition<?>>> CURRENT_ANGLE =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"current_angle").getPath(),() -> CurrentAngle::new);

    public static final RegistryObject<Supplier<Condition<?>>> HEALTH_CHECK =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"health_check").getPath(),() -> HealthCheck::new);

    public static final RegistryObject<Supplier<Condition<?>>> STAMINA =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"stamina_check").getPath(),() -> StaminaCheck::new);

    public static final RegistryObject<Supplier<Condition<?>>> ATTACK_LEVEL =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"attack_level").getPath(),() -> AttackLevel::new);

    public static final RegistryObject<Supplier<Condition<?>>> ATTACK_LEVEL_CONTAIN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"attack_level_contain").getPath(),() -> AttackLevelContain::new);

    public static final RegistryObject<Supplier<Condition<?>>> PHASE_BETWEEN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"phase_between").getPath(),() -> PhaseBetween::new);

    public static final RegistryObject<Supplier<Condition<?>>> PHASE_CONTAIN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"phase_contain").getPath(),() -> PhaseContain::new);

    public static final RegistryObject<Supplier<Condition<?>>> TARGET_GUARD_BREAK =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"target_guard_break").getPath(),() -> TargetGuardBreak::new);

}
