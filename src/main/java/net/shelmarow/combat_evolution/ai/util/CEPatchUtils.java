package net.shelmarow.combat_evolution.ai.util;

import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class CEPatchUtils {

    public static void setPlaySpeed(LivingEntityPatch<?> entityPatch,float speed){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        entityData.combat_evolution$setAttackSpeed(speed);
    }

    public static float getPlaySpeed(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getAttackSpeed();
    }

    public static void setStamina(LivingEntityPatch<?> entityPatch,float stamina){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        entityData.combat_evolution$setStamina(stamina);
    }

    public static float getStamina(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getStamina();
    }

    public static void setPhase(LivingEntityPatch<?> entityPatch,int phase){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        entityData.combat_evolution$setPhase(phase);
    }

    public static int getPhase(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getPhase();
    }

    public static StaminaStatus getStaminaStatus(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getStaminaStatus();
    }
}
