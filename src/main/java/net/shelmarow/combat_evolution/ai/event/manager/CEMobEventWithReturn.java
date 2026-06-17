package net.shelmarow.combat_evolution.ai.event.manager;

public interface CEMobEventWithReturn<P, R> {
    R executeAndReturn(P param);
}
