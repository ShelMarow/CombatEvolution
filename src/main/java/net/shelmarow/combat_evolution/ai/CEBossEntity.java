package net.shelmarow.combat_evolution.ai;

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

    private final CEBossEvent ceBossEvent = new CEBossEvent(getDisplayName());

    protected CEBossEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }


    @Override
    public void tick() {
        super.tick();
        this.ceBossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(this,LivingEntityPatch.class);
        if (entityPatch != null) {
            ceBossEvent.setStaminaStatus(CEPatchUtils.getStaminaStatus(entityPatch));
            ceBossEvent.setStamina(CEPatchUtils.getStaminaPercent(entityPatch));
        }
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        this.ceBossEvent.addPlayer(pPlayer);
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        this.ceBossEvent.removePlayer(pPlayer);
    }

}
