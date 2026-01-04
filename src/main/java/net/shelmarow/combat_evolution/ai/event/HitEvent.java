package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class HitEvent {
    private final BiConsumer<MobPatch<?>, Entity> behavior;
    private final int phaseIndex;
    private final AttackResult.ResultType hitType;
    private final HitEventType hitEventType;

    /**默认使用FORGE DAMAGE事件**/
    public HitEvent(BiConsumer<MobPatch<?>,Entity> behavior) {
        this(-1,behavior);
    }

    public HitEvent(int phase, BiConsumer<MobPatch<?>,Entity> behavior) {
        this(phase, AttackResult.ResultType.SUCCESS, HitEventType.DAMAGE_EVENT, behavior);
    }


    /**使用Mixin的EF命中事件，能够判断攻击结果，但是不支持所有攻击动画**/
    public HitEvent(AttackResult.ResultType hitType, BiConsumer<MobPatch<?>,Entity> behavior){
        this(-1, hitType, behavior);
    }

    public HitEvent(int phase, AttackResult.ResultType hitType, BiConsumer<MobPatch<?>,Entity> behavior){
        this(phase, hitType, HitEventType.EF_EVENT, behavior);
    }


    /**给成员变量赋值**/
    public HitEvent(int phase, AttackResult.ResultType hitType, HitEventType hitEventType, BiConsumer<MobPatch<?>,Entity> behavior){
        this.behavior = behavior;
        this.phaseIndex = phase;
        this.hitType = hitType;
        this.hitEventType = hitEventType;
    }



    /**需要传Result的是EF事件，在Mixin中调用执行**/
    public void executeHitEvent(int phase, AttackResult.ResultType resultType, MobPatch<?> mobPatch, Entity target){
        if(this.hitType == resultType && this.hitEventType == HitEventType.EF_EVENT){
            executeEvent(phase, mobPatch, target);
        }
    }

    /**不需要传Result的是FORGE事件，在FORGE伤害事件中调用执行**/
    public void executeHitEvent(int phase, MobPatch<?> mobPatch, Entity target){
        if(this.hitEventType == HitEventType.DAMAGE_EVENT) {
            executeEvent(phase, mobPatch, target);
        }
    }

    private void executeEvent(int phase, MobPatch<?> mobPatch, Entity target) {
        if (this.phaseIndex == -1 || this.phaseIndex == phase) {
            behavior.accept(mobPatch, target);
        }
    }

    public enum HitEventType {
        EF_EVENT,
        DAMAGE_EVENT,
    }
}
