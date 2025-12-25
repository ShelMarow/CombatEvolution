package net.shelmarow.combat_evolution.execution;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.tickTask.TickTask;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public class ExecutionTask extends TickTask {
    private final LivingEntity executor;
    private final LivingEntity target;
    private final ExecutionTypeManager.Type executionType;


    public ExecutionTask(LivingEntity executor, LivingEntity target,ExecutionTypeManager.Type executionType, int durationTicks) {
        super(durationTicks);
        this.executor = executor;
        this.target = target;
        this.executionType = executionType;
    }


    @Override
    public void onStart() {
        ExecutionHandler.addExecutingTarget(target, executor);

        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(executor, LivingEntityPatch.class);
        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);

        executor.addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 100, 1, true, false));
        target.addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 100, 1, true, false));

        //玩家回满耐力值，恢复生命值
        if (entityPatch instanceof PlayerPatch<?> playerPatch) {
            playerPatch.setStamina(playerPatch.getMaxStamina());
            executor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 4));
        }

        //只有人形生物和自定义的生物能播放
        if (entityPatch != null && targetPatch != null && ExecutionHandler.isTargetSupported(entityPatch) && ExecutionHandler.isTargetSupported(targetPatch)) {
            //播放处决动画
            entityPatch.playAnimationSynchronized(executionType.executionAnimation(), 0F);
            targetPatch.playAnimationSynchronized(executionType.executedAnimation(), 0F);

            //矫正处决者的模型朝向
            Vec3 from = executor.getEyePosition();
            Vec3 to = target.getEyePosition();
            double dx = to.x - from.x;
            double dz = to.z - from.z;
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F) + executionType.rotationOffset();

            if (entityPatch instanceof ServerPlayerPatch serverPlayerPatch) {
                serverPlayerPatch.setModelYRot(yaw, true);
            } else {
                entityPatch.setYRot(yaw);
            }
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onFinish() {
        ExecutionHandler.removeExecutingTarget(target);
    }

}

