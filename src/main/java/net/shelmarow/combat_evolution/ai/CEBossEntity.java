package net.shelmarow.combat_evolution.ai;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.bossbar.CEBossEvent;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public abstract class CEBossEntity extends PathfinderMob {

    protected final CEBossEvent ceBossEvent = new CEBossEvent(getDisplayName());
    protected LivingEntityPatch<?> cePatch = null;
    private float bossBarHealth = Float.NaN;
    private float bossBarMaxHealth = Float.NaN;

    protected CEBossEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        super.tick();
        if(!level().isClientSide()){
            setBossBarHealth();
            setBossBarStamina();
        }
    }


    protected void setBossBarHealth() {
        float maxHealth = Math.max(this.getMaxHealth(), 1.0F);
        float health = Math.max(this.getHealth(), 0.0F);
        this.ceBossEvent.setProgress(health / maxHealth);

        if (Float.compare(this.bossBarHealth, health) != 0 || Float.compare(this.bossBarMaxHealth, maxHealth) != 0) {
            CompoundTag customData = this.ceBossEvent.getCustomData();
            customData.putFloat("health", health);
            customData.putFloat("max_health", maxHealth);
            this.ceBossEvent.updateCustomData(customData);
            this.bossBarHealth = health;
            this.bossBarMaxHealth = maxHealth;
        }
    }

    protected void setBossBarStamina() {
        if(cePatch == null) {
            cePatch = EpicFightCapabilities.getEntityPatch(this, LivingEntityPatch.class);
        }
        if (cePatch != null) {
            ceBossEvent.setStaminaStatus(CEPatchUtils.getStaminaStatus(cePatch));
            ceBossEvent.setStamina(CEPatchUtils.getStaminaPercent(cePatch));
        }
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        this.ceBossEvent.addPlayer(pPlayer);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        this.ceBossEvent.removePlayer(pPlayer);
    }

}
