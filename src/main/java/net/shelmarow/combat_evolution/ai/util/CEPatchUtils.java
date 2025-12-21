package net.shelmarow.combat_evolution.ai.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.ai.iml.ILivingEntityData;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

public class CEPatchUtils {

    public static void setPlaySpeed(LivingEntityPatch<?> entityPatch,float speed){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        entityData.combat_evolution$setCanModifySpeed(true);
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

    public static void addStamina(LivingEntityPatch<?> entityPatch,float stamina){
        setStamina(entityPatch,getStamina(entityPatch) + stamina);
    }

    public static float getMaxStamina(LivingEntityPatch<?> entityPatch){
        AttributeInstance instance = entityPatch.getOriginal().getAttribute(EpicFightAttributes.MAX_STAMINA.get());
        if(instance != null){
            return (float) instance.getValue();
        }
        return 15F;
    }

    public static float getStaminaPercent(LivingEntityPatch<?> entityPatch){
        float stamina = getStamina(entityPatch);
        float maxStamina = getMaxStamina(entityPatch);
        return Mth.clamp(stamina / maxStamina,0,1F);
    }

    public static void setPhase(LivingEntityPatch<?> entityPatch, int phase){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        entityData.combat_evolution$setPhase(phase);
    }

    public static int getPhase(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getPhase();
    }

    public static void addPhase(LivingEntityPatch<?> entityPatch, int phase){
        setPhase(entityPatch, getPhase(entityPatch) + phase);
    }

    public static StaminaStatus getStaminaStatus(LivingEntityPatch<?> entityPatch){
        ILivingEntityData entityData = (ILivingEntityData) entityPatch;
        return entityData.combat_evolution$getStaminaStatus();
    }
}
