package net.shelmarow.combat_evolution.ai.goal;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;

public class CommonChasingGoal extends Goal {
    protected final MobPatch<? extends PathfinderMob> mobpatch;
    protected final Mob mob;
    protected final double attackRadius;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int failedPathFindingPenalty = 0;
    private final boolean canPenalize = false;
    private long lastCanUseCheck;

    public CommonChasingGoal(MobPatch<? extends PathfinderMob> mobpatch) {
        this(mobpatch, 0.0F);
    }

    public CommonChasingGoal(MobPatch<? extends PathfinderMob> mobpatch, double attackRadius) {
        this.mobpatch = mobpatch;
        this.mob = mobpatch.getOriginal();
        this.attackRadius = attackRadius;
        this.speedModifier = 1;
        this.followingTargetEvenIfNotSeen = false;
    }

    public boolean canUse() {
        if(this.mobpatch.getEntityState().inaction()) return false;
        long i = this.mob.level().getGameTime();
        if (i - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = i;
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else if (this.canPenalize) {
                if (--this.ticksUntilNextPathRecalculation <= 0) {
                    this.path = this.mob.getNavigation().createPath(livingentity, 0);
                    this.ticksUntilNextPathRecalculation = 4 + mob.getRandom().nextInt(7);
                    return this.path != null;
                } else {
                    return true;
                }
            } else {
                this.path = this.mob.getNavigation().createPath(livingentity, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                }
            }
        }
    }

    public boolean canContinueToUse() {
        if(this.mobpatch.getEntityState().inaction()) return false;
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null) {
            return false;
        } else if (!livingentity.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else if (!this.mob.isWithinRestriction(livingentity.blockPosition())) {
            return false;
        } else {
            return !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player)livingentity).isCreative();
        }
    }

    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
    }

    public void stop() {
        LivingEntity livingentity = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
            this.mob.setTarget(null);
        }
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    public void tick() {
        LivingEntity livingentity = this.mob.getTarget();
        ILivingEntityData entityData = (ILivingEntityData) mobpatch;
        if (livingentity != null) {
            double dd0 = this.attackRadius * this.attackRadius;
            double dd1 = this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());

            if(entityData.combat_evolution$isGuard()){
                dd0 = 2 * 2;
            }

            if (dd1 <= dd0 || entityData.combat_evolution$isWander()) {
                this.mob.getNavigation().stop();
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                this.mobpatch.rotateTo(livingentity,30,true);
            }
            else if(!mobpatch.getEntityState().inaction()){
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                double distance = this.mobpatch.getOriginal().distanceTo(livingentity);
                if ((distance < this.attackRadius)) {
                    this.mobpatch.getOriginal().getNavigation().stop();
                }
                else{
                    double d0 = this.mob.getPerceivedTargetDistanceSquareForMeleeAttack(livingentity);
                    this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

                    if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(livingentity)) &&
                            this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == (double)0.0F &&
                            this.pathedTargetY == (double)0.0F && this.pathedTargetZ == (double)0.0F ||
                            livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= (double)1.0F ||
                            this.mob.getRandom().nextFloat() < 0.05F)) {

                        this.pathedTargetX = livingentity.getX();
                        this.pathedTargetY = livingentity.getY();
                        this.pathedTargetZ = livingentity.getZ();
                        this.ticksUntilNextPathRecalculation = 20;
                        if (this.canPenalize) {
                            this.ticksUntilNextPathRecalculation += this.failedPathFindingPenalty;
                            if (this.mob.getNavigation().getPath() != null) {
                                Node finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                                if (finalPathPoint != null && livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1.0D) {
                                    this.failedPathFindingPenalty = 0;
                                } else {
                                    this.failedPathFindingPenalty += 10;
                                }
                            } else {
                                this.failedPathFindingPenalty += 10;
                            }
                        }

                        if (d0 > 1024.0D) {
                            this.ticksUntilNextPathRecalculation += 10;
                        } else if (d0 > 256.0D) {
                            this.ticksUntilNextPathRecalculation += 5;
                        }

                        if (!this.mob.getNavigation().moveTo(livingentity, this.speedModifier)) {
                            this.ticksUntilNextPathRecalculation += 15;
                        }

                        this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
                    }
                }
            }
        }

    }

    protected double getAttackReachSqr(LivingEntity pAttackTarget) {
        return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + pAttackTarget.getBbWidth();
    }
}

