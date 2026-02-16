package net.shelmarow.combat_evolution.ai.iml;

import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public interface CustomExecuteEntity {
    boolean canBeExecuted(LivingEntityPatch<?> executorPatch);
    boolean canUseCustomType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType);
    ExecutionTypeManager.Type getExecutionType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType);
}
