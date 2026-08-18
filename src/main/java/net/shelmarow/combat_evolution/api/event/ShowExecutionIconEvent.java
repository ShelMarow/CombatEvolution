package net.shelmarow.combat_evolution.api.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class ShowExecutionIconEvent extends Event implements ICancellableEvent {
    private final @NotNull LocalPlayerPatch playerPatch;
    private final @NotNull LivingEntityPatch<?> target;

    public ShowExecutionIconEvent(@NotNull LocalPlayerPatch executor, @NotNull LivingEntityPatch<?> target) {
        this.playerPatch = executor;
        this.target = target;
    }

    public @NotNull LocalPlayerPatch getPlayerPatch() {
        return playerPatch;
    }

    public @NotNull LivingEntityPatch<?> getTarget() {
        return target;
    }
}
