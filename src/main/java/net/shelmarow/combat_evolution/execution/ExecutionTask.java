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
    private final ExecutionHandler.ExecutionTransform transform;


    public ExecutionTask(LivingEntity executor, LivingEntity target, ExecutionTypeManager.Type executionType, ExecutionHandler.ExecutionTransform transform, int durationTicks) {
        super(durationTicks);
        this.executor = executor;
        this.target = target;
        this.executionType = executionType;
        this.transform = transform;
    }


    @Override
    public void onStart() {
        ExecutionHandler.addExecutingTarget(target, executor);

        LivingEntityPatch<?> executorPatch = EpicFightCapabilities.getEntityPatch(executor, LivingEntityPatch.class);
        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);

        executor.addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), maxTime, 1, true, false));
        target.addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), maxTime, 1, true, false));

        //玩家回满耐力值，恢复生命值
        if (executorPatch instanceof PlayerPatch<?> playerPatch) {
            playerPatch.setStamina(playerPatch.getMaxStamina());
            executor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, maxTime, 4));
        }

        if (executorPatch != null && targetPatch != null) {

            //矫正处决者和被处决目标的模型朝向
            Vec3 from = executor.getEyePosition();
            Vec3 to = target.getEyePosition();
            double dx = to.x - from.x;
            double dz = to.z - from.z;
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);

            //看向目标+偏移量
            if (executorPatch instanceof ServerPlayerPatch serverPlayerPatch) {
                serverPlayerPatch.setModelYRot(yaw + executionType.rotationOffset(), true);
            }
            else {
                executorPatch.setYRot(yaw + executionType.rotationOffset());
            }

            //旋转角度
            if (targetPatch instanceof ServerPlayerPatch serverPlayerPatch) {
                serverPlayerPatch.setModelYRot(transform.yaw(), true);
            }
            else {
                targetPatch.setYRot(transform.yaw());
            }


            //播放处决动画
            executorPatch.playAnimationSynchronized(executionType.executionAnimation(), 0F);
            targetPatch.playAnimationSynchronized(executionType.executedAnimation(), 0F);


            //是的孩子们还得二次矫正，不然位置对不上
//            if (executorPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                serverPlayerPatch.setModelYRot(yaw + executionType.rotationOffset(), true);
//            }
//            else {
//                executorPatch.setYRot(yaw + executionType.rotationOffset());
//            }
//            if (targetPatch instanceof ServerPlayerPatch serverPlayerPatch) {
//                serverPlayerPatch.setModelYRot(transform.yaw(), true);
//            }
//            else {
//                targetPatch.setYRot(transform.yaw());
//            }
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

