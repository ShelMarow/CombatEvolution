package net.shelmarow.combat_evolution.mixins;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.CEExpandedEntityDataAccessors;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.ai.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;

@Mixin(LivingEntityPatch.class)
public abstract class EFLivingEntityPatchMixin implements ILivingEntityData {


    @Inject(method = "registerExpandedEntityDataAccessors", at = @At("TAIL"), remap = false)
    private void onRegisterExpandedEntityDataAccessors(ExpandedSyncedData expandedSyncedData, CallbackInfo ci) {
        //伤害源修改
        expandedSyncedData.register(CEExpandedEntityDataAccessors.CAN_MODIFY_SPEED);
        expandedSyncedData.register(CEExpandedEntityDataAccessors.ATTACK_SPEED);

        //耐力修改
        expandedSyncedData.register(CEExpandedEntityDataAccessors.STAMINA);
        expandedSyncedData.register(CEExpandedEntityDataAccessors.STAMINA_STATUS);

        //战斗条件修改
        expandedSyncedData.register(CEExpandedEntityDataAccessors.PHASE);
        expandedSyncedData.register(CEExpandedEntityDataAccessors.GUARD);
        expandedSyncedData.register(CEExpandedEntityDataAccessors.IN_COUNTER);
        expandedSyncedData.register(CEExpandedEntityDataAccessors.WANDER);
    }

    @Unique
    private LivingEntity combatEvolution$getEntity(){
        return ((LivingEntityPatch<?>) (Object) this).getOriginal();
    }

    @Unique
    private ExpandedSyncedData combatEvolution$getEntityData(){
        return ((LivingEntityPatch<?>) (Object) this).getExpandedSynchedData();
    }


    @Override
    public int combat_evolution$getPhase() {
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.PHASE);
    }

    @Override
    public void combat_evolution$setPhase(int phase) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.PHASE,phase);
    }

    @Override
    public boolean combat_evolution$getCanModifySpeed() {
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.CAN_MODIFY_SPEED);
    }

    @Override
    public void combat_evolution$setCanModifySpeed(boolean canModifySpeed) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.CAN_MODIFY_SPEED,canModifySpeed);
    }

    @Override
    public float combat_evolution$getAttackSpeed(){
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.ATTACK_SPEED);
    }

    @Override
    public void combat_evolution$setAttackSpeed(float speed) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.ATTACK_SPEED,Math.max(0,speed));
    }

    @Override
    public float combat_evolution$getStamina() {
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.STAMINA);
    }

    @Override
    public float combat_evolution$getStamina(LivingEntity entity) {
        return combat_evolution$getStamina();
    }


    @Override
    public void combat_evolution$setStamina(float stamina) {
        LivingEntity entity = combatEvolution$getEntity();
        float maxStamina = 15;
        if (entity.getAttribute(EpicFightAttributes.MAX_STAMINA) != null) {
            maxStamina = (float) entity.getAttributeValue(EpicFightAttributes.MAX_STAMINA);
        }
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.STAMINA, Mth.clamp(stamina,0,maxStamina));
    }

    @Override
    public void combat_evolution$setStamina(LivingEntity entity, float stamina) {
        combat_evolution$setStamina(stamina);
    }

    @Override
    public boolean combat_evolution$isGuard() {
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.GUARD);
    }

    @Override
    public void combat_evolution$setGuard(boolean guard) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.GUARD,guard);
    }

    @Override
    public boolean combat_evolution$isInCounter(){
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.IN_COUNTER);
    }

    @Override
    public void combat_evolution$setInCounter(boolean counter){
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.IN_COUNTER,counter);
    }

    @Override
    public boolean combat_evolution$isWander() {
        return combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.WANDER);
    }

    @Override
    public void combat_evolution$setWander(boolean wander) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.WANDER,wander);
    }

    @Override
    public StaminaStatus combat_evolution$getStaminaStatus() {
        int index = combatEvolution$getEntityData().get(CEExpandedEntityDataAccessors.STAMINA_STATUS);
        if(index >= 0 && index < StaminaStatus.values().length){
            return StaminaStatus.values()[index];
        }
        return StaminaStatus.COMMON;
    }

    @Override
    public void combat_evolution$setStaminaStatus(StaminaStatus staminaStatus) {
        combatEvolution$getEntityData().set(CEExpandedEntityDataAccessors.STAMINA_STATUS, staminaStatus.ordinal());
    }
}
