package net.shelmarow.combat_evolution.ai;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.ai.condition.*;
import net.shelmarow.combat_evolution.ai.event.*;
import net.shelmarow.combat_evolution.ai.event.manager.CEMobEvent;
import net.shelmarow.combat_evolution.ai.event.manager.CEMobEventManager;
import net.shelmarow.combat_evolution.ai.event.manager.CEMobEventWithReturn;
import net.shelmarow.combat_evolution.ai.iml.ILivingEntityData;
import net.shelmarow.combat_evolution.ai.params.AnimationParams;
import net.shelmarow.combat_evolution.ai.params.PhaseParams;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.data.conditions.entity.CustomCondition;
import yesman.epicfight.data.conditions.entity.RandomChance;
import yesman.epicfight.data.conditions.entity.TargetInEyeHeight;
import yesman.epicfight.data.conditions.entity.TargetInPov;
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
    private final List<BehaviorRoot<T>> globalBehaviors;
    private final Map<BehaviorRoot<T>, Behavior<T>> cachedBehaviors = new HashMap<>();
    private Behavior<T> currentBehavior;

    private final Consumer<MobPatch<?>> noBehaviorTick;
    private final TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> noBehaviorOnHurt;
    private final Map<StunType, List<Consumer<MobPatch<?>>>> globeStunEvents;


    //AI决策树
    protected CECombatBehaviors(Builder<T> builder) {
        this.behaviorRoots = builder.behaviorRoots.stream().map(BehaviorRoot.Builder::build).toList();
        this.globalBehaviors = builder.globalBehaviors.stream().map(BehaviorRoot.Builder::build).toList();
        this.noBehaviorTick = builder.noBehaviorTick;
        this.noBehaviorOnHurt = builder.noBehaviorOnHurt;
        this.globeStunEvents = builder.stunEvents;
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
        5.全局行为
        （1）如果当前行为可接入全局行为，检查可用的全局行为
        （2）根据全局行为的属性，选择不同的接入方式
        （3）如果是打断方式，表示执行完毕后直接结束行为
        （4）如果是返回方式，表示执行完毕后返回原行为继续执行
        （5）执行全局行为时如果中断，自身和接入的行为都会进入冷却
     */


    public Behavior<T> selectBehaviorRootByPriority(T mobPatch,List<BehaviorRoot<T>> behaviorRootList, boolean shouldCheckGlobal) {
        //先清空缓存
        cachedBehaviors.clear();
        //可用的根节点列表
        List<BehaviorRoot<T>> usableBehaviors = new ArrayList<>();

        //选出冷却完毕，且存在子行为能执行（顺便选出最优并缓存）的根节点
        behaviorRootList.stream().filter(root-> root.cooldown <= 0 && root.priority > 0).forEach(root->{
            Behavior<T> behavior = selectBehavior(mobPatch,root.getBehaviors(),false, shouldCheckGlobal);
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
                    //System.out.println("找到可用的根节点["+ root.getRootName()+"]，执行其子行为["+cachedBehaviors.get(root).behaviorName+"]");
                    return cachedBehaviors.get(root);
                }
            }
        }

        //找不到可用根节点，返回空
        return null;
    }

    //选择最优子行为
    public Behavior<T> selectBehavior(T mobPatch, List<Behavior<T>> behaviors, boolean canBeInterrupted, boolean shouldCheckGlobal) {

        //先选出condition通过且符合打断条件的行为，然后按照优先级和权重挑选
        List<Pair<Behavior<T>,Boolean>> usableBehaviors = new ArrayList<>();

        //收集当前节点所属的子行为
        List<Behavior<T>> temp = behaviors.stream().filter(b -> b.priority() > 0 && b.checkPredicates(mobPatch) && (!canBeInterrupted || b.canInterruptParent())).toList();
        for (Behavior<T> behavior : temp) {
            usableBehaviors.add(new Pair<>(behavior,false));
        }

        //如果当前行为能够接入全局行为，额外挑选出可用的全局行为
        if(shouldCheckGlobal && currentBehavior != null && currentBehavior.canInsertGlobalBehavior()) {
            List<BehaviorRoot<T>> canUseList;
            List<String> allowedGlobalNameList = currentBehavior.getAllowedGlobalNameList();
            if(!allowedGlobalNameList.isEmpty()){
                canUseList = globalBehaviors.stream().filter(behaviorRoot-> allowedGlobalNameList.contains(behaviorRoot.rootName)).toList();
            }
            else{
                canUseList = globalBehaviors;
            }

            Behavior<T> usableGlobalBehavior = selectBehaviorRootByPriority(mobPatch, canUseList, false);
            if(usableGlobalBehavior != null){
                usableBehaviors.add(Pair.of(usableGlobalBehavior, true));
            }
        }

        usableBehaviors = selectHighestPriorityBehavior(usableBehaviors);

        //检查可用行为的数量，如果只有一个直接返回，否则按照权重进行随机选择（筛选掉权重为0的节点）
        if(usableBehaviors.size() == 1) {
            //如果选中的是全局行为，则记录当前行为
            if(usableBehaviors.get(0).getSecond()){
                //System.out.println("[普通查询]已选择全局行为["+usableBehaviors.get(0).getFirst().behaviorName+"]，记录上一个行为["+currentBehavior.behaviorName+"]");
                usableBehaviors.get(0).getFirst().getBehaviorRoot().addLastInsertedBehavior(currentBehavior);
            }

            //System.out.println("[普通查询1]已选择行为["+usableBehaviors.get(0).getFirst().behaviorName+"]");
            return usableBehaviors.get(0).getFirst();
        }
        else if(usableBehaviors.size() > 1) {
            Pair<Behavior<T>, Boolean> bestOne = selectBehaviorByWeight(usableBehaviors.stream().filter(b -> b.getFirst().weight() > 0).toList());

            //如果选中的是全局行为，则记录当前行为
            if(bestOne.getSecond()){
                //System.out.println("[普通查询]已选择全局行为["+bestOne.getFirst().behaviorName+"]，记录上一个行为["+currentBehavior.behaviorName+"]");
                bestOne.getFirst().getBehaviorRoot().addLastInsertedBehavior(currentBehavior);
            }

            //System.out.println("[普通查询2]已选择行为["+bestOne.getFirst().behaviorName+"]");
            return bestOne.getFirst();
        }

        return null;
    }

    public List<Pair<Behavior<T>, Boolean>> selectHighestPriorityBehavior(List<Pair<Behavior<T>, Boolean>> list) {
        List<Pair<Behavior<T>, Boolean>> usableBehaviors = new ArrayList<>();
        double max = -Double.MAX_VALUE;

        for (Pair<Behavior<T>, Boolean> pair : list) {
            //如果有优先级更高的，清空列表，添加自身，更新最大值
            if(pair.getFirst().priority() > max){
                usableBehaviors.clear();
                usableBehaviors.add(pair);
                max = pair.getFirst().priority();
            }
            //如果优先级相等，添加自身
            else if(pair.getFirst().priority() == max){
                usableBehaviors.add(pair);
            }
        }
        return usableBehaviors;
    }

    public Pair<Behavior<T>, Boolean> selectBehaviorByWeight(List<Pair<Behavior<T>, Boolean>> list){
        if(list.isEmpty()) return null;

        double totalWeight = 0;
        double counter = 0;

        for(Pair<Behavior<T>, Boolean> pair : list){
            totalWeight += pair.getFirst().weight();
        }

        double random = Math.random()*totalWeight;

        for (Pair<Behavior<T>, Boolean> pair : list) {
            counter += pair.getFirst().weight();
            if (counter >= random) {
                return pair;
            }
        }

        //理论上不会到这里，但是还是要写
        return null;
    }

    private void findAndExecuteLastBehavior(T mobPatch, BehaviorRoot<T> behaviorRoot) {
        if(!behaviorRoot.lastInsertedBehaviors.isEmpty()){
            currentBehavior = behaviorRoot.lastInsertedBehaviors.pop();
            //System.out.println("[递归查询]当前行为["+currentBehavior.behaviorName+"]记录的上一个行为是["+currentBehavior.behaviorName+"]");
            //递归查询子行为不为空的节点
            if(currentBehavior.getNextBehaviors().isEmpty()){
                //System.out.println("[递归查询]当前行为["+currentBehavior.behaviorName+"]不存在子行为，递归查询");
                //冷却当前行为，寻找下一个
                currentBehavior.resetCooldown();
                findAndExecuteLastBehavior(mobPatch, currentBehavior.getBehaviorRoot());
            }
            //成功找到子行为不为空的节点
            else{
                //尝试寻找其中可执行的行为
                Behavior<T> newBehavior = selectBehavior(mobPatch, currentBehavior.getNextBehaviors(), false, false);
                //成功找到，直接执行
                if(newBehavior != null){
                    //System.out.println("[递归查询]当前行为["+currentBehavior.behaviorName+"]找到可用的子行为["+newBehavior.behaviorName+"]，执行行为");
                    currentBehavior = newBehavior;
                    currentBehavior.execute(mobPatch);
                }
                //没找到且不是等待状态，冷却继续递归查询
                else if(!currentBehavior.isWaiting()){
                    //System.out.println("[递归查询]当前行为["+currentBehavior.behaviorName+"]没有找到可用的子行为，递归查询");
                    currentBehavior.resetCooldown();
                    findAndExecuteLastBehavior(mobPatch, currentBehavior.getBehaviorRoot());
                }
            }
        }
        else{
            //System.out.println("[递归查询]当前行为["+currentBehavior.behaviorName+"]没有记录的上一个行为，结束");
            currentBehavior.resetCooldown();
            clearCurrentBehavior();
        }
    }

    //主要tick时间，用于挑选和执行行为，以及处理冷却等
    public void tick(T mobPatch) {

        //不存在行为时，挑选行为
        if (currentBehavior == null && !mobPatch.getEntityState().inaction()) {
            Behavior<T> behavior = selectBehaviorRootByPriority(mobPatch,behaviorRoots, false);
            if (behavior != null) {
                //System.out.println("[tick查询]执行首个行为["+behavior.behaviorName+"]");
                currentBehavior = behavior;
                currentBehavior.execute(mobPatch);
            }
            else {
                executeNoActionTick(mobPatch);
            }
        }

        /*
        对于动画类行为，结束时如果没有可执行的子序列，直接结束行为
        对于防御，游荡类行为，在持续时间结束后如果没有可用的子序列，直接结束行为
         */
        if(currentBehavior != null){

            //未结束执行行为
            if(!currentBehavior.isFinished()){
                currentBehavior.tick(mobPatch);
            }
            //如果当前行为是可打断行为，需要持续检测子行为是否满足打断条件
            if(currentBehavior.canBeInterrupted(mobPatch)){
                Behavior<T> behavior = selectBehavior(mobPatch,currentBehavior.getNextBehaviors(), true, true);
                if(behavior != null){
                    //System.out.println("[tick查询]执行打断行为["+behavior.behaviorName+"]");
                    currentBehavior.stopGuardAndWander(mobPatch);
                    currentBehavior.behaviorFinished();
                    currentBehavior = behavior;
                    currentBehavior.execute(mobPatch);
                }
            }
            //如果当前行为不是运行中，尝试选择下一个行为
            if(!currentBehavior.isRunning()){
                //如果没有可执行的子行为
                if(currentBehavior.getNextBehaviors().isEmpty() && !currentBehavior.canInsertGlobalBehavior()){
                    //如果自身是全局行为并且允许返回，尝试返回至上一个行为并继续执行
                    BehaviorRoot<T> behaviorRoot = currentBehavior.getBehaviorRoot();
                    if(behaviorRoot.isGlobal() && behaviorRoot.isBackAfterFinished()){
                        //System.out.println("[tick查询]当前行为["+currentBehavior.behaviorName+"]为全局行为，尝试寻找上一个记录过的行为");
                        currentBehavior.resetCooldown();
                        //然后递归向上查找，直到查询到有子行为的节点并继续执行
                        findAndExecuteLastBehavior(mobPatch,behaviorRoot);
                    }
                    //动作已经执行完了
                    else if(!mobPatch.getEntityState().inaction()) {
                        //System.out.println("[tick查询]行为序列全部执行完毕");
                        //清空行为并重置冷却
                        currentBehavior.resetAllCooldown();
                        clearCurrentBehavior();
                    }
                }
                //尝试寻找可用的子行为
                else if(currentBehavior != null && (!currentBehavior.getNextBehaviors().isEmpty() || currentBehavior.canInsertGlobalBehavior())){
                    Behavior<T> behavior = selectBehavior(mobPatch,currentBehavior.getNextBehaviors(), false,true);
                    //成功找到，直接执行
                    if(behavior != null){
                        //System.out.println("[tick查询]当前行为["+currentBehavior.behaviorName+"]找到可用的子行为["+behavior.behaviorName+"]，执行行为");
                        currentBehavior = behavior;
                        currentBehavior.execute(mobPatch);
                    }
                    //没找到
                    else{
                        //如果自身是全局行为并且允许返回，尝试返回至上一个行为并继续执行
                        BehaviorRoot<T> behaviorRoot = currentBehavior.getBehaviorRoot();
                        if(currentBehavior.getNextBehaviors().isEmpty() && behaviorRoot.isGlobal() && behaviorRoot.isBackAfterFinished()){
                            //System.out.println("[tick查询]当前行为["+currentBehavior.behaviorName+"]为全局行为，尝试寻找上一个记录过的行为");
                            currentBehavior.resetCooldown();
                            //然后递归向上查找，直到查询到有子行为的节点并继续执行
                            findAndExecuteLastBehavior(mobPatch,behaviorRoot);
                        }
                        //不是等待状态，清空并结束
                        else if(!currentBehavior.isWaiting()) {
                            //System.out.println("[tick查询]当前行为["+currentBehavior.behaviorName+"]没有可用的子行为，结束");
                            currentBehavior.resetAllCooldown();
                            clearCurrentBehavior();
                        }
                    }
                }
            }
        }


        //处理根节点冷却
        behaviorRoots.forEach(BehaviorRoot::tick);
        globalBehaviors.forEach(BehaviorRoot::tick);


        //取消防御动画,为了防止有时候取消失败，在tick中执行
//        ILivingEntityData entityData = (ILivingEntityData) mobPatch;
//        if(!entityData.combat_evolution$isGuard()){
//            AssetAccessor<? extends DynamicAnimation> animation = Objects.requireNonNull(mobPatch.getAnimator().getPlayerFor(null)).getAnimation();
//            if(animation != null && animation == mobPatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.EMPTY_ANIMATION)){
//                if(animation != Animations.EMPTY_ANIMATION)
//                    mobPatch.stopPlaying(mobPatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.EMPTY_ANIMATION));
//            }
//        }
    }

    public void executeNoActionTick(T mobPatch){
        if(noBehaviorTick != null){
            noBehaviorTick.accept(mobPatch);
        }
    }

    public AttackResult executeNoBehaviorOnHurt(MobPatch<?> mobPatch, DamageSource damageSource, AttackResult attackResult){
        if(noBehaviorOnHurt != null){
            return noBehaviorOnHurt.apply(mobPatch, damageSource, attackResult);
        }
        return attackResult;
    }

    public void executeGlobeStunEvent(MobPatch<?> mobPatch, StunType stunType) {
        for (Map.Entry<StunType, List<Consumer<MobPatch<?>>>> entries : this.globeStunEvents.entrySet()) {
            if(entries.getKey() == stunType){
                for (Consumer<MobPatch<?>> consumer : entries.getValue()) {
                    consumer.accept(mobPatch);
                }
            }
        }
    }

    public Behavior<T> getCurrentBehavior() {
        return this.currentBehavior;
    }

    public void clearCurrentBehavior() {
        this.currentBehavior = null;
    }

    @SuppressWarnings("unchecked")
    public void changeBehavior(MobPatch<?> mobPatch, String rootName, String behaviorName, boolean ignoreCondition) {
        for (BehaviorRoot<T> behaviorRoot : behaviorRoots) {
            if(behaviorRoot.getRootName().equals(rootName)){
                for(Behavior<T> behavior : behaviorRoot.getBehaviors()){
                    if(behavior.getBehaviorName().equals(behaviorName)){
                        if(ignoreCondition || currentBehavior.checkPredicates((T) mobPatch)){
                            currentBehavior = behavior;
                            currentBehavior.execute((T) mobPatch);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void resetAllRootCooldown(){
        behaviorRoots.forEach(root->root.cooldown = 0);
        globalBehaviors.forEach(root->root.cooldown = 0);
    }

    public void resetRootCooldown(String rootName, boolean isGlobal){
        if(isGlobal){
            globalBehaviors.stream().filter(root->root.getRootName().equals(rootName))
                    .forEach(root->root.cooldown = 0);
        }
        else {
            behaviorRoots.stream().filter(root->root.getRootName().equals(rootName))
                    .forEach(root->root.cooldown = 0);
        }
    }

    public void setRootCooldown(String rootName, int cooldownSet, boolean isGlobal){
        if(isGlobal){
            globalBehaviors.stream().filter(root->root.getRootName().equals(rootName))
                    .forEach(root->root.cooldown = cooldownSet);
        }
        else {
            behaviorRoots.stream().filter(root->root.getRootName().equals(rootName))
                    .forEach(root->root.cooldown = cooldownSet);
        }
    }

    //构造器
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder<T extends MobPatch<?>>{
        private final List<BehaviorRoot.Builder<T>> behaviorRoots = new ArrayList<>();
        private final List<BehaviorRoot.Builder<T>> globalBehaviors = new ArrayList<>();
        private Consumer<MobPatch<?>> noBehaviorTick = (mobPatch) -> {};
        private TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> noBehaviorOnHurt =
                (mobPatch, damageSource, attackResult) -> attackResult;
        private final Map<StunType, List<Consumer<MobPatch<?>>>> stunEvents = new HashMap<>();

        public Builder<T> newBehaviorRoot(BehaviorRoot.Builder<T> behaviors) {
            behaviors.isGlobal = false;
            this.behaviorRoots.add(behaviors);
            return this;
        }

        public Builder<T> newGlobalBehavior(BehaviorRoot.Builder<T> behaviors) {
            behaviors.isGlobal = true;
            this.globalBehaviors.add(behaviors);
            return this;
        }

        public Builder<T> setNoBehaviorTick(Consumer<MobPatch<?>> noBehaviorTick){
            this.noBehaviorTick = noBehaviorTick;
            return this;
        }

        public Builder<T> setNoBehaviorHurt(TriFunction<MobPatch<?>, DamageSource, AttackResult, AttackResult> noActionOnHurt){
            this.noBehaviorOnHurt = noActionOnHurt;
            return this;
        }

        @SafeVarargs
        public final Builder<T> addStunEvent(StunType stunType, Consumer<MobPatch<?>>... consumers){
            stunEvents.computeIfAbsent(stunType, k -> new ArrayList<>()).addAll(Arrays.asList(consumers));
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
        //通用根属性
        private final String rootName;
        private final List<Behavior<T>> behaviors;
        private final boolean isGlobal;
        Deque<Behavior<T>> lastInsertedBehaviors = new LinkedList<>();
        //private Behavior<T> lastInsertedBehavior = null;
        private final double priority;
        private final double weight;
        private final int maxCooldown;
        private int cooldown;
        //全局专用属性
        //是否在结束后返回原行为
        private final boolean backAfterFinished;

        public BehaviorRoot(Builder<T> builder) {
            this.rootName = builder.rootName;
            this.behaviors = builder.behavior.stream().map(b->b.build(this)).toList();
            this.isGlobal = builder.isGlobal;
            this.priority = builder.priority;
            this.weight = builder.weight;
            this.maxCooldown = builder.maxCooldown;
            this.cooldown = builder.cooldown;
            this.backAfterFinished = builder.backAfterFinished;
        }

        public String getRootName() {
            return rootName;
        }


        public Deque<Behavior<T>> getLastInsertedBehaviors() {
            return lastInsertedBehaviors;
        }

        public void addLastInsertedBehavior(Behavior<T> behavior) {
            this.lastInsertedBehaviors.push(behavior);
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

        public boolean isBackAfterFinished() {
            return backAfterFinished;
        }

        public boolean isGlobal() {
            return isGlobal;
        }


        @SuppressWarnings("UnusedReturnValue")
        public static class Builder<T extends MobPatch<?>> {
            private String rootName = "";
            private final List<Behavior.Builder<T>> behavior = new ArrayList<>();
            private boolean isGlobal = false;
            private double priority = 1;
            private double weight = 1;
            private int maxCooldown = 0;
            private int cooldown = 0;
            private boolean backAfterFinished = false;

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

            public Builder<T> backAfterFinished(boolean backAfterFinished) {
                this.backAfterFinished = backAfterFinished;
                return this;
            }

            public Builder<T> isGlobal(boolean isGlobal) {
                this.isGlobal = isGlobal;
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




    public static class BehaviorParams {

        public static class CommonParams{
            public double priority = 1;                      //优先级
            public double weight = 1;                        //权重
            public boolean canInsertGlobalBehavior = false;      //是否允许接入全局行为
            public List<String> allowedGlobalNameList = new ArrayList<>();   //允许的全局行为名称列表
            public int totalWaitTime = 0;                    //行为结束后的等待窗口时间
            public int waitTime = 0;                               //等待计时器
            public int stopByStun = 1;                       //是否会被眩晕打断其余连段
            public boolean canInterruptParent = false;           //是否能打断父行为
            public boolean canBeInterrupted = false;             //是否能被其他行为打断
            public InterruptType interruptType = InterruptType.TIME;          //打断类型
            public List<Float> interruptedWindow = new ArrayList<>();        //能够被打断的窗口时间
            public int exCoolDown = 0;                       //额外冷却时间
            public int behaviorTime = 0;                     //特定行为的持续时间（游荡、防御）
            public int timeCount = 0;                              //计时器
            public boolean shouldExecuteTimeEvent = false;
            public boolean shouldExecuteHitEvent = false;
        }

        public static class AnimationBehavior{
            public Map<Integer, PhaseParams> phaseParams = new HashMap<>();
            public boolean canApplyPhaseParams = false;
        }

        public static class GuardBehavior<T extends MobPatch<?>>{
            public Consumer<T> counter;
            public CounterType counterType = CounterType.NEVER;
            public boolean canCounter = false;
            public double counterChance = 0.25;
            public int maxGuardHit = Integer.MAX_VALUE;
            public int guardHit = 0;
            public boolean resetGuardTime = false;
            public float guardCost = 1;
        }


    }

    //行为类
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static class Behavior<T extends MobPatch<?>> {
        private final String behaviorName;                  //行为名称
        private final BehaviorType type;                    //主行为类型
        private final Consumer<T> behavior;                 //具体行为
        private final BehaviorRoot<T> behaviorRoot;         //所属的根节点
        private final List<Condition<T>> conditions;        //执行需要满足的条件
        private final List<Behavior<T>> nextBehaviors;      //下一个能执行的行为列表
        private BehaviorState state = BehaviorState.RUNNING;//执行状态

        private final BehaviorParams.CommonParams commonParams;        //行为参数
        private final BehaviorParams.AnimationBehavior animationParams;        //行为参数
        private final BehaviorParams.GuardBehavior<T> guardParams;        //行为参数
        private final CEMobEventManager eventManager;       //事件管理器

        private Behavior(Builder<T> builder, BehaviorRoot<T> behaviorRoot){
            this.behaviorName = builder.behaviorName;
            this.behaviorRoot = behaviorRoot;
            this.behavior = builder.behavior;
            this.type = builder.type;
            this.conditions = builder.conditions;
            this.nextBehaviors = builder.nextBehaviors.stream().map(b->b.build(behaviorRoot)).toList();
            this.commonParams = builder.commonParams;
            this.animationParams = builder.animationParams;
            this.guardParams = builder.guardParams;
            this.eventManager = builder.eventManager;
        }

        public BehaviorRoot<T> getBehaviorRoot() {
            return behaviorRoot;
        }

        public double priority(){
            return commonParams.priority;
        }

        public double weight(){
            return commonParams.weight;
        }

        public float getGuardHitCost(){
            return guardParams.guardCost;
        }


        public Map<Integer, PhaseParams> getPhaseParams() {
            return animationParams.phaseParams;
        }

        public boolean canApplyPhaseParams() {
            return animationParams.canApplyPhaseParams;
        }

        public void setCanApplyPhaseParam(boolean canApplyPhaseParams) {
            this.animationParams.canApplyPhaseParams = canApplyPhaseParams;
        }

        public void setShouldExecuteTimeEvent(boolean shouldExecuteTimeEvent) {
            this.commonParams.shouldExecuteTimeEvent = shouldExecuteTimeEvent;
        }

        public boolean shouldExecuteTimeEvent() {
            return commonParams.shouldExecuteTimeEvent;
        }

        public void resetTimeEventAvailable(){
            for(CEMobEvent<?> event : eventManager.getEvents(TimeEvent.class)){
                ((TimeEvent) event).resetAvailable();
            }
        }

        public <P,R> void executeEvent(Class<? extends CEMobEvent<P>> eventClass, P params){
            this.eventManager.execute(eventClass, params);
        }

        public <P,R> @Nullable R executeEventAndReturn(Class<? extends CEMobEventWithReturn<P,R>> eventClass, P params){
            return this.eventManager.executeAndReturn(eventClass, params);
        }

        public void setShouldExecuteHitEvent(boolean shouldExecuteHitEvent) {
            this.commonParams.shouldExecuteHitEvent = shouldExecuteHitEvent;
        }

        public boolean shouldExecuteHitEvent() {
            return commonParams.shouldExecuteHitEvent;
        }


        public boolean canBeInterrupted(T mobPatch) {
            /*
                根据类型进行划分
                1.时间窗口划分
                2.动作level划分
             */
            if(commonParams.canBeInterrupted && !commonParams.interruptedWindow.isEmpty()){
                if (commonParams.interruptType == InterruptType.TIME) {
                    if(commonParams.behaviorTime == 0 || getType() == BehaviorType.ANIMATION) {
                        AnimationPlayer animator = mobPatch.getAnimator().getPlayerFor(null);
                        if (animator != null) {
                            float prevElapsedTime = animator.getPrevElapsedTime();
                            float elapsedTime = animator.getElapsedTime();
                            return elapsedTime >= commonParams.interruptedWindow.get(0) && elapsedTime < commonParams.interruptedWindow.get(1) || prevElapsedTime >= commonParams.interruptedWindow.get(0) && prevElapsedTime < commonParams.interruptedWindow.get(1);
                        }
                    }
                    else {
                        int elapsedTick = commonParams.behaviorTime - commonParams.timeCount;
                        return elapsedTick * 0.05F >= commonParams.interruptedWindow.get(0) && elapsedTick * 0.05F < commonParams.interruptedWindow.get(1);
                    }
                }
                else if(commonParams.interruptType == InterruptType.LEVEL) {
                    return commonParams.interruptedWindow.contains((float)mobPatch.getEntityState().getLevel());
                }
            }
            return false;
        }

        public boolean checkPredicates(T mobPatch) {
            for(Condition<T> condition : this.conditions) {
                if (!condition.predicate(mobPatch)) {
                    return false;
                }
            }
            return true;
        }

        public void execute(T mobPatch) {
            this.behavior.accept(mobPatch);
            this.eventManager.execute(BehaviorStartEvent.class, new BehaviorStartEvent.EventParams(mobPatch));
            mobPatch.updateEntityState();
            this.animationParams.canApplyPhaseParams = true;
            this.commonParams.shouldExecuteTimeEvent = true;
            this.commonParams.shouldExecuteHitEvent = true;
            commonParams.timeCount = commonParams.behaviorTime;
            commonParams.waitTime = commonParams.totalWaitTime;
            if(mobPatch instanceof CEHumanoidPatch<?> ceHumanoidPatch){
                ceHumanoidPatch.setGuardHitCost(guardParams.guardCost);
            }
            this.state = BehaviorState.RUNNING;
        }

        public List<Behavior<T>> getNextBehaviors() {
            return this.nextBehaviors;
        }

        public void resetAllCooldown() {
            //递归清理所有相关的序列冷却
            if(!this.behaviorRoot.lastInsertedBehaviors.isEmpty()){
                Behavior<T> lastBehavior = behaviorRoot.lastInsertedBehaviors.pop();
                lastBehavior.resetAllCooldown();
            }
            resetCooldown();
        }
        public void resetCooldown() {
            if(this.behaviorRoot.cooldown < this.behaviorRoot.maxCooldown) {
                //System.out.println("根节点["+behaviorRoot.getRootName()+"]进入冷却");
                this.behaviorRoot.resetCooldown(this.commonParams.exCoolDown);
            }
        }

        public boolean isFinished() {
            return this.state == BehaviorState.FINISHED;
        }

        public BehaviorType getType() {
            return this.type;
        }

        public void tick(T mobPatch) {
            if(state == BehaviorState.RUNNING) {
                running(mobPatch);
            }
            else if(state == BehaviorState.WAITING){
                waiting(mobPatch);
            }
        }

        public void running(T mobPatch) {
            if(type == BehaviorType.WANDER || type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) {
                if (commonParams.timeCount > 0) {
                    //索敌朝向
                    if(mobPatch.getTarget()!=null) {
                        mobPatch.rotateTo(mobPatch.getTarget(),360F,true);
                    }

                    //设置游荡
                    if(type == BehaviorType.WANDER || type == BehaviorType.GUARD_WANDER){
                        this.behavior.accept(mobPatch);
                        //如果游荡被取消，直接进入等待
                        if(!CEPatchUtils.isWander(mobPatch)) {
                            behaviorWaiting();
                        }
                    }

                    //设置防御
                    if(type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER){
                        this.behavior.accept(mobPatch);
                        //如果防御被取消，直接进入等待
                        if(!CEPatchUtils.isGuard(mobPatch) && !guardParams.canCounter) {
                            behaviorWaiting();
                        }
                    }


                    if((type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) && guardParams.canCounter){
                        CEPatchUtils.setInCounter(mobPatch, true);
                        if (mobPatch.getEntityState().canBasicAttack()) {
                            guardParams.canCounter = false;
                            stopGuardAndWander(mobPatch);
                            this.guardParams.counter.accept(mobPatch);
                            this.eventManager.execute(CounterStartEvent.class, new CounterStartEvent.EventParams(mobPatch));
                        }
                        else {
                            commonParams.timeCount++;
                        }
                    }

                    commonParams.timeCount--;
                }
                else if (mobPatch.getEntityState().canBasicAttack()) {
                    stopGuardAndWander(mobPatch);
                    behaviorWaiting();
                }

            }
            else if(type == BehaviorType.ANIMATION || type == BehaviorType.CUSTOM){
                if (mobPatch.getEntityState().canBasicAttack() && (!nextBehaviors.isEmpty() || behaviorRoot.isGlobal() || commonParams.canInsertGlobalBehavior)) {
                    behaviorWaiting();
                }
                else if (!mobPatch.getEntityState().inaction() && nextBehaviors.isEmpty()) {
                    behaviorWaiting();
                }
            }
        }

        public void stopGuardAndWander(T mobPatch) {
            //结束游荡
            if(type == BehaviorType.WANDER || type == BehaviorType.GUARD_WANDER) {
                CEPatchUtils.setWander(mobPatch, false);
                mobPatch.getOriginal().getMoveControl().strafe(0,0);
            }
            //结束防御
            if(type == BehaviorType.GUARD || type == BehaviorType.GUARD_WANDER) {
                CEPatchUtils.setGuard(mobPatch, false);
                CEPatchUtils.setInCounter(mobPatch, false);
                AssetAccessor<? extends StaticAnimation> guardAnimation = mobPatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.SWORD_GUARD);
                if (mobPatch.isLogicalClient()) {
                    mobPatch.getAnimator().stopPlaying(guardAnimation);
                } else {
                     mobPatch.stopPlaying(guardAnimation);
                }
            }
        }

        public void waiting(T mobPatch) {
            if(commonParams.waitTime > 0) {
                commonParams.waitTime--;
            }
            else {
                behaviorFinished();
            }
        }

        public boolean whenGuardHit(){
            if(guardParams.resetGuardTime){
                commonParams.timeCount = commonParams.behaviorTime;
            }
            switch (guardParams.counterType){
                case END -> {
                    guardParams.guardHit--;
                    if(guardParams.guardHit <= 0){
                        guardParams.canCounter = true;
                        commonParams.timeCount = 1;
                    }
                }
                case RANDOM -> {
                    if(guardParams.counterChance >= Math.random()){
                        guardParams.canCounter = true;
                        commonParams.timeCount = 1;
                    }
                }
            }
            return guardParams.canCounter;
        }

        public void behaviorWaiting() {
            if(commonParams.totalWaitTime > 0) this.state = BehaviorState.WAITING;
            else behaviorFinished();
        }

        public void behaviorFinished() {
            this.state = BehaviorState.FINISHED;
            commonParams.timeCount = commonParams.behaviorTime;
            commonParams.waitTime = commonParams.totalWaitTime;
            commonParams.shouldExecuteTimeEvent = false;
            commonParams.shouldExecuteHitEvent = false;
            animationParams.canApplyPhaseParams = false;
            guardParams.canCounter = false;
            guardParams.guardHit = guardParams.maxGuardHit;
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
            if(commonParams.stopByStun == 1) return true;
            else if(commonParams.stopByStun == 2){
                return stunType != StunType.SHORT;
            }
            else if(commonParams.stopByStun == 3){
                return stunType != StunType.SHORT && stunType != StunType.LONG;
            }
            else if(commonParams.stopByStun == 4){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD;
            }
            else if(commonParams.stopByStun == 5){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL;
            }
            else if(commonParams.stopByStun == 6){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.KNOCKDOWN;
            }
            else if(commonParams.stopByStun == 7){
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL && stunType != StunType.KNOCKDOWN;
            }
            else {
                return stunType != StunType.SHORT && stunType != StunType.LONG && stunType != StunType.HOLD && stunType != StunType.FALL && stunType != StunType.KNOCKDOWN && stunType != StunType.NEUTRALIZE;
            }
        }


        public int getBehaviorTime() {
            return commonParams.behaviorTime;
        }

        public int getTimeCount() {
            return commonParams.timeCount;
        }

        public boolean canInsertGlobalBehavior() {
            return commonParams.canInsertGlobalBehavior;
        }

        public List<String> getAllowedGlobalNameList() {
            return commonParams.allowedGlobalNameList;
        }

        public boolean canInterruptParent() {
            return commonParams.canInterruptParent;
        }


        //行为类构造器
        @SuppressWarnings("UnusedReturnValue")
        public static class Builder<T extends MobPatch<?>> {
            private String behaviorName = "";
            private Consumer<T> behavior;
            private final List<Condition<T>> conditions = new ArrayList<>();
            private final List<Builder<T>> nextBehaviors = new ArrayList<>();
            private BehaviorType type = BehaviorType.NONE;

            private final BehaviorParams.CommonParams commonParams = new BehaviorParams.CommonParams();        //行为参数
            private final BehaviorParams.AnimationBehavior animationParams = new BehaviorParams.AnimationBehavior();        //行为参数
            private final BehaviorParams.GuardBehavior<T> guardParams = new BehaviorParams.GuardBehavior<>();        //行为参数
            private final CEMobEventManager eventManager = new CEMobEventManager();

            private final LivingEntityPatch.ServerAnimationPacketProvider packetProvider = SPAnimatorControl::new;

            public Builder<T> canInsertGlobalBehavior(boolean canInsertGlobalBehavior,String... allowedGlobalNames) {
                this.commonParams.canInsertGlobalBehavior = canInsertGlobalBehavior;
                this.commonParams.allowedGlobalNameList = List.of(allowedGlobalNames);
                return this;
            }

            public Builder<T> addTimeEvent(TimeEvent timeEvents) {
                this.eventManager.addEvent(TimeEvent.class , timeEvents);
                return this;
            }

            public Builder<T> addTimeEvent(TimeEvent... timeEvents) {
                this.eventManager.addEvent(TimeEvent.class , timeEvents);
                return this;
            }

            public Builder<T> addHitEvent(HitEvent hitEvents) {
                this.eventManager.addEvent(HitEvent.class, hitEvents);
                return this;
            }

            public Builder<T> addHitEvent(HitEvent... hitEvents) {
                this.eventManager.addEvent(HitEvent.class, hitEvents);
                return this;
            }

            public Builder<T> setOnHurtEvent(OnHurtEvent onHurtEvent) {
                this.eventManager.setEventWithReturn(OnHurtEvent.class, onHurtEvent);
                return this;
            }

            public Builder<T> addBlockedEvent(BlockedEvent... blockedEvent) {
                this.eventManager.addEvent(BlockedEvent.class, blockedEvent);
                return this;
            }


            public Builder<T> setBeforeCounterEvent(BeforeCounterEvent event){
                this.eventManager.setEventWithReturn(BeforeCounterEvent.class, event);
                return this;
            }

            public Builder<T> addGuardHitEvent(GuardHitEvent... guardHitEvent){
                this.eventManager.addEvent(GuardHitEvent.class, guardHitEvent);
                return this;
            }

            public Builder<T> counterType(CounterType counterType) {
                this.guardParams.counterType = counterType;
                return this;
            }

            public Builder<T> counterChance(double counterChance) {
                this.guardParams.counterChance = counterChance;
                return this;
            }

            public Builder<T> maxGuardHit(int maxGuardHit) {
                this.guardParams.maxGuardHit = maxGuardHit;
                return this;
            }

            public Builder<T> guardHitCost(float cost){
                this.guardParams.guardCost = cost;
                return this;
            }

            public Builder<T> resetGuardTime(boolean resetGuardTime) {
                this.guardParams.resetGuardTime = resetGuardTime;
                return this;
            }

            public Builder<T> canInterruptParent(boolean canInterruptParent) {
                this.commonParams.canInterruptParent = canInterruptParent;
                return this;
            }

            public Builder<T> interruptedByTime(float start, float end) {
                this.commonParams.canBeInterrupted = true;
                this.commonParams.interruptType = InterruptType.TIME;
                this.commonParams.interruptedWindow = new ArrayList<>(List.of(start,end));
                return this;
            }

            public Builder<T> interruptedByLevel(Integer... levels) {
                this.commonParams.canBeInterrupted = true;
                this.commonParams.interruptType = InterruptType.LEVEL;
                for(int level : levels) {
                    commonParams.interruptedWindow.add((float) level);
                }
                return this;
            }

            public Builder<T> setCooldown(int exCoolDown) {
                this.commonParams.exCoolDown = exCoolDown;
                return this;
            }

            public Builder<T> addCooldown(int exCoolDown) {
                this.commonParams.exCoolDown += exCoolDown;
                return this;
            }

            public Builder<T> waitTime(int waitTime) {
                this.commonParams.totalWaitTime = waitTime;
                return this;
            }


            public Builder<T> stopByStun(int stopByStun) {
                this.commonParams.stopByStun = stopByStun;
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

            public Builder<T> onBehaviorStart(BehaviorStartEvent... behaviors) {
                this.eventManager.addEvent(BehaviorStartEvent.class, behaviors);
                return this;
            }

            @SafeVarargs
            public final Builder<T> onBehaviorStart(Consumer<MobPatch<?>>... customBehaviors) {
                for(Consumer<MobPatch<?>> consumer : customBehaviors) {
                    this.eventManager.addEvent(BehaviorStartEvent.class, new BehaviorStartEvent(consumer));
                }
                return this;
            }

            /**
             *请使用 {@link #onBehaviorStart(BehaviorStartEvent...)} 替代
             */
            @SuppressWarnings("unchecked")
            @Deprecated
            public Builder<T> addExBehavior(Consumer<T> behavior) {
                this.onBehaviorStart(new BehaviorStartEvent((Consumer<MobPatch<?>>) behavior));
                return this;
            }

            @SuppressWarnings("unchecked")
            @Deprecated
            @SafeVarargs
            public final Builder<T> addExBehavior(Consumer<T>... behaviors) {
                for(Consumer<T> behavior : behaviors) {
                    this.onBehaviorStart(new BehaviorStartEvent((Consumer<MobPatch<?>>) behavior));
                }
                return this;
            }

            public Builder<T> setStamina(float stamina) {
                this.onBehaviorStart(new BehaviorStartEvent(mobPatch -> {
                    CEPatchUtils.setStamina(mobPatch, stamina);
                }));
                return this;
            }

            public Builder<T> addStamina(float stamina) {
                this.onBehaviorStart(new BehaviorStartEvent(mobPatch -> {
                    CEPatchUtils.addStamina(mobPatch, stamina);
                }));
                return this;
            }

            public Builder<T> setPhase(int phase){
                this.onBehaviorStart(new BehaviorStartEvent((mobPatch)->{
                    CEPatchUtils.setPhase(mobPatch, phase);
                }));
                return this;
            }


            public Builder<T> addPhase(int add){
                this.onBehaviorStart(new BehaviorStartEvent((mobPatch)->{
                    CEPatchUtils.addPhase(mobPatch, add);
                }));
                return this;
            }

            public Builder<T> wander(int totalTime,float pForward, float pStrafe) {
                this.commonParams.behaviorTime = totalTime;
                this.type = BehaviorType.WANDER;
                this.behavior = (mobPatch)->{
                    CEPatchUtils.setWander(mobPatch, true);
                    mobPatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                };
                return this;
            }


            public Builder<T> wanderWithAnimation(AnimationManager.AnimationAccessor<? extends StaticAnimation> animation, int totalTime, float pForward, float pStrafe) {
                this.commonParams.behaviorTime = totalTime;
                this.type = BehaviorType.WANDER;
                this.behavior = (mobPatch)->{
                    CEPatchUtils.setWander(mobPatch, true);
                    AssetAccessor<? extends StaticAnimation> currentAnimation = Objects.requireNonNull(mobPatch.getAnimator().getPlayerFor(null)).getAnimation().get().getRealAnimation();
                    if(currentAnimation != animation) {
                        mobPatch.playAnimationSynchronized(animation, 0F, this.packetProvider);
                    }
                    mobPatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                };
                return this;
            }

            public Builder<T> guard(int totalTime) {
                commonParams.behaviorTime = totalTime;
                this.type = BehaviorType.GUARD;
                this.behavior = (mobPatch)->{
                    CEPatchUtils.setGuard(mobPatch, true);
                    AssetAccessor<? extends StaticAnimation> currentAnimation = Objects.requireNonNull(mobPatch.getAnimator().getPlayerFor(null)).getAnimation().get().getRealAnimation();
                    AssetAccessor<? extends StaticAnimation> guardAnimation = mobPatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.SWORD_GUARD);
                    if(currentAnimation != guardAnimation && !mobPatch.getEntityState().inaction()) {
                        mobPatch.playAnimationSynchronized(guardAnimation, 0F, this.packetProvider);
                    }
                };
                return this;
            }

            public Builder<T> guardWithWander(int totalTime,float pForward, float pStrafe) {
                return guardWithWander(totalTime,pForward,pStrafe,false);
            }


            public Builder<T> guardWithWander(int totalTime,float pForward, float pStrafe,boolean playGuardAnimation) {
                commonParams.behaviorTime = totalTime;
                this.type = BehaviorType.GUARD_WANDER;
                this.behavior = (mobPatch)->{
                    CEPatchUtils.setGuard(mobPatch, true);
                    CEPatchUtils.setWander(mobPatch, true);
                    mobPatch.getOriginal().getMoveControl().strafe(pForward,pStrafe);
                    if(playGuardAnimation){
                        AssetAccessor<? extends StaticAnimation> currentAnimation = Objects.requireNonNull(mobPatch.getAnimator().getPlayerFor(null)).getAnimation().get().getRealAnimation();
                        AssetAccessor<? extends StaticAnimation> guardAnimation = mobPatch.getAnimator().getLivingAnimation(LivingMotions.BLOCK, Animations.SWORD_GUARD);
                        if(currentAnimation != guardAnimation && !mobPatch.getEntityState().inaction()) {
                            mobPatch.playAnimationSynchronized(guardAnimation, 0F, this.packetProvider);
                        }
                    }
                };
                return this;
            }


            @SuppressWarnings("unchecked")
            public Builder<T> onCounterStart(Consumer<T> behavior) {
                this.eventManager.addEvent(CounterStartEvent.class, new CounterStartEvent((Consumer<MobPatch<?>>) behavior));
                return this;
            }

            @SuppressWarnings("unchecked")
            public final Builder<T> onCounterStart(Consumer<T>... behaviors) {
                for(Consumer<T> behavior : behaviors) {
                    this.eventManager.addEvent(CounterStartEvent.class, new CounterStartEvent((Consumer<MobPatch<?>>) behavior));
                }
                return this;
            }

            public Builder<T> onCounterStart(CounterStartEvent... events) {
                this.eventManager.addEvent(CounterStartEvent.class, events);
                return this;
            }

            public Builder<T> counterAnimation(AssetAccessor<? extends StaticAnimation> counterAnimation,float transitionTime) {
                return counterAnimation(counterAnimation, new AnimationParams().transitionTime(transitionTime));
            }

            public Builder<T> counterAnimation(AssetAccessor<? extends StaticAnimation> counterAnimation, AnimationParams params) {
                this.animationParams.phaseParams.clear();
                this.animationParams.phaseParams.putAll(params.getPhaseParams());
                this.guardParams.counter = (mobPatch)-> {
                    mobPatch.playAnimationSynchronized(counterAnimation, params.getTransitionTime(), this.packetProvider);
                    if(mobPatch instanceof ILivingEntityData livingEntityData) {
                        livingEntityData.combat_evolution$setCanModifySpeed(params.shouldChangeSpeed());
                        livingEntityData.combat_evolution$setAttackSpeed(params.getPlaySpeed());
                    }
                };
                return this;
            }

            public Builder<T> animationBehavior(AnimationManager.AnimationAccessor<? extends StaticAnimation> motion, AnimationParams params) {
                this.type = BehaviorType.ANIMATION;
                this.animationParams.phaseParams.clear();
                this.animationParams.phaseParams.putAll(params.getPhaseParams());
                this.behavior = (mobPatch) -> {
                    mobPatch.playAnimationSynchronized(motion, params.getTransitionTime(), this.packetProvider);
                    if (mobPatch instanceof ILivingEntityData livingEntityData) {
                        livingEntityData.combat_evolution$setCanModifySpeed(params.shouldChangeSpeed());
                        livingEntityData.combat_evolution$setAttackSpeed(params.getPlaySpeed());
                    }
                };
                return this;
            }

            public Builder<T> animationBehavior(AnimationManager.AnimationAccessor<? extends StaticAnimation> motion,float transitionTime) {
                return animationBehavior(motion, new AnimationParams().transitionTime(transitionTime));
            }

            public Builder<T> priority(double priority) {
                this.commonParams.priority = priority;
                return this;
            }

            public Builder<T> weight(double weight) {
                this.commonParams.weight = weight;
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

            public Builder<T> withinCurrentAngle(CurrentAngle.TargetSide side, double degreeFirst, double degreeSecond) {
                this.condition(new CurrentAngle(side, degreeFirst, degreeSecond));
                return this;
            }

            public Builder<T> withinAngle(double minDegree, double maxDegree) {
                this.condition(new TargetInPov(minDegree, maxDegree));
                return this;
            }

            public Builder<T> attackLevel(int min, int max) {
                this.condition(new AttackLevel(min, max));
                return this;
            }

            public Builder<T> attackLevelContain(Integer... levels) {
                this.condition(new AttackLevelContain(levels));
                return this;
            }

            public Builder<T> phaseBetween(int min, int max) {
                this.condition(new PhaseBetween(min, max));
                return this;
            }

            public Builder<T> phaseContain(Integer... phases) {
                this.condition(new PhaseContain(phases));
                return this;
            }

            public Builder<T> targetAnimation(AssetAccessor<? extends StaticAnimation> animation){
                return custom((mobPatch)->{
                    LivingEntity target = mobPatch.getTarget();
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

            public Builder<T> targetGuardBreak(){
                this.condition(new TargetGuardBreak());
                return this;
            }

            public Builder<T> withinAngleHorizontal(double minDegree, double maxDegree) {
                this.condition(new TargetInPov.TargetInPovHorizontal(minDegree, maxDegree));
                return this;
            }

            public Builder<T> health(float health, HealthCheck.Comparator comparator) {
                this.condition(new HealthCheck(health, comparator));
                return this;
            }

            public Builder<T> staminaCheck(float stamina, StaminaCheck.Comparator comparator) {
                this.condition(new StaminaCheck(stamina, comparator));
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

    public enum ContinueCondition{
        CAN_BASIC_ATTACK,
        CAN_USE_SKILL,
        NO_INACTION
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
