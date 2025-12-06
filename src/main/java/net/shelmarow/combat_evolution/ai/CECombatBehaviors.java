package net.shelmarow.combat_evolution.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.efcondition.CurrentAngle;
import net.shelmarow.combat_evolution.ai.efcondition.EntityTag;
import net.shelmarow.combat_evolution.ai.efcondition.HealthCheck;
import net.shelmarow.combat_evolution.iml.ILivingEntityData;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.*;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class CECombatBehaviors<T extends MobPatch<?>> {
    private final List<BehaviorRoot<T>> behaviorRoots;
    private final Map<BehaviorRoot<T>, Behavior<T>> cachedBehaviors = new HashMap<>();
    private Behavior<T> currentBehavior;

    //AI决策树
    protected CECombatBehaviors(Builder<T> builder) {
        this.behaviorRoots = builder.behaviorRoots.stream().map(BehaviorRoot.Builder::build).toList();
    }


    /*
        1.选择一个根节点行为序列
        （1）先判断根节点的冷却，挑选出冷却完毕的。
        （2）遍历所有可用根节点，选出能执行的最优行为，存储在缓存列表中
        （3）遍历完毕后，根据根节点的优先级和权重选出一个最优根节点
        （4）从缓存列表中取出该根节点的最优子行为并执行
        2.根节点选择优先级逻辑
        （1）选择冷却完毕的
        （2）选择优先级最高的
        （3）根据权重随机选择
        3.子行为选择优先级逻辑
        （1）condition通过
        （2）优先级最高的
        （3）权重随机
        4.特殊打断condition
        （1）如果当前行为可以被打断，且有打断窗口
        （2）持续检查子行为中，拥有能够打断父节点的行为是否满足条件
        （3）满足则结束当前行为，执行子行为
     */


    public Behavior<T> selectBehaviorRootByPriority(T mobpatch) {
        //先清空缓存
        cachedBehaviors.clear();
        //可用的根节点列表
        List<BehaviorRoot<T>> usableBehaviors = new ArrayList<>();

        //选出冷却完毕，且存在子行为能执行（顺便选出最优并缓存）的根节点
        behaviorRoots.stream().filter(root-> root.cooldown <= 0 && root.priority > 0).forEach(root->{
            Behavior<T> behavior = selectBehavior(mobpatch,root.getBehaviors(),false);
            if(behavior != null){
                usableBehaviors.add(root);
                cachedBehaviors.put(root,behavior);
            }
        });

        //现在根据优先级和权重选出最优根节点
        List<BehaviorRoot<T>> list = new ArrayList<>();
        double max = -Double.MAX_VALUE;

        for (BehaviorRoot<T> root : usableBehaviors) {
            if(root.priority > max){
                list.clear();
                list.add(root);
                max = root.priority;
            }
            else if(root.priority == max){
                list.add(root);
            }
        }

        //如果只有一个，直接返回
        if(list.size() == 1) return cachedBehaviors.get(list.get(0));
        //然后是权重随机选择
        else if(list.size() > 1) {
            List<BehaviorRoot<T>> weightList = list.stream().filter(b->b.weight > 0).toList();

            double totalWeight = 0;
            double counter = 0;

            for (BehaviorRoot<T> root : weightList) {
                totalWeight += root.weight;
            }

            double random = Math.random()*totalWeight;

            for (BehaviorRoot<T> root : weightList) {
                counter += root.weight;
                if (counter >= random) {
                    return cachedBehaviors.get(root);
                }
            }
        }

        //找不到可用根节点，返回空
        return null;
    }

    //选择最优子行为
    public Behavior<T> selectBehavior(T mobpatch, List<Behavior<T>> behaviors, boolean canBeInterrupted) {

        //先选出condition通过且符合打断条件的行为，然后按照优先级和权重挑选
        List<Behavior<T>> usableBehaviors = selectHighestPriorityBehavior(behaviors.stream().filter(b -> b.priority > 0 && b.checkPredicates(mobpatch) && (!canBeInterrupted || b.canInterruptParent)).toList());

        //检查可用行为的数量，如果只有一个直接返回，否则按照权重进行随机选择（筛选掉权重为0的节点）
        if(usableBehaviors.size() == 1) return usableBehaviors.get(0);
        else if(usableBehaviors.size() > 1) return selectBehaviorByWeight(usableBehaviors.stream().filter(b -> b.weight > 0).toList());

        return null;
    }

    public List<Behavior<T>> selectHighestPriorityBehavior(List<Behavior<T>> list) {
        List<Behavior<T>> usableBehaviors = new ArrayList<>();
        double max = -Double.MAX_VALUE;

        for (Behavior<T> behavior : list) {
            //如果有优先级更高的，清空列表，添加自身，更新最大值
            if(behavior.priority > max){
                usableBehaviors.clear();
                usableBehaviors.add(behavior);
                max = behavior.priority;
            }
            //如果优先级相等，添加自身
            else if(behavior.priority == max){
                usableBehaviors.add(behavior);
            }
        }
        return usableBehaviors;
    }

    public Behavior<T> selectBehaviorByWeight(List<Behavior<T>> list){
        if(list.isEmpty()) return null;

        double totalWeight = 0;
        double counter = 0;

        for (Behavior<T> behavior : list) {
            totalWeight += behavior.weight;
        }

        double random = Math.random()*totalWeight;

        for (Behavior<T> behavior : list) {
            counter += behavior.weight;
            if (counter >= random) {
                return behavior;
            }
        }

        //理论上不会到这里，但是还是要写
        return null;
    }


    //主要tick时间，用于挑选和执行行为，以及处理冷却等
    public void tick(T mobpatch) {

        //不存在行为时，挑选行为
        if (currentBehavior == null && !mobpatch.getEntityState().inaction()) {
            Behavior<T> behavior = selectBehaviorRootByPriority(mobpatch);
            if (behavior != null) {
                currentBehavior = behavior;
                currentBehavior.execute(mobpatch);
            }
        }

        /*
        对于动画类行为，结束时如果没有可执行的子序列，直接结束行为
        对于防御，游荡类行为，在持续时间结束后如果没有可用的子序列，直接结束行为
         */
        if(currentBehavior != null){

            //未结束执行行为
            if(!currentBehavior.isFinished()){
                currentBehavior.tick(mobpatch);
            }
            //如果当前行为是可打断行为，需要持续检测子行为是否满足打断条件
            if(currentBehavior.canBeInterrupted(mobpatch)){
                Behavior<T> behavior = selectBehavior(mobpatch,currentBehavior.getNextBehaviors(), true);
                if(behavior != null){
                    currentBehavior.stopGuardAndWander(mobpatch);
                    currentBehavior.behaviorFinished();
                    currentBehavior = behavior;
                    currentBehavior.execute(mobpatch);
                }
            }
            //如果当前行为不是运行中，尝试选择下一个行为
            if(!currentBehavior.isRunning()){
                //如果子节点已经全部执行完毕，清空当前行为
                if(currentBehavior.getNextBehaviors().isEmpty() && !mobpatch.getEntityState().inaction()){
                    currentBehavior.resetCooldown();
                    clearCurrentBehavior();
                }
                //尝试寻找可用的子行为
                else if(!currentBehavior.getNextBehaviors().isEmpty()){
                    Behavior<T> behavior = selectBehavior(mobpatch,currentBehavior.getNextBehaviors(), false);
                    //成功找到，直接执行
                    if(behavior != null){
                        currentBehavior = behavior;
                        currentBehavior.execute(mobpatch);
                    }
                    //没找到且不是等待状态，清空并结束
                    else if(!currentBehavior.isWaiting()){
                        currentBehavior.resetCooldown();
                        clearCurrentBehavior();
                    }
                }
            }
        }

        //处理根节点冷却
        for (BehaviorRoot<T> behaviorRoot : behaviorRoots) {
            behaviorRoot.tick();
        }



        //取消防御动画,为了防止有时候取消失败，在tick中执行
        ILivingEntityData entityData = (ILivingEntityData) mobpatch;
        if(!entityData.combat_evolution$isGuard(mobpatch.getOriginal())){
            AssetAccessor<? extends DynamicAnimation> animation = mobpatch.getAnimator().getPlayerFor(null).getAnimation();
            if(animation != null && animation == mobpatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.EMPTY_ANIMATION)){
                if(animation != Animations.EMPTY_ANIMATION)
                    mobpatch.stopPlaying(mobpatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.EMPTY_ANIMATION));
            }
        }
    }

    public Behavior<T> getCurrentBehavior() {
        return this.currentBehavior;
    }

    public void clearCurrentBehavior() {
        this.currentBehavior = null;
    }

    //构造器
    public static class Builder<T extends MobPatch<?>>{
        private final List<BehaviorRoot.Builder<T>> behaviorRoots = new ArrayList<>();

        public Builder<T> newBehaviorRoot(BehaviorRoot.Builder<T> behaviors) {
            this.behaviorRoots.add(behaviors);
            return this;
        }

        public CECombatBehaviors<T> build() {
            return new CECombatBehaviors<>(this);
        }
    }

    public static <T extends MobPatch<?>> Builder<T> builder() {
        return new Builder<>();
    }

    //根节点
    public static class BehaviorRoot<T extends MobPatch<?>> {
        private final String rootName;
        private final List<Behavior<T>> behaviors;
        private final double priority;
        private final double weight;
        private final int maxCooldown;
        private int cooldown;

        public BehaviorRoot(Builder<T> builder) {
            this.rootName = builder.rootName;
            this.behaviors = builder.behavior.stream().map(b->b.build(this)).toList();
            this.priority = builder.priority;
            this.weight = builder.weight;
            this.maxCooldown = builder.maxCooldown;
            this.cooldown = builder.cooldown;
        }

        public String getRootName() {
            return rootName;
        }

        public List<Behavior<T>> getBehaviors() {
            return this.behaviors;
        }

        public void tick() {
            if (this.cooldown > 0) {
                --this.cooldown;
            }
        }

        public void resetCooldown(int exCoolDown) {
            this.cooldown = this.maxCooldown + exCoolDown;
        }


        public static class Builder<T extends MobPatch<?>> {
            private String rootName;
            private final List<Behavior.Builder<T>> behavior = new ArrayList<>();
            private double priority = 1;
            private double weight = 1;
            private int maxCooldown = 0;
            private int cooldown = 0;

            public Builder<T> rootName(String rootName) {
                this.rootName = rootName;
                return this;
            }

            public Builder<T> addFirstBehavior(Behavior.Builder<T> behavior) {
                this.behavior.add(behavior);
                return this;
            }

            public Builder<T> priority(double priority) {
                this.priority = priority;
                return this;
            }

            public Builder<T> weight(double weight) {
                this.weight = weight;
                return this;
            }
            public Builder<T> maxCooldown(int maxCooldown) {
                this.maxCooldown = maxCooldown;
                return this;
            }

            public Builder<T> cooldown(int cooldown) {
                this.cooldown = cooldown;
                return this;
            }

            public BehaviorRoot<T> build() {
                return new BehaviorRoot<>(this);
            }

        }

        public static <T extends MobPatch<?>> Builder<T> builder() {
            return new Builder<>();
        }
    }
    //行为类
    public static class Behavior<T extends MobPatch<?>> {
        private final String behaviorName;                  //行为名称
        private final Consumer<T> behavior;                 //具体行为
        private final Consumer<T> counter;                  //反击行为
        private final List<Consumer<T>> exBehaviors;        //额外行为
        private final List<Consumer<T>> onCounterStart;     //额外行为
        private final BehaviorType type;                    //主行为类型
        private final int exCoolDown;                       //额外冷却时间
        private final int behaviorTime;                     //特定行为的持续时间（游荡、防御）
        private int timeCount;                              //计时器
        private final CounterType counterType;              //反击类型（不反击，随机反击，防御次数耗尽时）
        private boolean canCounter = false;                 //反击标志
        private final double counterChance;                 //反击概率
        private final int maxGuardHit;                      //最大防御次数
        private int guardHit;                               //计数器
        private final int totalWaitTime;                    //行为结束后的等待窗口时间
        private int waitTime;                               //等待计时器
        private final double priority;                      //优先级
        private final double weight;                        //权重
        private final int stopByStun;                       //是否会被眩晕打断其余连段
        private final boolean canInterruptParent;           //是否能打断父行为
        private final boolean canBeInterrupted;             //是否能被其他行为打断
        private final InterruptType interruptType;          //打断类型
        private final List<Float> interruptedWindow;        //能够被打断的窗口时间
        private final BehaviorRoot<T> behaviorRoot;         //所属的根节点
        private final List<Condition<T>> conditions;        //执行需要满足的条件
        private final List<Behavior<T>> nextBehaviors;      //下一个能执行的行为列表
        private BehaviorState state = BehaviorState.RUNNING;//执行状态
        private final AssetAccessor<? extends StaticAnimation> counterAnimation;    //防御反击动画
        private final List<TimeEvent> timeEventList;                                //时间事件列表
        private final List<HitEvent> hitEventList;                                  //攻击命中事件列表
        private boolean shouldExecuteTimeEvent = false;                             //时间事件锁
        private boolean shouldExecuteHitEvent = false;                              //攻击命中事件锁

        private Behavior(Builder<T> builder, BehaviorRoot<T> behaviorRoot){
            this.behaviorName = builder.behaviorName;
            this.stopByStun = builder.stopByStun;
            this.behaviorRoot = behaviorRoot;
            this.behavior = builder.behavior;
            this.counter = builder.counter;
            this.exBehaviors = builder.exBehaviors;
            this.onCounterStart = builder.onCounterStart;
            this.type = builder.type;
            this.exCoolDown = builder.exCoolDown;
            this.behaviorTime = builder.behaviorTime;
            this.timeCount = behaviorTime;
            this.counterType = builder.counterType;
            this.counterChance = builder.counterChance;
            this.maxGuardHit = builder.maxGuardHit;
            this.guardHit = maxGuardHit;
            this.totalWaitTime = builder.totalWaitTime;
            this.waitTime = totalWaitTime;
            this.canInterruptParent = builder.canInterruptParent;
            this.canBeInterrupted = builder.canBeInterrupted;
            this.interruptType = builder.interruptType;
            this.interruptedWindow = builder.interruptedWindow;
            this.priority = builder.priority;
            this.weight = builder.weight;
            this.conditions = builder.conditions;
            this.counterAnimation = builder.counterAnimation;
            this.nextBehaviors = builder.nextBehaviors.stream().map(b->b.build(behaviorRoot)).toList();
            this.timeEventList = builder.timeEventList;
            this.hitEventList = builder.hitEventList;
        }

        public BehaviorRoot<T> getBehaviorRoot() {
            return behaviorRoot;
        }

        public void setShouldExecuteTimeEvent(boolean shouldExecuteTimeEvent) {
            this.shouldExecuteTimeEvent = shouldExecuteTimeEvent;
        }

        public void executeTimeEvent(float pre,float current,MobPatch<?> mobPatch){
            if(shouldExecuteTimeEvent && !this.timeEventList.isEmpty()) {
                for (TimeEvent event : this.timeEventList) {
                    event.executeIfAvailable(pre, current, mobPatch);
                }
            }
        }

        public void resetTimeEventAvailable(){
            for(TimeEvent event : this.timeEventList){
                event.resetAvailable();
            }
        }

        public void setShouldExecuteHitEvent(boolean shouldExecuteHitEvent) {
            this.shouldExecuteHitEvent = shouldExecuteHitEvent;
        }

        public void executeHitEvent(MobPatch<?> mobPatch, Entity target){
            if(shouldExecuteHitEvent && !this.hitEventList.isEmpty()) {
                for(HitEvent event : this.hitEventList){
                    event.executeHitEvent(mobPatch,target);
                }
            }
        }

        public boolean canBeInterrupted(T mobpatch) {
            /*
                根据类型进行划分
                1.时间窗口划分
                2.动作level划分
             */
            if(canBeInterrupted && !interruptedWindow.isEmpty()){
                if (interruptType == InterruptType.TIME) {
                    if(behaviorTime == 0 || getType() == BehaviorType.ANIMATION) {
                        AnimationPlayer animator = mobpatch.getAnimator().getPlayerFor(null);
                        if (animator != null) {
                            float prevElapsedTime = animator.getPrevElapsedTime();
                            float elapsedTime = animator.getElapsedTime();
                            return elapsedTime >= interruptedWindow.get(0) && elapsedTime < interruptedWindow.get(1) || prevElapsedTime >= interruptedWindow.get(0) && prevElapsedTime < interruptedWindow.get(1);
                        }
                    }
                    else {
                        return timeCount * 0.05F >= interruptedWindow.get(0) && timeCount * 0.05F < interruptedWindow.get(1);
                    }
                }
                else if(interruptType == InterruptType.LEVEL) {
                    return interruptedWindow.contains((float)mobpatch.getEntityState().getLevel());
                }
            }
            return false;
        }

        public boolean checkPredicates(T mobpatch) {
            for(Condition<T> condition : this.conditions) {
                if (!condition.predicate(mobpatch)) {
                    return false;
                }
            }
            return true;
        }

        public void execute(T mobpatch) {
            this.behavior.accept(mobpatch);
            for(Consumer<T> consumer : this.exBehaviors){
                consumer.accept(mobpatch);
            }
            mobpatch.updateEntityState();
            this.shouldExecuteTimeEvent = true;
            this.shouldExecuteHitEvent = true;
            this.state = BehaviorState.RUNNING;
        }

        public List<Behavior<T>> getNextBehaviors() {
            return this.nextBehaviors;
        }

        public void resetCooldown() {
            this.behaviorRoot.resetCooldown(this.exCoolDown);
        }

        public boolean isFinished() {
            return this.state == BehaviorState.FINISHED;
        }

        public BehaviorType getType() {
            return this.type;
        }

        public void tick(T mobpatch) {
            if(state == BehaviorState.RUNNING) {
                running(mobpatch);
            }
            else if(state == BehaviorState.WAITING){
                waiting(mobpatch);
            }
        }

        public void running(T mobpatch) {
            if(type == BehaviorType.WANDER || type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) {
                if (timeCount > 0) {
                    ILivingEntityData entityData = (ILivingEntityData) mobpatch;
                    //索敌朝向
                    if(mobpatch.getTarget()!=null) {
                        mobpatch.rotateTo(mobpatch.getTarget(),360F,true);
                    }

                    //设置游荡
                    if(type == BehaviorType.WANDER || type == BehaviorType.GUARD_WANDER){
                        this.behavior.accept(mobpatch);

                        if(timeCount >= behaviorTime - 1) {
                            entityData.combat_evolution$setWander(mobpatch.getOriginal(), true);
                        }
                        //如果游荡被取消，直接进入等待
                        if(!entityData.combat_evolution$isWander(mobpatch.getOriginal())) {
                            behaviorWaiting();
                        }
                    }

                    //设置防御
                    if(type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER){
                        this.behavior.accept(mobpatch);
                        if(timeCount >= behaviorTime - 1) {
                            entityData.combat_evolution$setGuard(mobpatch.getOriginal(), true);
                        }
                        //如果防御被取消，直接进入等待
                        if(!entityData.combat_evolution$isGuard(mobpatch.getOriginal()) && !canCounter) {
                            behaviorWaiting();
                        }
                    }


                    if((type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) && canCounter){
                        entityData.combat_evolution$setInCounter(mobpatch.getOriginal(), true);
                        if (mobpatch.getEntityState().canBasicAttack()) {
                            canCounter = false;
                            stopGuardAndWander(mobpatch);
                            this.counter.accept(mobpatch);
                            for (Consumer<T> consumer : this.onCounterStart){
                                consumer.accept(mobpatch);
                            }
                        }
                        else {
                            timeCount++;
                        }
                    }

                    timeCount--;
                }
                else if (timeCount == 0 && mobpatch.getEntityState().canBasicAttack()) {
                    stopGuardAndWander(mobpatch);
                    behaviorWaiting();
                }

            }
            else if(type == BehaviorType.ANIMATION || type == BehaviorType.CUSTOM){
                if (mobpatch.getEntityState().canBasicAttack() && !nextBehaviors.isEmpty()) {
                    behaviorWaiting();
                }
                else if (!mobpatch.getEntityState().inaction() && nextBehaviors.isEmpty()) {
                    behaviorWaiting();
                }
            }
        }

        public void stopGuardAndWander(T mobpatch) {
            ILivingEntityData entityData = (ILivingEntityData) mobpatch;
            //结束游荡
            if(type == BehaviorType.WANDER || type == BehaviorType.GUARD_WANDER) {
                entityData.combat_evolution$setWander(mobpatch.getOriginal(), false);
                mobpatch.getOriginal().getMoveControl().strafe(0,0);
            }
            //结束防御
            if(type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) {
                entityData.combat_evolution$setGuard(mobpatch.getOriginal(), false);
                entityData.combat_evolution$setInCounter(mobpatch.getOriginal(), false);
                AssetAccessor<? extends StaticAnimation> guardAnimation = mobpatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.SWORD_GUARD);
                if (mobpatch.isLogicalClient()) {
                    mobpatch.getAnimator().stopPlaying(guardAnimation);
                } else {
                    mobpatch.stopPlaying(guardAnimation);
                }
            }
        }

        public void waiting(T mobpatch) {
            if(waitTime > 0) {
                waitTime--;
            }
            else {
                behaviorFinished();
            }
        }

        public boolean whenGuardHit(){
            switch (counterType){
                case END -> {
                    guardHit--;
                    if(guardHit <= 0){
                        canCounter = true;
                        timeCount = 1;
                    }
                }
                case RANDOM -> {
                    if(counterChance >= Math.random()){
                        canCounter = true;
                        timeCount = 1;
                    }
                }
            }
            return canCounter;
        }

        public void behaviorWaiting(){
            if(totalWaitTime > 0)this.state = BehaviorState.WAITING;
            else behaviorFinished();
        }

        public void behaviorFinished(){
            this.state = BehaviorState.FINISHED;
            timeCount = behaviorTime;
            guardHit = maxGuardHit;
            waitTime = totalWaitTime;
            canCounter = false;
            shouldExecuteTimeEvent = false;
            shouldExecuteHitEvent = false;
        }

        public boolean isWaiting() {
            return state == BehaviorState.WAITING;
        }

        public boolean isRunning() {
            return state == BehaviorState.RUNNING;
        }

        public String getBehaviorName() {
            return behaviorName;
        }

        public boolean stopByStun(StunType stunType) {
            /*
                其他 不会被眩晕打断
                1 会被所有眩晕打断
                2 不会被short打断
                3 不会被short long打断
                4 不会被short long hold打断
                5 不会被short long hold fall打断
                6 不会被short long hold knockdown打断
                7 不会被short long hold fall knockdown打断
             */
            if(stopByStun == 1) return true;
            else if(stopByStun == 2){
                return stunType != StunType.SHORT;
            }
            else if(stopByStun == 3){
                return stunType != StunType.SHORT && stunType != StunType.LONG;
            }
            else if(stopByStun == 4){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD;
            }
            else if(stopByStun == 5){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL;
            }
            else if(stopByStun == 6){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.KNOCKDOWN;
            }
            else if(stopByStun == 7){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL && stunType != StunType.KNOCKDOWN;
            }
            else {
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL && stunType != StunType.KNOCKDOWN && stunType != StunType.NEUTRALIZE;
            }
        }

        public AssetAccessor<? extends StaticAnimation> getCounterAnimation() {
            return counterAnimation;
        }


        //行为类构造器
        public static class Builder<T extends MobPatch<?>> {
            private Consumer<T> counter;
            private String behaviorName;
            private Consumer<T> behavior;
            private final List<Consumer<T>> exBehaviors = new ArrayList<>();
            private final List<Consumer<T>> onCounterStart = new ArrayList<>();
            private BehaviorType type = BehaviorType.NONE;
            private double priority = 1;
            private double weight = 1;
            private int exCoolDown = 0;
            private int behaviorTime = 0;
            private CounterType counterType = CounterType.NEVER;
            private double counterChance = 0.25;
            private int maxGuardHit = Integer.MAX_VALUE;
            private int totalWaitTime = 0;
            private int stopByStun = 1;
            private boolean canInterruptParent = false;
            private boolean canBeInterrupted = false;
            private InterruptType interruptType = InterruptType.TIME;
            private List<Float> interruptedWindow = new ArrayList<>();
            private final List<Condition<T>> conditions = new ArrayList<>();
            private final List<Builder<T>> nextBehaviors = new ArrayList<>();
            private AssetAccessor<? extends StaticAnimation> counterAnimation;
            private final LivingEntityPatch.ServerAnimationPacketProvider packetProvider = SPAnimatorControl::new;
            private final List<TimeEvent> timeEventList = new ArrayList<>();
            private final List<HitEvent> hitEventList = new ArrayList<>();


            public Builder<T> addTimeEvent(TimeEvent timeEvent) {
                this.timeEventList.add(timeEvent);
                return this;
            }

            public Builder<T> addTimeEvent(TimeEvent... timeEvents) {
                this.timeEventList.addAll(List.of(timeEvents));
                return this;
            }

            public Builder<T> addHitEvent(HitEvent hitEvent) {
                this.hitEventList.add(hitEvent);
                return this;
            }

            public Builder<?> addHitEvent(HitEvent... hitEvents) {
                this.hitEventList.addAll(List.of(hitEvents));
                return this;
            }

            public Builder<T> onCounterStart(Consumer<T> behavior) {
                this.onCounterStart.add(behavior);
                return this;
            }

            @SafeVarargs
            public final Builder<T> onCounterStart(Consumer<T>... behavior) {
                this.exBehaviors.addAll(List.of(behavior));
                return this;
            }

            public Builder<T> counterAnimation(AssetAccessor<? extends StaticAnimation> counterAnimation, AnimationParams params) {
                this.counterAnimation = counterAnimation;
                this.counter = (mobPatch)-> {
                    mobPatch.playAnimationSynchronized(counterAnimation, params.getTransitionTime(), this.packetProvider);
                    if(mobPatch instanceof ILivingEntityData livingEntityData) {
                        livingEntityData.combat_evolution$setCanModifySpeed(mobPatch.getOriginal(), true);
                        livingEntityData.combat_evolution$setAttackSpeed(mobPatch.getOriginal(), params.getAttackSpeed());
                        livingEntityData.combat_evolution$setDamageMultiplier(mobPatch.getOriginal(), params.getDamageMultiplier());
                        livingEntityData.combat_evolution$setImpactMultiplier(mobPatch.getOriginal(), params.getImpactMultiplier());
                        livingEntityData.combat_evolution$setArmorNegationMultiplier(mobPatch.getOriginal(), params.getArmorNegationMultiplier());
                        livingEntityData.combat_evolution$setStunType(mobPatch.getOriginal(), params.getStunType());
                        livingEntityData.combat_evolution$setDamageSource(params.getDamageSource());
                    }
                };
                return this;
            }

            public Builder<T> counterAnimation(AssetAccessor<? extends StaticAnimation> counterAnimation,float transitionTime) {
                return counterAnimation(counterAnimation, new AnimationParams().transitionTime(transitionTime));
//                this.counterAnimation = counterAnimation;
//                this.counter = (mobpatch)->{
//                    if(counterAnimation != null && counterAnimation != Animations.EMPTY_ANIMATION) {
//                        if(mobpatch instanceof ILivingEntityData livingEntityData) {
//                            livingEntityData.combat_evolution$setAttackSpeed(mobpatch.getOriginal(),1);
//                            livingEntityData.combat_evolution$setDamageMultiplier(mobpatch.getOriginal(), 1);
//                            livingEntityData.combat_evolution$setImpactMultiplier(mobpatch.getOriginal(), 1);
//                            livingEntityData.combat_evolution$setArmorNegationMultiplier(mobpatch.getOriginal(), 1);
//                            livingEntityData.combat_evolution$setStunType(mobpatch.getOriginal(), -1);
//                            livingEntityData.combat_evolution$setDamageSource(new HashSet<>());
//                        }
//                        mobpatch.playAnimationSynchronized(counterAnimation, transitionTime, this.packetProvider);
//                    }
//                };
            }

            public Builder<T> counterType(CounterType counterType) {
                this.counterType = counterType;
                return this;
            }

            public Builder<T> counterChance(double counterChance) {
                this.counterChance = counterChance;
                return this;
            }

            public Builder<T> maxGuardHit(int maxGuardHit) {
                this.maxGuardHit = maxGuardHit;
                return this;
            }

            public Builder<T> canInterruptParent(boolean canInterruptParent) {
                this.canInterruptParent = canInterruptParent;
                return this;
            }

            public Builder<T> interruptedByTime(float start, float end) {
                this.canBeInterrupted = true;
                this.interruptType = InterruptType.TIME;
                this.interruptedWindow = new ArrayList<>(List.of(start,end));
                return this;
            }

            public Builder<T> interruptedByLevel(Integer... levels) {
                this.canBeInterrupted = true;
                this.interruptType = InterruptType.LEVEL;
                for(int level : levels) {
                    interruptedWindow.add((float) level);
                }
                return this;
            }

            public Builder<T> setCooldown(int exCoolDown) {
                this.exCoolDown = exCoolDown;
                return this;
            }

            public Builder<T> addCooldown(int exCoolDown) {
                this.exCoolDown += exCoolDown;
                return this;
            }

            public Builder<T> waitTime(int waitTime) {
                this.totalWaitTime = waitTime;
                return this;
            }


            public Builder<T> stopByStun(int stopByStun) {
                this.stopByStun = stopByStun;
                return this;
            }

            public Builder<T> customBehavior(Consumer<T> behavior) {
                this.behavior = behavior;
                this.type = BehaviorType.CUSTOM;
                return this;
            }

            public Builder<T> name(String name) {
                this.behaviorName = name;
                return this;
            }

            public Builder<T> addExBehavior(Consumer<T> behavior) {
                this.exBehaviors.add(behavior);
                return this;
            }

            @SafeVarargs
            public final Builder<T> addExBehavior(Consumer<T>... behaviors) {
                this.exBehaviors.addAll(List.of(behaviors));
                return this;
            }

            public Builder<T> setPhase(int phase){
                this.exBehaviors.add((mobpatch)->{
                    ILivingEntityData entityData = (ILivingEntityData) mobpatch;
                    entityData.combat_evolution$setPhase(mobpatch.getOriginal(), phase);
                });
                return this;
            }

            public Builder<T> wander(int totalTime,float pForward, float pStrafe) {
                this.behaviorTime = totalTime;
                this.type = BehaviorType.WANDER;
                this.behavior = (mobpatch)->{
                    mobpatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                };
                return this;
            }


            public Builder<T> wanderWithAnimation(AnimationManager.AnimationAccessor<? extends StaticAnimation> animation, int totalTime, float pForward, float pStrafe) {
                this.behaviorTime = totalTime;
                this.type = BehaviorType.WANDER;
                this.behavior = (mobpatch)->{
                    mobpatch.playAnimationSynchronized(animation,0F);
                    mobpatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                };
                return this;
            }

            public Builder<T> guard(int totalTime) {
                behaviorTime = totalTime;
                this.type = BehaviorType.GUARD;
                this.behavior = (mobpatch)->{
                    AssetAccessor<? extends StaticAnimation> currentAnimation = mobpatch.getAnimator().getPlayerFor(null).getAnimation().get().getRealAnimation();
                    AssetAccessor<? extends StaticAnimation> guardAnimation = mobpatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.SWORD_GUARD);
                    if(currentAnimation != guardAnimation && !mobpatch.getEntityState().inaction()) {
                        mobpatch.playAnimationSynchronized(guardAnimation, 0F, this.packetProvider);
                    }
                };
                return this;
            }

            public Builder<T> guardWithWander(int totalTime,float pForward, float pStrafe) {
                behaviorTime = totalTime;
                this.type = BehaviorType.GUARD_WANDER;
                this.behavior = (mobpatch)->{
                    mobpatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                };
                return this;
            }

            public Builder<T> animationBehavior(AnimationManager.AnimationAccessor<? extends StaticAnimation> motion,AnimationParams params) {
                this.type = BehaviorType.ANIMATION;
                this.behavior = (mobPatch) -> {
                    mobPatch.playAnimationSynchronized(motion, params.getTransitionTime(), this.packetProvider);
                    if (mobPatch instanceof ILivingEntityData livingEntityData) {
                        livingEntityData.combat_evolution$setCanModifySpeed(mobPatch.getOriginal(), true);
                        livingEntityData.combat_evolution$setAttackSpeed(mobPatch.getOriginal(), params.getAttackSpeed());
                        livingEntityData.combat_evolution$setDamageMultiplier(mobPatch.getOriginal(), params.getDamageMultiplier());
                        livingEntityData.combat_evolution$setImpactMultiplier(mobPatch.getOriginal(), params.getImpactMultiplier());
                        livingEntityData.combat_evolution$setArmorNegationMultiplier(mobPatch.getOriginal(), params.getArmorNegationMultiplier());
                        livingEntityData.combat_evolution$setStunType(mobPatch.getOriginal(), params.getStunType());
                        livingEntityData.combat_evolution$setDamageSource(params.getDamageSource());
                    }
                };
                return this;
            }

            public Builder<T> animationBehavior(AnimationManager.AnimationAccessor<? extends StaticAnimation> motion,float transitionTime) {
                return animationBehavior(motion, new AnimationParams().transitionTime(transitionTime));
//                this.type = BehaviorType.ANIMATION;
//                this.behavior = (mobpatch) -> {
//                    if(mobpatch instanceof ILivingEntityData livingEntityData) {
//                        livingEntityData.combat_evolution$setAttackSpeed(mobpatch.getOriginal(), 1.0F);
//                        livingEntityData.combat_evolution$setDamageMultiplier(mobpatch.getOriginal(), 1.0F);
//                        livingEntityData.combat_evolution$setImpactMultiplier(mobpatch.getOriginal(), 1.0F);
//                        livingEntityData.combat_evolution$setArmorNegationMultiplier(mobpatch.getOriginal(), 1.0F);
//                        livingEntityData.combat_evolution$setStunType(mobpatch.getOriginal(), -1);
//                        livingEntityData.combat_evolution$setDamageSource(new HashSet<>());
//                    }
//                    mobpatch.playAnimationSynchronized(motion, transitionTime, this.packetProvider);
//                };
//                return this;
            }

            public Builder<T> priority(double priority) {
                this.priority = priority;
                return this;
            }

            public Builder<T> weight(double weight) {
                this.weight = weight;
                return this;
            }

            public Builder<T> addNextBehavior(Builder<T> builder) {
                nextBehaviors.add(builder);
                return this;
            }


            //----------------<一些预设条件>----------------

            public Builder<T> withinEyeHeight() {
                this.condition(new TargetInEyeHeight());
                return this;
            }

            public Builder<T> randomChance(float chance) {
                this.condition(new RandomChance(chance));
                return this;
            }

            public Builder<T> withinDistance(double minDistance, double maxDistance) {
                this.condition(new TargetInDistance(minDistance, maxDistance));
                return this;
            }

            public Builder<T> entityTag(String tag) {
                this.condition(new EntityTag(tag));
                return this;
            }

            public Builder<T> withinCurrentAngle(String side,double degreeFirst, double degreeSecond) {
                this.condition(new CurrentAngle(side,degreeFirst, degreeSecond));
                return this;
            }

            public Builder<T> withinAngle(double minDegree, double maxDegree) {
                this.condition(new TargetInPov(minDegree, maxDegree));
                return this;
            }

            public Builder<T> attackLevel(int min, int max) {
                return custom((patch) -> {
                    LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(patch.getTarget(), LivingEntityPatch.class);
                    if (targetPatch == null) {
                        return false;
                    }
                    else {
                        int level = targetPatch.getEntityState().getLevel();
                        return min <= level && level <= max;
                    }
                });
            }

            public Builder<T> attackLevelContain(Integer... levels) {
                return custom((patch) -> {
                    LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(patch.getTarget(), LivingEntityPatch.class);
                    if (targetPatch == null) {
                        return false;
                    }
                    else {
                        int level = targetPatch.getEntityState().getLevel();
                        return Arrays.stream(levels).toList().contains(level);
                    }
                });
            }

            public Builder<T> phaseBetween(int min, int max) {
                return custom((mobpatch) -> {
                    ILivingEntityData entityData = (ILivingEntityData) mobpatch;
                    int phase = entityData.combat_evolution$getPhase(mobpatch.getOriginal());
                    return min <= phase && phase <= max;
                });
            }

            public Builder<T> phaseContain(Integer... phases) {
                return custom((mobpatch) -> {
                    List<Integer> list = new ArrayList<>(List.of(phases));
                    ILivingEntityData entityData = (ILivingEntityData) mobpatch;
                    int phase = entityData.combat_evolution$getPhase(mobpatch.getOriginal());
                    return list.contains(phase);
                });
            }

            public Builder<T> targetAnimation(AssetAccessor<? extends StaticAnimation> animation){
                return custom((mobpatch)->{
                    LivingEntity target = mobpatch.getTarget();
                    if(target != null){
                        LivingEntityPatch<?> entityPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);
                        if(entityPatch != null){
                            AssetAccessor<? extends StaticAnimation> tAnimation = Objects.requireNonNull(entityPatch.getAnimator().getPlayerFor(null)).getRealAnimation();
                            return animation == tAnimation;
                        }
                    }
                    return false;
                });
            }

            public Builder<T> withinAngleHorizontal(double minDegree, double maxDegree) {
                this.condition(new TargetInPov.TargetInPovHorizontal(minDegree, maxDegree));
                return this;
            }

            public Builder<T> health(float health, HealthCheck.Comparator comparator) {
                this.condition(new HealthCheck(health, comparator));
                return this;
            }

            public Builder<T> custom(Function<T, Boolean> customPredicate) {
                this.condition(new CustomCondition<>(customPredicate));
                return this;
            }

            @SuppressWarnings("unchecked")
            public void condition(Condition<?> predicate) {
                this.conditions.add((Condition<T>) predicate);
            }

            public Behavior<T> build(BehaviorRoot<T> behaviorRoot) {
                return new Behavior<>(this,behaviorRoot);
            }

        }

        public static <T extends MobPatch<?>> Builder<T> builder() {
            return new Builder<>();
        }
    }



    public enum InterruptType{
        TIME,
        LEVEL
    }

    public enum CounterType{
        NEVER,
        RANDOM,
        END,
    }

    public enum BehaviorState {
        RUNNING,
        WAITING,
        FINISHED,
    }

    public enum BehaviorType {
        ANIMATION,
        GUARD,
        WANDER,
        GUARD_WANDER,
        CUSTOM,
        NONE,
    }
}
