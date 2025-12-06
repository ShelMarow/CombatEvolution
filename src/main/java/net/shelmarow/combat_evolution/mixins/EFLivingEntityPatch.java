package net.shelmarow.combat_evolution.mixins;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

import java.util.HashSet;
import java.util.Set;

@Mixin(LivingEntityPatch.class)
public abstract class EFLivingEntityPatch implements ILivingEntityData {

    //伤害源修改
    @Unique
    private static EntityDataAccessor<Boolean> combat_evolution$CAN_MODIFY_SPEED;
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$ATTACK_SPEED;
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$DAMAGE_MULTIPLIER;
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$IMPACT_MULTIPLIER;
    @Unique
    private static EntityDataAccessor<Float> combat_evolution$ARMOR_NEGATION_MULTIPLIER;
    @Unique
    private static EntityDataAccessor<Integer> combat_evolution$STUN_TYPE;
//    @Unique
//    private static final Set<TagKey<DamageType>> combatEvolution$DAMAGE_SOURCE = new HashSet<>();

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
        combat_evolution$DAMAGE_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        combat_evolution$IMPACT_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        combat_evolution$ARMOR_NEGATION_MULTIPLIER = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
        combat_evolution$STUN_TYPE = SynchedEntityData.defineId(LivingEntity.class,EntityDataSerializers.INT);

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
        livingentity.getEntityData().define(combat_evolution$DAMAGE_MULTIPLIER, 1.0F);
        livingentity.getEntityData().define(combat_evolution$IMPACT_MULTIPLIER, 1.0F);
        livingentity.getEntityData().define(combat_evolution$ARMOR_NEGATION_MULTIPLIER, 1.0F);
        livingentity.getEntityData().define(combat_evolution$STUN_TYPE, -1);

        livingentity.getEntityData().define(combat_evolution$STAMINA, 0.0F);
        livingentity.getEntityData().define(combat_evolution$STAMINA_STATUS, StaminaStatus.COMMON.ordinal());

        livingentity.getEntityData().define(combat_evolution$PHASE, 0);
        livingentity.getEntityData().define(combat_evolution$GUARD, false);
        livingentity.getEntityData().define(combat_evolution$IN_COUNTER, false);
        livingentity.getEntityData().define(combat_evolution$WANDER, false);
    }


    @Unique
    public int combat_evolution$getPhase(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$PHASE);
    }

    @Unique
    public void combat_evolution$setPhase(LivingEntity entity, int phase) {
        entity.getEntityData().set(combat_evolution$PHASE,phase);
    }


    @Unique
    public boolean combat_evolution$getCanModifySpeed(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$CAN_MODIFY_SPEED);
    }

    @Unique
    public void combat_evolution$setCanModifySpeed(LivingEntity entity, boolean canModifySpeed) {
        entity.getEntityData().set(combat_evolution$CAN_MODIFY_SPEED,canModifySpeed);
    }

    @Unique
    public float combat_evolution$getAttackSpeed(LivingEntity entity){
        return entity.getEntityData().get(combat_evolution$ATTACK_SPEED);
    }

    @Unique
    public void combat_evolution$setAttackSpeed(LivingEntity entity, float speed) {
        entity.getEntityData().set(combat_evolution$ATTACK_SPEED,Math.max(0,speed));
    }

    @Override
    public float combat_evolution$getDamageMultiplier(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$DAMAGE_MULTIPLIER);
    }

    @Override
    public void combat_evolution$setDamageMultiplier(LivingEntity entity, float multiplier) {
        entity.getEntityData().set(combat_evolution$DAMAGE_MULTIPLIER,Math.max(0,multiplier));
    }

    @Override
    public float combat_evolution$getImpactMultiplier(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$IMPACT_MULTIPLIER);
    }

    @Override
    public void combat_evolution$setImpactMultiplier(LivingEntity entity, float multiplier) {
        entity.getEntityData().set(combat_evolution$IMPACT_MULTIPLIER,Math.max(0,multiplier));
    }

    public float combat_evolution$getArmorNegationMultiplier(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$ARMOR_NEGATION_MULTIPLIER);
    }

    public void combat_evolution$setArmorNegationMultiplier(LivingEntity entity, float multiplier) {
        entity.getEntityData().set(combat_evolution$ARMOR_NEGATION_MULTIPLIER,multiplier);
    }

    @Override
    public int combat_evolution$getStunType(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$STUN_TYPE);
    }

    @Override
    public void combat_evolution$setStunType(LivingEntity entity, StunType stunType) {
        entity.getEntityData().set(combat_evolution$STUN_TYPE,stunType.ordinal());
    }

    @Override
    public void combat_evolution$setStunType(LivingEntity entity, int stunType) {
        entity.getEntityData().set(combat_evolution$STUN_TYPE,stunType);
    }

//    public Set<TagKey<DamageType>> combat_evolution$getDamageSource() {
//        return combatEvolution$DAMAGE_SOURCE;
//    }
//
//    public void combat_evolution$setDamageSource(Set<TagKey<DamageType>> sourceSet) {
//        combatEvolution$DAMAGE_SOURCE.clear();
//        if (!sourceSet.isEmpty()) {
//            combatEvolution$DAMAGE_SOURCE.addAll(sourceSet);
//        }
//    }

    @Unique
    public float combat_evolution$getStamina(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$STAMINA);
    }

    @Unique
    public void combat_evolution$setStamina(LivingEntity entity, float stamina) {
        float maxStamina = 15;
        if (entity.getAttribute(EpicFightAttributes.MAX_STAMINA.get()) != null) {
            maxStamina = (float) entity.getAttributeValue(EpicFightAttributes.MAX_STAMINA.get());
        }
        entity.getEntityData().set(combat_evolution$STAMINA, Mth.clamp(stamina,0,maxStamina));
    }

    @Unique
    public boolean combat_evolution$isGuard(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$GUARD);
    }

    @Unique
    public void combat_evolution$setGuard(LivingEntity entity, boolean guard) {
        entity.getEntityData().set(combat_evolution$GUARD,guard);
    }

    @Unique
    public boolean combat_evolution$isInCounter(LivingEntity entity){
        return  entity.getEntityData().get(combat_evolution$IN_COUNTER);
    }

    @Unique
    public void combat_evolution$setInCounter(LivingEntity entity, boolean counter){
        entity.getEntityData().set(combat_evolution$IN_COUNTER,counter);
    }

    @Unique
    public boolean combat_evolution$isWander(LivingEntity entity) {
        return entity.getEntityData().get(combat_evolution$WANDER);
    }

    @Unique
    public void combat_evolution$setWander(LivingEntity entity, boolean wander) {
        entity.getEntityData().set(combat_evolution$WANDER,wander);
    }

    @Unique
    public StaminaStatus combat_evolution$getStaminaStatus(LivingEntity entity) {
        int index = entity.getEntityData().get(combat_evolution$STAMINA_STATUS);
        if(index >= 0 && index < StaminaStatus.values().length){
            return StaminaStatus.values()[index];
        }
        return StaminaStatus.COMMON;
    }

    @Unique
    public void combat_evolution$setStaminaStatus(LivingEntity entity, StaminaStatus staminaStatus) {
        entity.getEntityData().set(combat_evolution$STAMINA_STATUS, staminaStatus.ordinal());
    }
}
