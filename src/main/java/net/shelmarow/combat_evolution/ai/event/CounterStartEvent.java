package net.shelmarow.combat_evolution.ai.event;

import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.Consumer;

public class CounterStartEvent implements CEMobEvent<CounterStartEvent.EventParams> {
    private final Consumer<MobPatch<?>> consumer;

    public CounterStartEvent(Consumer<MobPatch<?>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void execute(EventParams param) {
        consumer.accept(param.mobPatch);
    }

    public record EventParams(MobPatch<?> mobPatch){}
}
