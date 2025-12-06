package net.shelmarow.combat_evolution.ai;

import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.Consumer;

public class TimeEvent {
    private boolean available = true;
    private final float timeStart;
    private final float timeEnd;
    private final EventType eventType;
    private final Consumer<MobPatch<?>> behavior;


    public TimeEvent(Consumer<MobPatch<?>> behavior) {
        this.timeStart = 0;
        this.timeEnd = 0;
        this.behavior = behavior;
        this.eventType = EventType.TICK;
    }

    public TimeEvent(float time, Consumer<MobPatch<?>> behavior) {
        this.timeStart = time;
        this.timeEnd = time;
        this.behavior = behavior;
        this.eventType = EventType.IN_TIME;
    }

    public TimeEvent(float timeStart,float timeEnd, Consumer<MobPatch<?>> behavior) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.behavior = behavior;
        this.eventType = EventType.BETWEEN_TIMES;
    }

    public void executeIfAvailable(float preTime, float currenTime, MobPatch<?> mobPatch) {
        switch (this.eventType) {
            case TICK ->  {
                if(available) {
                    this.behavior.accept(mobPatch);
                }
            }
            case IN_TIME -> {
                if(available && preTime >= timeStart && timeStart <= currenTime) {
                    behavior.accept(mobPatch);
                    available = false;
                }
            }
            case BETWEEN_TIMES -> {
                if(available) {
                    if ((preTime >= timeStart && timeStart <= currenTime) || (preTime >= timeEnd && timeEnd <= currenTime)) {
                        behavior.accept(mobPatch);
                    }
                }
            }

        }
    }

    public void resetAvailable() {
        available = true;
    }

    public enum EventType{
        TICK,
        IN_TIME,
        BETWEEN_TIMES
    }
}
