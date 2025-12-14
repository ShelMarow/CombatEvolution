package net.shelmarow.combat_evolution.mixins;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

@Mixin(LivingEntityPatch.class)
public abstract class EFLivingEntityPatch implements ILivingEntityData {

    //伤害源修改
    @Unique
    private static EntityDataAccessor<Boolean> combat_evolution$CAN_MODIFY_SPEED;
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$ATTACK_SPEED;
//    @Unique
//    private static EntityDataAccessor<Float> combat_evolution$DAMAGE_MULTIPLIER;
//    @Unique
//    private static EntityDataAccessor<Float> combat_evolution$IMPACT_MULTIPLIER;
//    @Unique
//    private static EntityDataAccessor<Float> combat_evolution$ARMOR_NEGATION_MULTIPLIER;
//    @Unique
//    private static EntityDataAccessor<Integer> combat_evolution$STUN_TYPE;


    //耐力修改
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$STAMINA;
    @Unique
    private static EntityDataAccessor<Integer> combat_evolution$STAMINA_STATUS;

    //战斗条件修改
    @Unique
    private static EntityDataAccessor<Integer> combat_evolution$PHASE;
    @Unique
    private static EntityDataAccessor<Boolean> combat_evolution$GUARD;
    @Unique
    private static EntityDataAccessor<Boolean> combat_evolution$IN_COUNTER;
    @Unique
    private static EntityDataAccessor<Boolean> combat_evolution$WANDER;


    @Inject(method = "initLivingEntityDataAccessor", at = @At("HEAD"), remap = false)
    private static void onInitLivingEntityDataAccessor(CallbackInfo ci) {
        combat_evolution$CAN_MODIFY_SPEED = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
        combat_evolution$ATTACK_SPEED = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
//        combat_evolution$DAMAGE_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
//        combat_evolution$IMPACT_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
//        combat_evolution$ARMOR_NEGATION_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
//        combat_evolution$STUN_TYPE = SynchedEntityData.defineId(LivingEntity.class,EntityDataSerializers.INT);

        combat_evolution$STAMINA = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        combat_evolution$STAMINA_STATUS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);

        combat_evolution$PHASE = SynchedEntityData.defineId(LivingEntity.class,EntityDataSerializers.INT);
        combat_evolution$GUARD = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
        combat_evolution$IN_COUNTER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
        combat_evolution$WANDER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    }

    @Inject(method = "createSyncedEntityData", at = @At("HEAD"), remap = false)
    private static void onCreateSyncedEntityData(LivingEntity livingentity,CallbackInfo ci) {
        livingentity.getEntityData().define(combat_evolution$CAN_MODIFY_SPEED, false);
        livingentity.getEntityData().define(combat_evolution$ATTACK_SPEED, 1.0F);
//        livingentity.getEntityData().define(combat_evolution$DAMAGE_MULTIPLIER, 1.0F);
//        livingentity.getEntityData().define(combat_evolution$IMPACT_MULTIPLIER, 1.0F);
//        livingentity.getEntityData().define(combat_evolution$ARMOR_NEGATION_MULTIPLIER, 1.0F);
//        livingentity.getEntityData().define(combat_evolution$STUN_TYPE, -1);

        livingentity.getEntityData().define(combat_evolution$STAMINA, 0.0F);
        livingentity.getEntityData().define(combat_evolution$STAMINA_STATUS, StaminaStatus.COMMON.ordinal());

        livingentity.getEntityData().define(combat_evolution$PHASE, 0);
        livingentity.getEntityData().define(combat_evolution$GUARD, false);
        livingentity.getEntityData().define(combat_evolution$IN_COUNTER, false);
        livingentity.getEntityData().define(combat_evolution$WANDER, false);
    }

    @Unique
    private LivingEntity combatEvolution$getEntity(){
        return ((LivingEntityPatch<?>) (Object) this).getOriginal();
    }
    @Unique
    private SynchedEntityData combatEvolution$getEntityData(){
        return combatEvolution$getEntity().getEntityData();
    }

    @Unique
    @Override
    public int combat_evolution$getPhase() {
        return combatEvolution$getEntityData().get(combat_evolution$PHASE);
    }

    @Unique
    @Override
    public void combat_evolution$setPhase(int phase) {
        combatEvolution$getEntityData().set(combat_evolution$PHASE,phase);
    }


    @Unique
    @Override
    public boolean combat_evolution$getCanModifySpeed() {
        return combatEvolution$getEntityData().get(combat_evolution$CAN_MODIFY_SPEED);
    }

    @Unique
    @Override
    public void combat_evolution$setCanModifySpeed(boolean canModifySpeed) {
        combatEvolution$getEntityData().set(combat_evolution$CAN_MODIFY_SPEED,canModifySpeed);
    }

    @Unique
    @Override
    public float combat_evolution$getAttackSpeed(){
        return combatEvolution$getEntityData().get(combat_evolution$ATTACK_SPEED);
    }

    @Unique
    @Override
    public void combat_evolution$setAttackSpeed(float speed) {
        combatEvolution$getEntityData().set(combat_evolution$ATTACK_SPEED,Math.max(0,speed));
    }

//    @Override
//    public float combat_evolution$getDamageMultiplier(LivingEntity entity) {
//        return entity.getEntityData().get(combat_evolution$DAMAGE_MULTIPLIER);
//    }
//
//    @Override
//    public void combat_evolution$setDamageMultiplier(LivingEntity entity, float multiplier) {
//        entity.getEntityData().set(combat_evolution$DAMAGE_MULTIPLIER,Math.max(0,multiplier));
//    }
//
//    @Override
//    public float combat_evolution$getImpactMultiplier(LivingEntity entity) {
//        return entity.getEntityData().get(combat_evolution$IMPACT_MULTIPLIER);
//    }
//
//    @Override
//    public void combat_evolution$setImpactMultiplier(LivingEntity entity, float multiplier) {
//        entity.getEntityData().set(combat_evolution$IMPACT_MULTIPLIER,Math.max(0,multiplier));
//    }
//
//    public float combat_evolution$getArmorNegationMultiplier(LivingEntity entity) {
//        return entity.getEntityData().get(combat_evolution$ARMOR_NEGATION_MULTIPLIER);
//    }
//
//    public void combat_evolution$setArmorNegationMultiplier(LivingEntity entity, float multiplier) {
//        entity.getEntityData().set(combat_evolution$ARMOR_NEGATION_MULTIPLIER,multiplier);
//    }
//
//    @Override
//    public int combat_evolution$getStunType(LivingEntity entity) {
//        return entity.getEntityData().get(combat_evolution$STUN_TYPE);
//    }
//
//    @Override
//    public void combat_evolution$setStunType(LivingEntity entity, StunType stunType) {
//        entity.getEntityData().set(combat_evolution$STUN_TYPE,stunType.ordinal());
//    }
//
//    @Override
//    public void combat_evolution$setStunType(LivingEntity entity, int stunType) {
//        entity.getEntityData().set(combat_evolution$STUN_TYPE,stunType);
//    }

    @Unique
    @Override
    public float combat_evolution$getStamina() {
        return combatEvolution$getEntityData().get(combat_evolution$STAMINA);
    }

    @Unique
    @Override
    public void combat_evolution$setStamina(float stamina) {
        LivingEntity entity = combatEvolution$getEntity();
        float maxStamina = 15;
        if (entity.getAttribute(EpicFightAttributes.MAX_STAMINA.get()) != null) {
            maxStamina = (float) entity.getAttributeValue(EpicFightAttributes.MAX_STAMINA.get());
        }
        combatEvolution$getEntityData().set(combat_evolution$STAMINA, Mth.clamp(stamina,0,maxStamina));
    }

    @Unique
    @Override
    public boolean combat_evolution$isGuard() {
        return combatEvolution$getEntityData().get(combat_evolution$GUARD);
    }

    @Unique
    @Override
    public void combat_evolution$setGuard(boolean guard) {
        combatEvolution$getEntityData().set(combat_evolution$GUARD,guard);
    }

    @Unique
    @Override
    public boolean combat_evolution$isInCounter(){
        return combatEvolution$getEntityData().get(combat_evolution$IN_COUNTER);
    }

    @Unique
    @Override
    public void combat_evolution$setInCounter(boolean counter){
        combatEvolution$getEntityData().set(combat_evolution$IN_COUNTER,counter);
    }

    @Unique
    @Override
    public boolean combat_evolution$isWander() {
        return combatEvolution$getEntityData().get(combat_evolution$WANDER);
    }

    @Unique
    @Override
    public void combat_evolution$setWander(boolean wander) {
        combatEvolution$getEntityData().set(combat_evolution$WANDER,wander);
    }

    @Unique
    @Override
    public StaminaStatus combat_evolution$getStaminaStatus() {
        int index = combatEvolution$getEntityData().get(combat_evolution$STAMINA_STATUS);
        if(index >= 0 && index < StaminaStatus.values().length){
            return StaminaStatus.values()[index];
        }
        return StaminaStatus.COMMON;
    }

    @Unique
    @Override
    public void combat_evolution$setStaminaStatus(StaminaStatus staminaStatus) {
        combatEvolution$getEntityData().set(combat_evolution$STAMINA_STATUS, staminaStatus.ordinal());
    }
}
