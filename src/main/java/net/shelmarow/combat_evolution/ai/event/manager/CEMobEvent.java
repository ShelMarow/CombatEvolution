package net.shelmarow.combat_evolution.ai.event.manager;

public interface CEMobEvent {
    default void execute(Object... params){}

    default Object executeAndReturn(Object... params){
        return null;
    }
}
