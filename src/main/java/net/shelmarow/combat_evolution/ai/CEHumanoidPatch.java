package net.shelmarow.combat_evolution.ai;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.shelmarow.combat_evolution.ai.goal.CEAnimationAttackGoal;
import net.shelmarow.combat_evolution.ai.goal.CommonChasingGoal;
import net.shelmarow.combat_evolution.ai.iml.ILivingEntityData;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeLivingMotion;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.effect.EpicFightMobEffects;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import java.util.*;

public abstract class CEHumanoidPatch extends MobPatch<PathfinderMob> {
    protected final Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions = new HashMap<>();
    protected final Map<WeaponCategory, Map<Style, List<AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> guardHitMotions = new HashMap<>();
    protected final Map<WeaponCategory, Map<Style, CECombatBehaviors.Builder<MobPatch<?>>>> weaponAttackMotions = new HashMap<>();

    protected int breakTime = 40;
    protected int recoverTime = 60;
    protected int recoverTickCount = 0;

    protected int lastActionTime = 0;
    protected int staminaRegenDelay = 60;

    protected int attackRadius = 1;

    public CEHumanoidPatch(Factions factions) {
        super(factions);
        this.setWeaponMotions();
    }

    @Override
    public void initAI(){
        super.initAI();
        setAIAsInfantry();
    }

    @Override
    protected void selectGoalToRemove(Set<Goal> toRemove) {
        for(WrappedGoal wrappedGoal : this.original.goalSelector.getAvailableGoals()) {
            Goal goal = wrappedGoal.getGoal();
            if (goal instanceof MeleeAttackGoal || goal instanceof AnimatedAttackGoal ||
                    goal instanceof CEAnimationAttackGoal<?> || goal instanceof RangedAttackGoal ||
                    goal instanceof TargetChasingGoal || goal instanceof CommonChasingGoal) {
                toRemove.add(goal);
            }
        }
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);
        initLivingMotions(animator);
    }

    public void initLivingMotions(Animator animator){
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
    }

    @Override
    public void onAddedToWorld(){
        CEPatchUtils.setStamina(this,CEPatchUtils.getMaxStamina(this));
        modifyLivingMotionByCurrentItem();
    }

    @Override
    public void tick(LivingEvent.LivingTickEvent event) {
        super.tick(event);

        if(this instanceof ILivingEntityData entityData){
            //处理耐力状态
            float maxStamina = CEPatchUtils.getMaxStamina(this);
            float currentStamina = CEPatchUtils.getStamina(this);
            StaminaStatus staminaStatus = CEPatchUtils.getStaminaStatus(this);


            if(staminaStatus == StaminaStatus.COMMON){
                //普通状态下重置计时器
                if(recoverTickCount != 0) {
                    recoverTickCount = 0;
                }
                //如果耐力归零，切换至破防状态
                if(currentStamina <= 0){
                    CEPatchUtils.setStaminaStatus(this, StaminaStatus.BREAK);
                }
                //如果有耐力回复属性，则在一段时间未行动时回复耐力
                else if(original.getAttribute(EpicFightAttributes.STAMINA_REGEN.get()) != null){
                    if(state.inaction() || entityData.combat_evolution$isGuard()){
                        lastActionTime = original.tickCount;
                    }
                    else if(original.tickCount - lastActionTime > staminaRegenDelay && currentStamina < maxStamina){
                        float regenSpeed = (float) original.getAttributeValue(EpicFightAttributes.STAMINA_REGEN.get());
                        CEPatchUtils.setStamina(this, currentStamina + maxStamina * 0.01F * regenSpeed);
                    }
                }
            }
            else {
                //非普通状态下持续计时
                recoverTickCount++;
                //破防状态下，等待一段时间，然后切换至恢复状态
                if(staminaStatus == StaminaStatus.BREAK){
                    if(recoverTickCount >= breakTime){
                        CEPatchUtils.setStaminaStatus(this, StaminaStatus.RECOVER);
                    }
                }
                //恢复状态下，持续恢复耐力值，恢复满后切换至普通状态
                else if (staminaStatus == StaminaStatus.RECOVER) {
                    original.addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 5, 0, false, false, false));
                    float progress = Mth.clamp((float) (recoverTickCount - breakTime) / recoverTime,0F,1F);
                    currentStamina = Mth.lerp(progress,0,maxStamina);
                    entityData.combat_evolution$setStamina(currentStamina);
                    if(progress == 1F){
                        CEPatchUtils.setStaminaStatus(this, StaminaStatus.COMMON);
                    }
                }
            }

            //防御状态下，移动速度降低
            AttributeInstance instance = original.getAttribute(Attributes.MOVEMENT_SPEED);
            AttributeModifier modifier = new AttributeModifier(UUID.fromString("086f00c3-2763-463e-a64e-b19c8959d4bd"),"guard_move_speed",-0.45D, AttributeModifier.Operation.MULTIPLY_TOTAL);
            if (instance != null) {
                if (entityData.combat_evolution$isGuard()) {
                    if (!instance.hasModifier(modifier)) {
                        instance.addPermanentModifier(modifier);
                    }
                }
                else if(instance.hasModifier(modifier)){
                    instance.removeModifier(modifier);
                }
            }
        }

        //眩晕时触发连段打断
        this.original.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof CEAnimationAttackGoal<?>).forEach(g->{
            CEAnimationAttackGoal<?> goal = (CEAnimationAttackGoal<?>) g.getGoal();
            AssetAccessor<? extends DynamicAnimation> animation = Objects.requireNonNull(getAnimator().getPlayerFor(null)).getAnimation();
            for(StunType stunType : StunType.values()){
                if(stunType != StunType.NONE && getHitAnimation(stunType) == animation){
                    goal.interruptByStun(stunType);
                }
            }
        });
    }

    @Override
    public AttackResult tryHurt(DamageSource damageSource, float amount) {
        AttackResult result = super.tryHurt(damageSource, amount);

        if(damageSource.getDirectEntity() != null && result.resultType == AttackResult.ResultType.SUCCESS) {
            ILivingEntityData entityData = (ILivingEntityData) this;
            //防御受击
            if(entityData.combat_evolution$isGuard() && isBlockableSource(damageSource) && !isStunned()){
                if(!entityData.combat_evolution$isInCounter()) {
                    onGuardHit(damageSource);
                }
                return AttackResult.blocked(0);
            }

            //正常受伤
            onCommonHurt(damageSource);
        }

        return result;
    }

    public boolean isBlockableSource(DamageSource damageSource){
        return !damageSource.is(EpicFightDamageTypeTags.UNBLOCKALBE) && !damageSource.is(EpicFightDamageTypeTags.GUARD_PUNCTURE);
    }

    public void onCommonHurt(DamageSource damageSource) {
        EpicFightDamageSource efSource = damageSource instanceof EpicFightDamageSource ? (EpicFightDamageSource) damageSource : null;

        if (!original.hasEffect(EpicFightMobEffects.STUN_IMMUNITY.get()) && !original.hasEffect(CEMobEffects.FULL_STUN_IMMUNITY.get())) {
            //普通受击削减耐力
            float hurtImpactPercent = getHurtImpactPercent(damageSource);
            float impact = 0.5F * hurtImpactPercent;
            if (efSource != null) {
                impact = efSource.calculateImpact() * hurtImpactPercent;
            }
            dealStaminaDamage(damageSource,impact);
        }
    }

    public void onGuardHit(DamageSource damageSource){
        EpicFightDamageSource efSource = damageSource instanceof EpicFightDamageSource ? (EpicFightDamageSource) damageSource : null;

        float impact = 0.5F;
        if (efSource != null) {
            impact = efSource.calculateImpact();
        }
        impact *= getGuardHitImpactPercent(damageSource);

        if(!dealStaminaDamage(damageSource,impact)){
            if (damageSource.getDirectEntity() != null) {
                knockBackEntity(damageSource.getDirectEntity().position(), 0.15F);
            }

            //是否能进行反击
            boolean canCounter = BehaviorUtils.onGuardHit(this);
            playGuardHitAnimation(damageSource,canCounter);
        }
    }

    public float getGuardHitImpactPercent(DamageSource damageSource){
        return 1F;
    }

    public void playGuardHitAnimation(DamageSource damageSource, boolean canCounter){
        //播放防御动画
        AnimationManager.AnimationAccessor<? extends StaticAnimation> guardHit = getGuardHitAnimation(damageSource);
        this.playAnimationSynchronized(guardHit,0F);
        playGuardHitSound();
    }

    public boolean dealStaminaDamage(DamageSource damageSource,float amount){
        //只有在正常状态下能造成耐力伤害
        if(CEPatchUtils.getStaminaStatus(this) == StaminaStatus.COMMON){
            float stamina = CEPatchUtils.getStamina(this);
            CEPatchUtils.setStamina(this,stamina - amount);
            if (amount >= stamina) {
                onBreak(damageSource);
                CEPatchUtils.setStaminaStatus(this, StaminaStatus.BREAK);
                return true;
            }
        }
        return false;
    }


    public void onBreak(DamageSource damageSource){
        EpicFightDamageSource efSource = damageSource instanceof EpicFightDamageSource ? (EpicFightDamageSource) damageSource : null;
        if (efSource != null) {
            efSource.setStunType(StunType.NONE);
            Vec3 sourcePosition = efSource.getInitialPosition();
            if (sourcePosition != null) {
                original.lookAt(EntityAnchorArgument.Anchor.FEET, sourcePosition);
            }
        }

        //切换状态，并进入硬直
        if(this.applyStun(StunType.NEUTRALIZE, 0F)){
            original.forceAddEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 100), original);

            Vec3 eyePosition = this.original.getEyePosition();
            Vec3 viewVec = this.original.getLookAngle().scale(2.0F);
            Vec3 pos = new Vec3(eyePosition.x + viewVec.x, eyePosition.y + viewVec.y, eyePosition.z + viewVec.z);
            this.getOriginal().level().addParticle(EpicFightParticles.NEUTRALIZE.get(), pos.x, pos.y, pos.z, 0, 0, 0);
            playGuardBreakSound();
        }
    }

    @Override
    public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> blocker) {
        //交给子类重写
    }

    public void onAttackParried(DamageSource damageSource, LivingEntityPatch<?> blocker) {
        //交给子类重写
    }

    public void playGuardBreakSound(){
        this.playSound(EpicFightSounds.NEUTRALIZE_MOBS.get(), 1.0F,1.0F);
    }

    public void playGuardHitSound(){
        this.playSound(EpicFightSounds.CLASH.get(),1.0F,1.0F);
    }

    public float getHurtImpactPercent(DamageSource damageSource){
        return 0.35F;
    }

    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getGuardHitAnimation(DamageSource damageSource){
        //获取随机防御受击动画
        CapabilityItem capabilityItem = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        WeaponCategory category = capabilityItem.getWeaponCategory();
        Style style = capabilityItem.getStyle(this);

        List<AnimationManager.AnimationAccessor<? extends StaticAnimation>> list =
                guardHitMotions.getOrDefault(category, new HashMap<>()).getOrDefault(style, new ArrayList<>());

        if(list != null && !list.isEmpty()){
            int index = original.getRandom().nextInt(0, list.size());
            return list.get(index);
        }

        return Animations.EMPTY_ANIMATION;
    }

    protected abstract void setWeaponMotions();

    protected CECombatBehaviors.Builder<MobPatch<?>> getCustomWeaponMotionBuilder() {
        CapabilityItem itemCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        if (this.weaponAttackMotions.containsKey(itemCap.getWeaponCategory())) {
            Map<Style, CECombatBehaviors.Builder<MobPatch<?>>> motionByStyle = this.weaponAttackMotions.get(itemCap.getWeaponCategory());
            Style style = itemCap.getStyle(this);
            if (motionByStyle.containsKey(style) || motionByStyle.containsKey(CapabilityItem.Styles.COMMON)) {
                return motionByStyle.getOrDefault(style, motionByStyle.get(CapabilityItem.Styles.COMMON));
            }
        }
        return DefaultCombatBehavior.FIST;
    }

    protected void setAIAsInfantry() {
        CECombatBehaviors.Builder<MobPatch<?>> builder = this.getCustomWeaponMotionBuilder();
        if(builder != null) {
            this.original.goalSelector.addGoal(0, new CEAnimationAttackGoal<>(this, builder.build()));
            this.original.goalSelector.addGoal(1, new CommonChasingGoal(this, attackRadius));
        }

    }

    @Override
    public void updateMotion(boolean considerInaction) {
        if (this.original.getHealth() <= 0.0F) {
            this.currentLivingMotion = LivingMotions.DEATH;
        }
        else if (this.state.inaction() && considerInaction) {
            this.currentLivingMotion = LivingMotions.IDLE;
        }
        else if (this.original.getVehicle() != null) {
            this.currentLivingMotion = LivingMotions.MOUNT;
        }
        else if (!(this.original.getDeltaMovement().y < (double)-0.55F) && !this.isAirborneState()) {
            if (this.original.walkAnimation.speed() > 0.08F && this.original.walkAnimation.speed() <= 0.72) {
                this.currentLivingMotion = LivingMotions.WALK;
            }
            else if (this.original.walkAnimation.speed() > 0.72) {
                if (this.original.isAggressive()) {
                    this.currentLivingMotion = LivingMotions.CHASE;
                } else {
                    this.currentLivingMotion = LivingMotions.RUN;
                }
            }
            else {
                this.currentLivingMotion = LivingMotions.IDLE;
            }
        }
        else {
            this.currentLivingMotion = LivingMotions.FALL;
        }

        this.currentCompositeMotion = this.currentLivingMotion;
    }

    @Override
    public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
        super.initAI();
        this.setAIAsInfantry();
        this.modifyLivingMotionByCurrentItem();

        if (hand == InteractionHand.OFF_HAND) {
            if (!from.isEmpty()) {
                from.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED).forEach(attributeModifier -> {
                    AttributeInstance instance = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get());
                    if (instance != null && attributeModifier != null) {
                        instance.removeModifier(attributeModifier);
                    }
                });
            }

            if (!fromCap.isEmpty()) {
                fromCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(Attributes.ATTACK_SPEED).forEach(mod -> {
                    AttributeInstance atkSpeed = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get());
                    if (atkSpeed != null && mod != null) {
                        atkSpeed.removeModifier(mod);
                    }
                });

                fromCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.ARMOR_NEGATION.get()).forEach(mod -> {
                    AttributeInstance armorNeg = this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get());
                    if (armorNeg != null && mod != null) {
                        armorNeg.removeModifier(mod);
                    }
                });

                fromCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.IMPACT.get()).forEach(mod -> {
                    AttributeInstance impact = this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT.get());
                    if (impact != null && mod != null) {
                        impact.removeModifier(mod);
                    }
                });

                fromCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.MAX_STRIKES.get()).forEach(mod -> {
                    AttributeInstance maxStrikes = this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES.get());
                    if (maxStrikes != null && mod != null) {
                        maxStrikes.removeModifier(mod);
                    }
                });
            }

            if (!to.isEmpty()) {
                to.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED).forEach(mod -> {
                    AttributeInstance atkSpeed = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get());
                    if (atkSpeed != null && mod != null) {
                        atkSpeed.addTransientModifier(mod);
                    }
                });
            }

            if (!toCap.isEmpty()) {
                toCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(Attributes.ATTACK_SPEED).forEach(mod -> {
                    AttributeInstance atkSpeed = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED.get());
                    if (atkSpeed != null && mod != null) {
                        atkSpeed.addTransientModifier(mod);
                    }
                });

                toCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.ARMOR_NEGATION.get()).forEach(mod -> {
                    AttributeInstance armorNeg = this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION.get());
                    if (armorNeg != null && mod != null) {
                        armorNeg.addTransientModifier(mod);
                    }
                });

                toCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.IMPACT.get()).forEach(mod -> {
                    AttributeInstance impact = this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT.get());
                    if (impact != null && mod != null) {
                        impact.addTransientModifier(mod);
                    }
                });

                toCap.getAttributeModifiers(EquipmentSlot.MAINHAND, this).get(EpicFightAttributes.MAX_STRIKES.get()).forEach(mod -> {
                    AttributeInstance maxStrikes = this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES.get());
                    if (maxStrikes != null && mod != null) {
                        maxStrikes.addTransientModifier(mod);
                    }
                });
            }
        }
    }


    public void modifyLivingMotionByCurrentItem() {
        Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> oldLivingAnimations = this.getAnimator().getLivingAnimations();
        Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> newLivingAnimations = Maps.newHashMap();

        CapabilityItem mainHandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
        CapabilityItem offHandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);

        Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> livingMotionModifiers = new HashMap<>(mainHandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND));
        livingMotionModifiers.putAll(offHandCap.getLivingMotionModifier(this, InteractionHand.OFF_HAND));

        boolean hasChange = false;

        for (Map.Entry<LivingMotion, AssetAccessor<? extends StaticAnimation>> entry : livingMotionModifiers.entrySet()) {
            AssetAccessor<? extends StaticAnimation> aniamtion = entry.getValue();

            if (!oldLivingAnimations.containsKey(entry.getKey())) {
                hasChange = true;
            } else if (oldLivingAnimations.get(entry.getKey()) != aniamtion) {
                hasChange = true;
            }

            newLivingAnimations.put(entry.getKey(), aniamtion);
        }

        if (this.weaponLivingMotions.containsKey(mainHandCap.getWeaponCategory())) {
            Map<Style, Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>>> byStyle = this.weaponLivingMotions.get(mainHandCap.getWeaponCategory());
            Style style = mainHandCap.getStyle(this);

            if (byStyle.containsKey(style) || byStyle.containsKey(CapabilityItem.Styles.COMMON)) {
                Set<Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>>> animModifierSet = byStyle.getOrDefault(style, byStyle.get(CapabilityItem.Styles.COMMON));

                for (Pair<LivingMotion, AnimationManager.AnimationAccessor<? extends StaticAnimation>> pair : animModifierSet) {
                    newLivingAnimations.put(pair.getFirst(), pair.getSecond());
                }
            }
        }

        if (!hasChange) {
            for (LivingMotion oldLivingMotion : oldLivingAnimations.keySet()) {
                if (!newLivingAnimations.containsKey(oldLivingMotion)) {
                    hasChange = true;
                    break;
                }
            }
        }

        if (hasChange) {
            this.getAnimator().resetLivingAnimations();
            newLivingAnimations.forEach(this.getAnimator()::addLivingAnimation);

            SPChangeLivingMotion msg = new SPChangeLivingMotion(this.original.getId());
            msg.putEntries(newLivingAnimations.entrySet());
            if(!isLogicalClient()) {
                EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(msg, this.original);
            }
        }
    }


    @Override
    public boolean applyStun(StunType stunType, float stunTime) {
        if(CEPatchUtils.getStaminaStatus(this) == StaminaStatus.RECOVER){
            return false;
        }
        if (stunType == StunType.NEUTRALIZE && (CEPatchUtils.getStamina(this) != 0 || CEPatchUtils.getStaminaStatus(this) != StaminaStatus.COMMON)) {
            stunType = StunType.LONG;
        }
        return super.applyStun(stunType, stunTime);
    }

    @Override
    public AnimationManager.AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
        switch (stunType) {
            case LONG -> {
                return Animations.BIPED_HIT_LONG;
            }
            case SHORT, HOLD -> {
                return Animations.BIPED_HIT_SHORT;
            }
            case KNOCKDOWN -> {
                return Animations.BIPED_KNOCKDOWN;
            }
            case FALL -> {
                return Animations.BIPED_LANDING;
            }
            case NEUTRALIZE -> {
                return Animations.BIPED_COMMON_NEUTRALIZED;
            }
            default -> {
                return null;
            }
        }
    }
}
