package net.shelmarow.combat_evolution.ai.event;

import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.Function;

public class BeforeCounterEvent {
    private final Function<MobPatch<?>, Boolean> beforeCounterEvents;

    public BeforeCounterEvent(Function<MobPatch<?>, Boolean> beforeCounterEvents) {
        this.beforeCounterEvents = beforeCounterEvents;
    }

    public boolean executeBeforeCounterEvent(MobPatch<?> mobPatch) {
        return beforeCounterEvents.apply(mobPatch);
    }
}
