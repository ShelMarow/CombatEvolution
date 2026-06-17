package net.shelmarow.combat_evolution.ai.event;

import net.shelmarow.combat_evolution.ai.event.manager.CEMobEventWithReturn;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.Function;

public class BeforeCounterEvent implements CEMobEventWithReturn<MobPatch<?>, Boolean> {
    private final Function<MobPatch<?>, Boolean> beforeCounterEvents;

    public BeforeCounterEvent(Function<MobPatch<?>, Boolean> beforeCounterEvents) {
        this.beforeCounterEvents = beforeCounterEvents;
    }

    public boolean executeBeforeCounterEvent(MobPatch<?> mobPatch) {
        return beforeCounterEvents.apply(mobPatch);
    }

    @Override
    public Boolean executeAndReturn(MobPatch<?> param) {
        return executeBeforeCounterEvent(param);
    }

}
