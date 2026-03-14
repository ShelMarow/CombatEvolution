package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class TargetGuardBreak implements Condition<LivingEntityPatch<?>> {

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag tag) throws IllegalArgumentException {
        return this;
    }

    @Override
    public CompoundTag serializePredicate() {
        return new CompoundTag();
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> entityPatch) {
        LivingEntity target = entityPatch.getTarget();
        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
        if (targetPatch != null) {
            AnimationPlayer animationPlayer = targetPatch.getAnimator().getPlayerFor(null);
            if (animationPlayer != null) {
                AssetAccessor<? extends StaticAnimation> currentAnimation = animationPlayer.getRealAnimation();
                return ExecutionHandler.isTargetGuardBreak(currentAnimation, targetPatch);
            }
        }
        return false;
    }

    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen) {
        return List.of();
    }
}
