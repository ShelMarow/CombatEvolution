package net.shelmarow.combat_evolution.ai.iml;

import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.StaminaStatus;

public interface ILivingEntityData {
    boolean combat_evolution$getCanModifySpeed();
    void combat_evolution$setCanModifySpeed(boolean canModifySpeed);

    float combat_evolution$getAttackSpeed();
    void combat_evolution$setAttackSpeed(float attackSpeed);

    float combat_evolution$getStamina();
    float combat_evolution$getStamina(LivingEntity entity);
    void combat_evolution$setStamina(float stamina);
    void combat_evolution$setStamina(LivingEntity entity, float stamina);

    StaminaStatus combat_evolution$getStaminaStatus();
    void combat_evolution$setStaminaStatus(StaminaStatus staminaStatus);

    int combat_evolution$getPhase();
    void combat_evolution$setPhase(int Phase);

    boolean combat_evolution$isGuard();
    void combat_evolution$setGuard(boolean guard);

    boolean combat_evolution$isInCounter();
    void combat_evolution$setInCounter(boolean counter);

    boolean combat_evolution$isWander();
    void combat_evolution$setWander(boolean wander);
}
