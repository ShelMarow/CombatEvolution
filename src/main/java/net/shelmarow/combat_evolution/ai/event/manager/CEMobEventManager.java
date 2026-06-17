package net.shelmarow.combat_evolution.ai.event.manager;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CEMobEventManager {
    private final Map<Class<? extends CEMobEvent<?>>, List<CEMobEvent<?>>> mobEvents = new HashMap<>();
    private final Map<Class<? extends CEMobEventWithReturn<?, ?>>, CEMobEventWithReturn<?, ?>> mobEventWithReturn = new HashMap<>();

    public final void addEvent(Class<? extends CEMobEvent<?>> eventClass, CEMobEvent<?>... event){
        mobEvents.computeIfAbsent(eventClass, k -> new ArrayList<>()).addAll(Arrays.stream(event).toList());
    }

    public void setEventWithReturn(Class<? extends CEMobEventWithReturn<?, ?>> eventClass, CEMobEventWithReturn<?, ?> event){
        mobEventWithReturn.put(eventClass, event);
    }


    @SuppressWarnings("unchecked")
    public <P> void execute(Class<? extends CEMobEvent<?>> eventClass, P param){
        for (CEMobEvent<?> ceMobEvent : this.mobEvents.getOrDefault(eventClass, new ArrayList<>())) {
            ((CEMobEvent<P>) ceMobEvent).execute(param);
        }
    }

    @SuppressWarnings("unchecked")
    public <P, R> @Nullable R executeAndReturn(Class<? extends CEMobEventWithReturn<P,R>> eventClass, P param){
        if(mobEventWithReturn.containsKey(eventClass)){
            return ((CEMobEventWithReturn<P, R>) mobEventWithReturn.get(eventClass)).executeAndReturn(param);
        }
        return null;
    }

    public List<? extends CEMobEvent<?>> getEvents(Class<? extends CEMobEvent<?>> eventClass) {
        return this.mobEvents.getOrDefault(eventClass, new ArrayList<>());
    }
}
