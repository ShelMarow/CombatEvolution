package net.shelmarow.combat_evolution.ai.event;

import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class BlockedEvent {
    private final BiConsumer<MobPatch<?>, LivingEntityPatch<?>> behavior;
    private final int phaseIndex;
    private final boolean parried;

    public BlockedEvent(boolean parried,BiConsumer<MobPatch<?>, LivingEntityPatch<?>> behavior) {
        this.phaseIndex = -1;
        this.parried = parried;
        this.behavior = behavior;
    }

    public BlockedEvent(int phase, boolean parried,BiConsumer<MobPatch<?>, LivingEntityPatch<?>> behavior) {
        this.phaseIndex = phase;
        this.parried = parried;
        this.behavior = behavior;
    }


    public void executeBlockedEvent(int phase, MobPatch<?> mobPatch, LivingEntityPatch<?> target, boolean parried) {
        if (this.parried == parried && (this.phaseIndex == -1 || this.phaseIndex == phase)) {
            behavior.accept(mobPatch, target);
        }
    }
}
