package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.damagesource.DamageSource;
import org.apache.commons.lang3.function.TriFunction;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class OnHurtEvent {
    private final TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> behavior;

    public OnHurtEvent(TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> behavior) {
        this.behavior = behavior;
    }

    public AttackResult executeOnHurtEvent(MobPatch<?> mobPatch, DamageSource damageSource, AttackResult result) {
        return behavior.apply(mobPatch, damageSource, result);
    }
}
