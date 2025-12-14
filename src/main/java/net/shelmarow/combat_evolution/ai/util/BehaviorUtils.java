package net.shelmarow.combat_evolution.ai.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.shelmarow.combat_evolution.ai.goal.CEAnimationAttackGoal;
import net.shelmarow.combat_evolution.ai.CECombatBehaviors;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.PhaseParams;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BehaviorUtils {

    public static CECombatBehaviors<?> getCECombatBehaviors(LivingEntityPatch<?> entityPatch){
        if (entityPatch == null || entityPatch.getOriginal() == null) return null;
        if(entityPatch.isInitialized()) {
            if (entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
                return ceHumanoidPatch.getOriginal().goalSelector.getAvailableGoals().stream()
                        .filter(g -> g.getGoal() instanceof CEAnimationAttackGoal<?>)
                        .map(g -> ((CEAnimationAttackGoal<?>) g.getGoal()).getCombatBehaviors()).findFirst().orElse(null);
            }
        }
        return null;
    }

    public static CECombatBehaviors.Behavior<?> getCurrentBehavior(LivingEntityPatch<?> entityPatch) {
        if (entityPatch == null || entityPatch.getOriginal() == null) return null;
        if(entityPatch.isInitialized()) {
            if (entityPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
                return ceHumanoidPatch.getOriginal().goalSelector.getAvailableGoals().stream()
                        .filter(g -> g.getGoal() instanceof CEAnimationAttackGoal<?>)
                        .map(g -> ((CEAnimationAttackGoal<?>) g.getGoal()).getCombatBehaviors())
                        .filter(Objects::nonNull).map(CECombatBehaviors::getCurrentBehavior)
                        .filter(Objects::nonNull).findFirst().orElse(null);
            }
        }
        return null;
    }

    public static boolean onGuardHit(LivingEntityPatch<?> entityPatch) {
        CECombatBehaviors.Behavior<?> b = getCurrentBehavior(entityPatch);
        if (b != null) return b.whenGuardHit();
        return false;
    }

    public static void stopCurrentBehavior(LivingEntity entity) {
        if(entity instanceof Mob mob) {
            mob.goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof CEAnimationAttackGoal<?>)
                    .forEach(goal -> {
                        if (goal.getGoal() instanceof CEAnimationAttackGoal<?> animationAttackGoal) {
                            CECombatBehaviors.Behavior<?> behavior = animationAttackGoal.getCombatBehaviors().getCurrentBehavior();
                            if (behavior != null) animationAttackGoal.clearCurrentBehavior(behavior);
                        }
                    });
        }
    }

    public static Map<Integer, PhaseParams> getPhaseParams(LivingEntityPatch<?> entitypatch) {
        CECombatBehaviors.Behavior<?> current = getCurrentBehavior(entitypatch);
        return current != null && current.canApplyPhaseParams() ? current.getPhaseParams() : new HashMap<>();
    }
}
