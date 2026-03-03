package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class HitEvent {
    private final BiConsumer<MobPatch<?>, Entity> behavior;
    private final int phaseIndex;
    private final AttackResult.ResultType hitType;

    public HitEvent(BiConsumer<MobPatch<?>, Entity> behavior) {
        this(-1,behavior);
    }

    public HitEvent(int phase, BiConsumer<MobPatch<?>, Entity> behavior) {
        this(phase, AttackResult.ResultType.SUCCESS, behavior);
    }

    public HitEvent(AttackResult.ResultType hitType, BiConsumer<MobPatch<?>, Entity> behavior){
        this(-1, hitType, behavior);
    }

    public HitEvent(int phase, AttackResult.ResultType hitType, BiConsumer<MobPatch<?>, Entity> behavior){
        this.behavior = behavior;
        this.phaseIndex = phase;
        this.hitType = hitType;
    }


    public void executeHitEvent(int phase, AttackResult.ResultType resultType, MobPatch<?> mobPatch, Entity target){
        if(this.hitType == resultType){
            executeEvent(phase, mobPatch, target);
        }
    }

    private void executeEvent(int phase, MobPatch<?> mobPatch, Entity target) {
        if (this.phaseIndex == -1 || this.phaseIndex == phase) {
            behavior.accept(mobPatch, target);
        }
    }
}
