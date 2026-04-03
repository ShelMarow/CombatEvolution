package net.shelmarow.combat_evolution.ai.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;

public class GuardHitEvent {
    private final BiConsumer<MobPatch<?>, DamageSource> behavior;

    public GuardHitEvent(BiConsumer<MobPatch<?>, DamageSource> behavior) {
        this.behavior = behavior;
    }

    public void executeGuardHitEvent(MobPatch<?> mobPatch, DamageSource damageSource){
        this.behavior.accept(mobPatch, damageSource);
    }
}
