package net.shelmarow.combat_evolution.ai;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.world.entity.data.ExpandedEntityDataAccessor;

public class CEExpandedEntityDataAccessors {
    public static final DeferredRegister<ExpandedEntityDataAccessor<?>> REGISTRY =
            DeferredRegister.create(EpicFightRegistries.Keys.EXPANDED_ENTITY_DATA_ACCESSOR, CombatEvolution.MOD_ID);

    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Boolean>> CAN_MODIFY_SPEED =
            REGISTRY.register("can_modify_speed", () -> ExpandedEntityDataAccessor.<Boolean>builder().dataSerializer(ByteBufCodecs.BOOL).defaultValue(false).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Float>> ATTACK_SPEED =
            REGISTRY.register("attack_speed", () -> ExpandedEntityDataAccessor.<Float>builder().dataSerializer(ByteBufCodecs.FLOAT).defaultValue(1.0F).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Float>> STAMINA =
            REGISTRY.register("stamina", () -> ExpandedEntityDataAccessor.<Float>builder().dataSerializer(ByteBufCodecs.FLOAT).defaultValue(0.0F).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Integer>> STAMINA_STATUS =
            REGISTRY.register("stamina_status", () -> ExpandedEntityDataAccessor.<Integer>builder().dataSerializer(ByteBufCodecs.INT).defaultValue(StaminaStatus.COMMON.ordinal()).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Integer>> PHASE =
            REGISTRY.register("phase", () -> ExpandedEntityDataAccessor.<Integer>builder().dataSerializer(ByteBufCodecs.INT).defaultValue(0).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Boolean>> GUARD =
            REGISTRY.register("guard", () -> ExpandedEntityDataAccessor.<Boolean>builder().dataSerializer(ByteBufCodecs.BOOL).defaultValue(false).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Boolean>> IN_COUNTER =
            REGISTRY.register("in_counter", () -> ExpandedEntityDataAccessor.<Boolean>builder().dataSerializer(ByteBufCodecs.BOOL).defaultValue(false).build());
    public static final DeferredHolder<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Boolean>> WANDER =
            REGISTRY.register("wander", () -> ExpandedEntityDataAccessor.<Boolean>builder().dataSerializer(ByteBufCodecs.BOOL).defaultValue(false).build());
}
