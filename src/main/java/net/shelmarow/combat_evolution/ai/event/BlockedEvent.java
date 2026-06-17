package net.shelmarow.combat_evolution.ai.event;

import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class BlockedEvent implements CEMobEvent<BlockedEvent.EventParams> {
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
    public void execute(BlockedEvent.EventParams param) {
        int phase = param.phase;
        MobPatch<?> mobPatch = param.mobPatch;
        LivingEntityPatch<?> target = param.target;
        boolean parried = param.parried;
        executeBlockedEvent(phase, mobPatch, target, parried);
    }

    public record EventParams(int phase, MobPatch<?> mobPatch, LivingEntityPatch<?> target, boolean parried){}
}
