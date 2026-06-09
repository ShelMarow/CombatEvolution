package net.shelmarow.combat_evolution.ai.event;

import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class BlockedEvent implements CEMobEvent {
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

    @Override
    public void execute(Object... params) {
        int phase = (int) params[0];
        MobPatch<?> mobPatch = (MobPatch<?>) params[1];
        LivingEntityPatch<?> target = (LivingEntityPatch<?>) params[2];
        boolean parried = (boolean) params[3];
        if (this.parried == parried && (this.phaseIndex == -1 || this.phaseIndex == phase)) {
            behavior.accept(mobPatch, target);
        }
    }
}
