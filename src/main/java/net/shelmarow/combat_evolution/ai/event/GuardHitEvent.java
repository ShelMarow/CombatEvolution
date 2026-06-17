package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.damagesource.DamageSource;
import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class GuardHitEvent implements CEMobEvent<GuardHitEvent.EventParams> {
    private final BiConsumer<MobPatch<?>, DamageSource> behavior;

    public GuardHitEvent(BiConsumer<MobPatch<?>, DamageSource> behavior) {
        this.behavior = behavior;
    }

    public void executeGuardHitEvent(MobPatch<?> mobPatch, DamageSource damageSource){
        this.behavior.accept(mobPatch, damageSource);
    }

    @Override
    public void execute(EventParams param) {
        executeGuardHitEvent(param.mobPatch, param.damageSource);
    }

    public record EventParams(MobPatch<?> mobPatch, DamageSource damageSource){}
}
