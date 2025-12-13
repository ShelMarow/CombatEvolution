package net.shelmarow.combat_evolution.execution;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.iml.CustomExecuteEntity;
import net.shelmarow.combat_evolution.tickTask.TickTaskManager;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
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

    private static final Map<LivingEntity, LivingEntity> executingTargets = new HashMap<>();
    public static final float EXECUTION_DISTANCE = 3.5F;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        //处决时，双方不受到外部伤害
        if (executingTargets.containsKey(target)) {
            //目标只受到处决来源的伤害
            LivingEntity allowedAttacker = executingTargets.get(target);
            if (source == null || !source.getUUID().equals(allowedAttacker.getUUID())) {
                event.setCanceled(true);
            }
        } else if (executingTargets.containsValue(target)) event.setCanceled(true);

        //处决者只会对目标造成伤害
        if (source instanceof LivingEntity livingEntity) {
            if (executingTargets.containsValue(livingEntity)) {
                if (!executingTargets.containsKey(target))
                    event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        //取消处决击退
        Entity target = event.getEntity();
        if (executingTargets.containsKey(target)) {
            event.setStrength(0.0F);
        }
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
//        if (!event.getSide().isServer()) return;
//        Entity entity = event.getTarget();
//        ServerPlayer player = (ServerPlayer) event.getEntity();
//
//        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
//        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);
//
//        if(entityPatch != null && playerPatch != null && playerPatch.getEntityState().canUseSkill()) {
//            AssetAccessor<? extends DynamicAnimation> stunAnimation = Objects.requireNonNull(entityPatch.getAnimator().getPlayerFor(null)).getAnimation();
//            if (entity instanceof LivingEntity livingEntity && canExecuteFont(player, livingEntity, entityPatch)) {
//                if (stunAnimation != null && stunAnimation == entityPatch.getHitAnimation(StunType.NEUTRALIZE)) {
//                    if (placePlayerInFront(player, livingEntity, 1.35F)) {
//                        TickTaskManager.addTask(entity.getUUID(), new Execution(player, livingEntity, 100, true));
//                    }
//                }
//            }
//        }
    }

    public static boolean tryExecute(ServerPlayer player){
        //获取视线上的第一个实体
        Entity entity = getEntityLookedAt(player,EXECUTION_DISTANCE);

        if(entity != null){

            ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);

            //检测是否满足处决的条件
            if(targetPatch != null && playerPatch != null && playerPatch.isEpicFightMode() && playerPatch.getEntityState().canUseSkill()) {

                //首先判断目标是否处于破防状态
                AssetAccessor<? extends DynamicAnimation> stunAnimation = Objects.requireNonNull(targetPatch.getAnimator().getPlayerFor(null)).getAnimation();
                if (stunAnimation != null && stunAnimation == targetPatch.getHitAnimation(StunType.NEUTRALIZE)) {

                    //检测其他条件
                    if (entity instanceof LivingEntity livingEntity && canExecute(player, livingEntity, targetPatch)) {

                        //获取处决类型
                        ExecutionTypeManager.Type executionType = getExecutionType(playerPatch,targetPatch);

                        //检查是否有足够的空间进行处决,一些处决位移不一样，需要额外调整
                        Level level = player.level();
                        Vec3 frontPos = calculateExecutionPosition(livingEntity, executionType.getOffset());
                        if (canStandHere(level, frontPos,playerPatch.getOriginal())) {
                            player.teleportTo(frontPos.x, frontPos.y, frontPos.z);
                            TickTaskManager.addTask(entity.getUUID(), new ExecutionTask(player, livingEntity,executionType, 100));
                            return true;
                        }
                        else{
                            player.displayClientMessage(Component.translatable("text.combat_evolution.not_available_pos").withStyle(ChatFormatting.RED),true);
                        }

                    }
                }
            }
        }
        return false;
    }

    public static ExecutionTypeManager.Type getExecutionType(LivingEntityPatch<?> executor, LivingEntityPatch<?> targetPatch) {
        ExecutionTypeManager.Type executionType = ExecutionTypeManager.DEFAULT_TYPE;
        //优先使用自定义处决实体的动画
        if (targetPatch instanceof CustomExecuteEntity customExecuteEntity) {
            ExecutionTypeManager.Type type = customExecuteEntity.getExecutionType();
            if(type != null){
                executionType = type;
            }
        }
        else {
            //寻找是否有对应武器的处决动画
            CapabilityItem capabilityItem = targetPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
            if (capabilityItem != null) {
                ExecutionTypeManager.Type type = ExecutionTypeManager.getExecutionType(executor.getOriginal().getItemInHand(InteractionHand.MAIN_HAND));
                if(type == null){
                    WeaponCategory weaponCategory = capabilityItem.getWeaponCategory();
                    type = ExecutionTypeManager.getExecutionType(weaponCategory);
                }
                if(type != null){
                    executionType = type;
                }
            }
        }
        return executionType;
    }

    public static Entity getEntityLookedAt(Player player, double maxDistance) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 reachVec = eyePos.add(lookVec.scale(maxDistance));

        AABB aabb = new AABB(eyePos, reachVec).inflate(0.2D);

        //检测视线上的第一个实体
        EntityHitResult entityHit = getEntityHitResult(
                player,eyePos,reachVec,aabb,
                entity -> entity != player && !entity.isSpectator() && entity.isAlive() && entity instanceof LivingEntity,
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
                return entityHit.getEntity();
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
        return player.isAlive() && entity.isAlive() && isExecutingTarget(player, entity) && isTargetPatchSupported(entityPatch) &&
                isHoldingWeapon(player) && targetIsInRange(player, entity,0, EXECUTION_DISTANCE,180);
    }

    public static boolean isHoldingWeapon(Player player){
        CapabilityItem capabilityItem = EpicFightCapabilities.getItemStackCapability(player.getItemInHand(InteractionHand.MAIN_HAND));
        return capabilityItem.getWeaponCategory() != CapabilityItem.WeaponCategories.NOT_WEAPON && capabilityItem.getWeaponCategory() != CapabilityItem.WeaponCategories.FIST;
    }

    public static boolean isTargetPatchSupported(LivingEntityPatch<?> entityPatch) {
        if(entityPatch instanceof CustomExecuteEntity customExecuteEntity) {
            return customExecuteEntity.canBeExecuted(entityPatch);
        }
        else {
            return entityPatch instanceof CEHumanoidPatch || entityPatch instanceof PlayerPatch || entityPatch instanceof HumanoidMobPatch;
        }
    }

    public static void addExecutingTarget(LivingEntity target, LivingEntity executor) {
        executingTargets.put(target, executor);
    }

    public static void removeExecutingTarget(LivingEntity target) {
        executingTargets.remove(target);
    }

    public static boolean isExecutingTarget(LivingEntity executor, LivingEntity target) {
        return !executingTargets.containsKey(target) && !executingTargets.containsKey(executor);
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

    public static boolean canStandHere(Level level, Vec3 pos, LivingEntity entity) {

        //根据实体的碰撞箱在目标位置重新构建碰撞检测AABB
        AABB entityBox = entity.getBoundingBox();
        double width = entityBox.getXsize();
        double height = entityBox.getYsize();

        //检查脚下方块是否能够站立
        boolean solidBelow = false;
        BlockPos blockPosBelow = BlockPos.containing(pos.x, pos.y - 1, pos.z);
        BlockState stateBelow = level.getBlockState(blockPosBelow);
        VoxelShape shapeBelow = stateBelow.getCollisionShape(level, blockPosBelow);
        if(!shapeBelow.isEmpty()) {
            double surfaceY = shapeBelow.max(Direction.Axis.Y) + blockPosBelow.getY();
            solidBelow =  Math.abs(pos.y - surfaceY) <= 0.5D;
        }

        //检测站立的位置是否存在方块
        BlockPos blockPosStand = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockState stateStand = level.getBlockState(blockPosStand);
        VoxelShape shapeStand = stateStand.getCollisionShape(level, blockPosStand);
        //如果存在，检测高度是否小于0.5格，如果大于则不允许站立
        double offsetY = 0;
        if(!shapeStand.isEmpty()) {
            double surfaceY = shapeStand.max(Direction.Axis.Y);
            if(surfaceY <= 0.5D){
                offsetY = surfaceY;
            }
        }

        AABB checkBox = new AABB(
                pos.x - width / 2.0, pos.y + offsetY, pos.z - width / 2.0,
                pos.x + width / 2.0,pos.y + offsetY + height,pos.z + width / 2.0
        );

        //根据实体的碰撞箱检测是否有足够空间站立
        boolean spaceFree = level.noCollision(checkBox);

        return solidBelow && spaceFree;
    }

}
