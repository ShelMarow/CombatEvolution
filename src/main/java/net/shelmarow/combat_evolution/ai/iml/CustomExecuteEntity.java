package net.shelmarow.combat_evolution.ai.iml;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.Tags;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public interface CustomExecuteEntity {
    boolean canBeExecuted(LivingEntityPatch<?> executorPatch);
    boolean canUseCustomType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType);
    ExecutionTypeManager.Type getExecutionType(LivingEntityPatch<?> executorPatch, ExecutionTypeManager.Type originalType);

    default boolean canBeAssassinate(LivingEntityPatch<?> executorPatch, LivingEntityPatch<?> targetPatch){
        LivingEntity target = targetPatch.getOriginal();
        boolean hasNoTarget = target instanceof Mob mob && mob.getTarget() == null;
        return hasNoTarget && !target.getType().is(Tags.EntityTypes.BOSSES);
    }
}
