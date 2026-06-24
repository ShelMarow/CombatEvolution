package net.shelmarow.combat_evolution.api.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Cancelable
public class OnExecutionStartEvent extends Event {
    private final @NotNull LivingEntityPatch<?> executor;
    private final @NotNull LivingEntityPatch<?> target;
    private @NotNull ExecutionTypeManager.Type type;

    public OnExecutionStartEvent(@NotNull LivingEntityPatch<?> executor, @NotNull LivingEntityPatch<?> target, @NotNull ExecutionTypeManager.Type type) {
        this.executor = executor;
        this.target = target;
        this.type = type;
    }

    public @NotNull LivingEntityPatch<?> getExecutor() {
        return executor;
    }

    public @NotNull LivingEntityPatch<?> getTarget() {
        return target;
    }

    public ExecutionTypeManager.@NotNull Type getType() {
        return type;
    }

    public void setType(@NotNull ExecutionTypeManager.Type type) {
        this.type = type;
    }
}
