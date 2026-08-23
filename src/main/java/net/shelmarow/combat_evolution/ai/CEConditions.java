package net.shelmarow.combat_evolution.ai;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.condition.*;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.main.EpicFightMod;

import java.util.function.Supplier;

public class CEConditions {

    public static final DeferredRegister<Supplier<Condition<?>>> CONDITIONS =
            // Epic Fight's registry key is singular: EpicFightRegistries.Keys.CONDITION
            // is built from key("condition"). Registering into "conditions" targets a
            // registry that does not exist, so none of the entries below ever reach
            // EpicFightRegistries.CONDITION. Every datapack using a combat_evolution:*
            // condition then dies in deserializeBehaviorCondition with "Unknown
            // condition", which aborts the whole CEDatapackMobPatch constructor.
            // Code-built mob AI never looks conditions up by id and is unaffected --
            // which is why this only ever showed up on the datapack path.
            DeferredRegister.create(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID,"condition"), CombatEvolution.MOD_ID);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_IN_DISTANCE =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"target_in_distance").getPath(),() -> TargetInDistance::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> ENTITY_TAG =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"entity_tag").getPath(),() -> EntityTag::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> CURRENT_ANGLE =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"current_angle").getPath(),() -> CurrentAngle::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> HEALTH_CHECK =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"health_check").getPath(),() -> HealthCheck::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> STAMINA =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"stamina_check").getPath(),() -> StaminaCheck::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> ATTACK_LEVEL =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"attack_level").getPath(),() -> AttackLevel::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> ATTACK_LEVEL_CONTAIN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"attack_level_contain").getPath(),() -> AttackLevelContain::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> PHASE_BETWEEN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"phase_between").getPath(),() -> PhaseBetween::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> PHASE_CONTAIN =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"phase_contain").getPath(),() -> PhaseContain::new);

    public static final DeferredHolder<Supplier<Condition<?>>, Supplier<Condition<?>>> TARGET_GUARD_BREAK =
            CONDITIONS.register(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID,"target_guard_break").getPath(),() -> TargetGuardBreak::new);

}
