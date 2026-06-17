package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.damagesource.DamageSource;
import net.shelmarow.combat_evolution.ai.event.manager.CEMobEventWithReturn;
import org.apache.commons.lang3.function.TriFunction;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class OnHurtEvent implements CEMobEventWithReturn<OnHurtEvent.EventParams,AttackResult> {
    private final TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> behavior;

    public OnHurtEvent(TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> behavior) {
        this.behavior = behavior;
    }

    public AttackResult executeOnHurtEvent(MobPatch<?> mobPatch, DamageSource damageSource, AttackResult result) {
        return behavior.apply(mobPatch, damageSource, result);
    }

    @Override
    public AttackResult executeAndReturn(OnHurtEvent.EventParams param) {
        MobPatch<?> mobPatch = param.mobPatch;
        DamageSource damageSource = param.damageSource;
        AttackResult result = param.result;
        return executeOnHurtEvent(mobPatch, damageSource, result);
    }

    public record EventParams(MobPatch<?> mobPatch, DamageSource damageSource, AttackResult result){}
}
