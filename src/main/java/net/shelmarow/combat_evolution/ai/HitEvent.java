package net.shelmarow.combat_evolution.ai;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class HitEvent {
    private final BiConsumer<MobPatch<?>, Entity> behavior;
    private final int phase;
    private final AttackResult.ResultType hitType;

    public HitEvent(BiConsumer<MobPatch<?>,Entity> behavior) {
        this(-1,behavior);
    }

    public HitEvent(int phase, BiConsumer<MobPatch<?>,Entity> behavior) {
        this(phase, AttackResult.ResultType.SUCCESS, behavior);
    }

    public HitEvent(int phase, AttackResult.ResultType hitType, BiConsumer<MobPatch<?>,Entity> behavior){
        this.behavior = behavior;
        this.phase = phase;
        this.hitType = hitType;
    }

    public void executeHitEvent(int phase, AttackResult.ResultType resultType, MobPatch<?> mobPatch, Entity target){
        if(this.hitType == resultType){
            if(this.phase == -1 || this.phase == phase){
                behavior.accept(mobPatch,target);
            }
        }
    }
}
