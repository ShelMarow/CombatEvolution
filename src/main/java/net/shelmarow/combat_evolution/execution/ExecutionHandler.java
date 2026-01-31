package net.shelmarow.combat_evolution.execution;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.iml.CustomExecuteEntity;
import net.shelmarow.combat_evolution.config.CECommonConfig;
import net.shelmarow.combat_evolution.damage_source.CEDamageTypeTags;
import net.shelmarow.combat_evolution.mixins.GuardSkillInvoker;
import net.shelmarow.combat_evolution.tickTask.TickTaskManager;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.StunType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExecutionHandler {

    //参数说明
    //Key：被处决的实体
    //value：处决者
    private static final Map<LivingEntity, LivingEntity> EXECUTION_TARGETS = new HashMap<>();
    public static final float EXECUTION_DISTANCE = 3.5F;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        //处决时，双方不受到外部伤害
        if (EXECUTION_TARGETS.containsKey(target)) {
            //目标只受到处决来源的伤害
            LivingEntity allowedAttacker = EXECUTION_TARGETS.get(target);
            if (source == null || !source.getUUID().equals(allowedAttacker.getUUID())) {
                event.setCanceled(true);
            }
        } else if (EXECUTION_TARGETS.containsValue(target)) event.setCanceled(true);

        //处决者只会对目标造成伤害
        if (source instanceof LivingEntity livingEntity) {
            if (EXECUTION_TARGETS.containsValue(livingEntity)) {
                if (!EXECUTION_TARGETS.containsKey(target))
                    event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        //处决保护，如果不是最后一击，实体会锁血，防止提前击杀
        LivingEntity target = event.getEntity();
        DamageSource damageSource = event.getSource();
        if(!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !damageSource.is(CEDamageTypeTags.EXECUTION_FINISHED) && EXECUTION_TARGETS.containsKey(target)) {
            float damageAmount = event.getAmount();
            float health = target.getHealth();
            if(damageAmount >= health){
                event.setAmount(health - 0.01F);
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        //取消处决击退
        Entity target = event.getEntity();
        if (EXECUTION_TARGETS.containsKey(target)) {
            event.setStrength(0.0F);
        }
    }


    public static void addExecutingTarget(LivingEntity target, LivingEntity executor) {
        EXECUTION_TARGETS.put(target, executor);
    }

    public static void removeExecutingTarget(LivingEntity target) {
        EXECUTION_TARGETS.remove(target);
    }

    public static boolean isExecutingTarget(LivingEntity executor, LivingEntity target) {
        return EXECUTION_TARGETS.containsKey(target) || EXECUTION_TARGETS.containsKey(executor);
    }


    public static boolean tryExecute(ServerPlayer player){
        if(!CECommonConfig.ENABLED_EXECUTION.get()) {
            //player.sendSystemMessage(Component.translatable("hud.combat_evolution.execution_disabled"));
            return false;
        }

        //获取视线上的第一个实体
        LivingEntity livingEntity = getEntityLookedAt(player,EXECUTION_DISTANCE);

        if(livingEntity != null){

            ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(livingEntity, LivingEntityPatch.class);

            //检测是否满足处决的条件
            if(targetPatch != null && playerPatch != null && playerPatch.isEpicFightMode() && playerPatch.getEntityState().canUseSkill()) {

                //判断目标是否处于破防状态
                AssetAccessor<? extends StaticAnimation> currentAnimation = Objects.requireNonNull(targetPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
                if (isTargetGuardBreak(currentAnimation, targetPatch) && canExecute(player, livingEntity, targetPatch)) {

                    //获取处决类型
                    ExecutionTypeManager.Type executionType = getExecutionType(playerPatch,targetPatch);

                    //检查是否有足够的空间进行处决,一些处决位移不一样，需要额外调整
                    Level level = player.level();
                    Vec3 frontPos = calculateExecutionPosition(livingEntity, executionType.offset());
                    frontPos = canStandHere(level, frontPos,playerPatch.getOriginal());
                    if (frontPos != null) {
                        player.teleportTo(frontPos.x, frontPos.y, frontPos.z);
                        TickTaskManager.addTask(livingEntity.getUUID(), new ExecutionTask(player, livingEntity,executionType, executionType.totalTick()));
                        return true;
                    }
                    else{
                        player.displayClientMessage(Component.translatable("text.combat_evolution.not_available_pos").withStyle(ChatFormatting.RED),true);
                    }
                }
            }
        }
        return false;
    }

    public static boolean isTargetGuardBreak(AssetAccessor<? extends StaticAnimation> currentAnimation, LivingEntityPatch<?> targetPatch) {
        return currentAnimation != null && currentAnimation != Animations.EMPTY_ANIMATION &&
                (currentAnimation == targetPatch.getHitAnimation(StunType.NEUTRALIZE) || targetPatch instanceof PlayerPatch<?> targetPlayer && currentAnimation == playerGuardBreakAnimation(targetPlayer));
    }

    public static AssetAccessor<? extends DynamicAnimation> playerGuardBreakAnimation(PlayerPatch<?> playerPatch) {
        SkillContainer skillContainer = playerPatch.getSkill(SkillSlots.GUARD);
        CapabilityItem capabilityItem = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        if(skillContainer != null){
            Skill skill = skillContainer.getSkill();
            if(skill instanceof GuardSkill guardSkill){
                return  ((GuardSkillInvoker) guardSkill).invokeGetGuardMotion(skillContainer,playerPatch,capabilityItem, GuardSkill.BlockType.GUARD_BREAK);
            }
        }
        return null;
    }

    public static ExecutionTypeManager.Type getExecutionType(LivingEntityPatch<?> executor, LivingEntityPatch<?> targetPatch) {
        //寻找是否有对应武器的处决动画
        CapabilityItem capabilityItem = executor.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        WeaponCategory weaponCategory = capabilityItem.getWeaponCategory();

        //先寻找物品
        ExecutionTypeManager.Type executionType =
                ExecutionTypeManager.getExecutionTypeByItem(executor.getOriginal().getItemInHand(InteractionHand.MAIN_HAND).getItem());

        //再寻找武器类型
        if(executionType == null){
            executionType = ExecutionTypeManager.getExecutionTypeByCategory(weaponCategory);
        }

        //优先使用自定义处决实体的动画
        if (targetPatch instanceof CustomExecuteEntity customExecuteEntity && customExecuteEntity.canUseCustomType(targetPatch)) {
            ExecutionTypeManager.Type customType = customExecuteEntity.getExecutionType();
            if(customType != null){
                executionType = customType;
            }
        }

        return executionType;
    }

    public static LivingEntity getEntityLookedAt(Player player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        AABB aabb = new AABB(eyePos, reachVec).inflate(0.2D);

        //检测视线上的第一个实体
        EntityHitResult entityHit = getEntityHitResult(
                player,eyePos,reachVec,aabb,
                entity -> entity != player && entity instanceof LivingEntity living &&
                        player.canAttack(living, TargetingConditions.forCombat()) && !living.isInvulnerable() && living.isAlive(),
                maxDistance * maxDistance,0.5
        );

        //如果存在实体
        if (entityHit != null) {
            //创建玩家到实体的向量，检测路径上是否存在遮挡
            BlockHitResult blockHit = player.level().clip(new ClipContext(eyePos,entityHit.getEntity().getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            //获取方块距离
            double blockDistanceSqr = blockHit.getType() != HitResult.Type.MISS ? eyePos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
            //获取实体距离
            double entityDistanceSqr = eyePos.distanceToSqr(entityHit.getEntity().getEyePosition());
            //如果实体距离比方块距离短，说明没有遮挡
            if (entityDistanceSqr < blockDistanceSqr && blockDistanceSqr - entityDistanceSqr > entityHit.getEntity().getBoundingBox().minX) {
                //返回获取的第一个实体
                return (LivingEntity) entityHit.getEntity();
            }
        }

        return null;
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Entity shooter, Vec3 start, Vec3 end, AABB region, Predicate<Entity> filter, double maxDistance, double expandRadius) {
        Level level = shooter.level();
        double closest = maxDistance;
        Entity hitEntity = null;
        Vec3 hitPos = null;

        for (Entity e : level.getEntities(shooter, region, filter)) {
            AABB aabb = e.getBoundingBox().inflate(e.getPickRadius() + expandRadius);

            Optional<Vec3> hit = aabb.clip(start, end);

            if (aabb.contains(start)) {
                if (closest >= 0.0D) {
                    hitEntity = e;
                    hitPos = hit.orElse(start);
                    closest = 0.0D;
                }
            } else if (hit.isPresent()) {
                Vec3 point = hit.get();
                double dist = start.distanceToSqr(point);

                if (dist < closest || closest == 0.0D) {
                    if (e.getRootVehicle() == shooter.getRootVehicle() && !e.canRiderInteract()) {
                        if (closest == 0.0D) {
                            hitEntity = e;
                            hitPos = point;
                        }
                    } else {
                        hitEntity = e;
                        hitPos = point;
                        closest = dist;
                    }
                }
            }
        }

        return hitEntity == null ? null : new EntityHitResult(hitEntity, hitPos);
    }

    public static boolean canExecute(Player player, LivingEntity entity, LivingEntityPatch<?> entityPatch) {
        return player.isAlive() && entity.isAlive() && !isExecutingTarget(player, entity) && isTargetSupported(entityPatch) &&
                isHoldingWeapon(player) && targetIsInRange(player, entity,0, EXECUTION_DISTANCE,180);
    }

    public static boolean isHoldingWeapon(Player player){
        CapabilityItem capabilityItem = EpicFightCapabilities.getItemStackCapability(player.getItemInHand(InteractionHand.MAIN_HAND));
        return capabilityItem.getWeaponCategory() != CapabilityItem.WeaponCategories.NOT_WEAPON && capabilityItem.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST;
    }

    public static boolean isTargetSupported(LivingEntityPatch<?> entityPatch) {
        if(entityPatch instanceof CustomExecuteEntity customExecuteEntity) {
            return customExecuteEntity.canBeExecuted(entityPatch);
        }
        else {
            return entityPatch.getArmature() instanceof HumanoidArmature;
        }
    }

    private static Vec3 calculateExecutionPosition(LivingEntity target, Vec3 offset) {
        float yaw = target.getYRot();
        double rad = Math.toRadians(yaw);

        // 朝向单位向量（前方）
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);

        // 右方单位向量（垂直于前方）
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);

        // 组合偏移（x=前后, z=左右, y=上下）
        double offsetX = forwardX * offset.x + rightX * offset.z;
        double offsetY = offset.y;
        double offsetZ = forwardZ * offset.x + rightZ * offset.z;

        return target.position().add(offsetX, offsetY, offsetZ);
    }

    public static boolean targetIsInRange(Player player, LivingEntity target, double minDist, double maxDist, double maxAngleDegrees) {
        Vec3 targetPos = target.position();
        Vec3 playerPos = player.position();

        double distance = playerPos.distanceTo(targetPos);
        if (distance < minDist || distance > maxDist) return false;

        float yaw = target.getYRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();
        Vec3 toPlayer = playerPos.subtract(targetPos).normalize();

        double dot = forward.dot(toPlayer);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle <= maxAngleDegrees;
    }

    public static Vec3 canStandHere(Level level, Vec3 pos, LivingEntity entity) {

        //根据实体的碰撞箱在目标位置重新构建碰撞检测AABB
        AABB entityBox = entity.getBoundingBox();
        double width = entityBox.getXsize();
        double height = entityBox.getYsize();

        //检查脚下方块是否能够站立
        for (float i = 0.5F; i >= -0.5F; i -= 0.05F) {
            BlockPos blockPosBelow = BlockPos.containing(pos.x, pos.y + i, pos.z);
            BlockState stateBelow = level.getBlockState(blockPosBelow);
            VoxelShape shapeBelow = stateBelow.getCollisionShape(level, blockPosBelow);
            if(!shapeBelow.isEmpty()) {
                double offsetY = shapeBelow.max(Direction.Axis.Y);
                AABB checkBox = new AABB(
                        pos.x - width / 2.0, blockPosBelow.getY() + offsetY, pos.z - width / 2.0,
                        pos.x + width / 2.0,blockPosBelow.getY() + offsetY + height,pos.z + width / 2.0
                );
                //根据实体的碰撞箱检测是否有足够空间站立
                if(level.noCollision(checkBox)){
                    return pos.add(0, i + 0.05, 0);
                }
            }
        }

//        //检测站立的位置是否存在方块
//        BlockPos blockPosStand = BlockPos.containing(pos.x, pos.y, pos.z);
//        BlockState stateStand = level.getBlockState(blockPosStand);
//        VoxelShape shapeStand = stateStand.getCollisionShape(level, blockPosStand);
//        //如果存在，检测高度是否小于0.5格，如果大于则不允许站立
//        double offsetY = 0;
//        if(!shapeStand.isEmpty()) {
//            double surfaceY = shapeStand.max(Direction.Axis.Y);
//            if(surfaceY <= 0.5D){
//                offsetY = surfaceY;
//            }
//        }

        return null;
    }

}
