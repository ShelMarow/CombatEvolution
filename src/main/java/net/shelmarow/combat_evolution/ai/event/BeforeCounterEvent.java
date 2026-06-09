package net.shelmarow.combat_evolution.ai.event;

import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.Function;

public class BeforeCounterEvent implements CEMobEvent {
    private final Function<MobPatch<?>, Boolean> beforeCounterEvents;

    public BeforeCounterEvent(Function<MobPatch<?>, Boolean> beforeCounterEvents) {
        this.beforeCounterEvents = beforeCounterEvents;
    }

    public boolean executeBeforeCounterEvent(MobPatch<?> mobPatch) {
        return beforeCounterEvents.apply(mobPatch);
    }

    @Override
    public Object executeAndReturn(Object... params) {
        MobPatch<?> mobPatch = (MobPatch<?>) params[0];
        return beforeCounterEvents.apply(mobPatch);
    }
}
