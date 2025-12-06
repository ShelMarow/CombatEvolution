package net.shelmarow.combat_evolution.ai;

import net.minecraft.world.entity.Entity;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HitEvent {
    private final BiConsumer<MobPatch<?>, Entity> behavior;

    public HitEvent(BiConsumer<MobPatch<?>,Entity> behavior) {
        this.behavior = behavior;
    }

    public void executeHitEvent(MobPatch<?> mobPatch, Entity target){
        behavior.accept(mobPatch,target);
    }
}
