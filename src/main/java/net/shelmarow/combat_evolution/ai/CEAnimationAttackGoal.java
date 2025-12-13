package net.shelmarow.combat_evolution.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;

public class CEAnimationAttackGoal<T extends MobPatch<?>> extends Goal {
    protected final T mobpatch;
    protected final CECombatBehaviors<T> combatBehaviors;
    protected boolean actionStoped = false;

    public CEAnimationAttackGoal(T mobpatch, CECombatBehaviors<T> combatBehaviors){
        this.mobpatch = mobpatch;
        this.combatBehaviors = combatBehaviors;
    }

    @Override
    public boolean canUse() {
        this.actionStoped = combatBehaviors.getCurrentBehavior() != null && !mobpatch.getEntityState().inaction();
        return this.checkTargetValid() || this.actionStoped;
    }

    public CECombatBehaviors<T> getCombatBehaviors(){
        return this.combatBehaviors;
    }

    public void interruptByStun(StunType stunType){
        CECombatBehaviors.Behavior<?> currentBehavior = combatBehaviors.getCurrentBehavior();
        if(currentBehavior != null && currentBehavior.stopByStun(stunType)){
            clearCurrentBehavior(currentBehavior);
        }
    }

    public void clearCurrentBehavior(CECombatBehaviors.Behavior<?> currentBehavior){
        currentBehavior.behaviorFinished();
        currentBehavior.resetAllCooldown();
        combatBehaviors.clearCurrentBehavior();
        ILivingEntityData entityData = (ILivingEntityData) mobpatch;
        entityData.combat_evolution$setWander(mobpatch.getOriginal(), false);
        entityData.combat_evolution$setGuard(mobpatch.getOriginal(), false);
        entityData.combat_evolution$setInCounter(mobpatch.getOriginal(), false);
    }

    @Override
    public void tick() {
        CECombatBehaviors.Behavior<?> currentBehavior = combatBehaviors.getCurrentBehavior();
        //丢失目标结束行为
        if(mobpatch.getTarget() == null){
            if (currentBehavior != null) {
                clearCurrentBehavior(currentBehavior);
            }
        }
        else {
            this.combatBehaviors.tick(mobpatch);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    protected boolean checkTargetValid() {
        LivingEntity livingentity = this.mobpatch.getTarget();
        if (livingentity == null) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else {
            if (livingentity instanceof Player player) {
                return !livingentity.isSpectator() && !player.isCreative();
            }
            return true;
        }
    }
}

