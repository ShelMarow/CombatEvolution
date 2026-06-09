package net.shelmarow.combat_evolution.ai.event.manager;

import java.util.*;

public class CEMobEventManager {
    private final Map<Class<? extends CEMobEvent>, List<CEMobEvent>> mobEvents = new HashMap<>();
    private final Map<Class<? extends CEMobEvent>, CEMobEvent> mobEventWithReturn = new HashMap<>();

    public void addEvent(Class<? extends CEMobEvent> eventClass, CEMobEvent... event){
        mobEvents.computeIfAbsent(eventClass, k -> new ArrayList<>()).addAll(Arrays.stream(event).toList());
    }

    public void setEventWithReturn(Class<? extends CEMobEvent> eventClass, CEMobEvent event){
        mobEventWithReturn.put(eventClass, event);
    }

    public void clearEvents(Class<? extends CEMobEvent> eventClass){
        if(mobEvents.containsKey(eventClass)){
            mobEvents.get(eventClass).clear();
        }
    }

    public void execute(Class<? extends CEMobEvent> eventClass, Object... params){
        for (CEMobEvent ceMobEvent : this.mobEvents.getOrDefault(eventClass, new ArrayList<>())) {
            ceMobEvent.execute(params);
        }
    }

    public Object executeAndReturn(Class<? extends CEMobEvent> eventClass, Object... params){
        if(mobEventWithReturn.containsKey(eventClass)){
            return mobEventWithReturn.get(eventClass).executeAndReturn(params);
        }
        return null;
    }
}
