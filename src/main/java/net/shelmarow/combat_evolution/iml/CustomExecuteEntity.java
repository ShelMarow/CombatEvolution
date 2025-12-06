package net.shelmarow.combat_evolution.iml;

import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public interface CustomExecuteEntity {
    boolean canBeExecuted(LivingEntityPatch<?> entityPatch);
    ExecutionTypeManager.Type getExecutionType();
}
