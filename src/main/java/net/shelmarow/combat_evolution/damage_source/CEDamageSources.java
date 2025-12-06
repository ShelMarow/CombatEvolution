package net.shelmarow.combat_evolution.damage_source;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public class CEDamageSources {
    private final Registry<DamageType> damageTypes;

    public CEDamageSources(LivingEntity livingEntity) {
        this.damageTypes = livingEntity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
    }

    public DamageSource getExecutionDamageSource() {
        return new DamageSource(this.damageTypes.getHolderOrThrow(CEDamageTypes.EXECUTION));
    }
}
