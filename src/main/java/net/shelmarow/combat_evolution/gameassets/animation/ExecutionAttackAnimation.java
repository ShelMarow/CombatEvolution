package net.shelmarow.combat_evolution.gameassets.animation;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.shelmarow.combat_evolution.damage_source.CEDamageTypeTags;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.ValueModifier;

import java.util.HashSet;
import java.util.Set;

public class ExecutionAttackAnimation extends AttackAnimation {

    public ExecutionAttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        this(transitionTime, antic, preDelay, contact, recovery,InteractionHand.MAIN_HAND, collider, colliderJoint, accessor, armature);
    }

    public ExecutionAttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        this(transitionTime, accessor, armature, new ExecutionPhase(true,0F, antic, preDelay, contact, recovery, Float.MAX_VALUE, hand, colliderJoint, collider));
    }

    public ExecutionAttackAnimation(float transitionTime, AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature, ExecutionPhase... phases) {
        super(transitionTime, accessor, armature, phases);
        this.addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100F));
        this.addProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0));
        this.newTimePair(0.0F, Float.MAX_VALUE).addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false);
        this.newTimePair(0.0F, Float.MAX_VALUE).addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false);
    }


    public ExecutionAttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, String path, AssetAccessor<? extends Armature> armature) {
        this(convertTime, path, armature, new ExecutionPhase(true, 0F, antic, preDelay, contact, recovery, Float.MAX_VALUE, hand, colliderJoint, collider));
    }

    public ExecutionAttackAnimation(float convertTime, String path, AssetAccessor<? extends Armature> armature, ExecutionPhase... phases) {
        super(convertTime, path, armature, phases);
        this.addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(100F));
        this.addProperty(AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(0));
        this.newTimePair(0.0F, Float.MAX_VALUE).addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false);
        this.newTimePair(0.0F, Float.MAX_VALUE).addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false);
    }


    public static class ExecutionPhase extends Phase {

        public ExecutionPhase(boolean isFinished, float start, float antic, float contact, float recovery, float end, Joint joint, Collider collider) {
            this(isFinished, start, antic, contact, recovery, end, InteractionHand.MAIN_HAND, joint, collider);
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float contact, float recovery, float end, InteractionHand hand, Joint joint, Collider collider) {
            this(isFinished, start, antic, antic, contact, recovery, end, hand, joint, collider);
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float preDelay, float contact, float recovery, float end, Joint joint, Collider collider) {
            this(isFinished, start, antic, preDelay, contact, recovery, end, InteractionHand.MAIN_HAND, joint, collider);
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float preDelay, float contact, float recovery, float end, InteractionHand hand, Joint joint, Collider collider) {
            this(isFinished, start, antic, preDelay, contact, recovery, end, false, hand, joint, collider);
        }

        public ExecutionPhase(boolean isFinished, InteractionHand hand, Joint joint, Collider collider) {
            this(isFinished,0, 0, 0, 0, 0, 0, true, hand, joint, collider);
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float preDelay, float contact, float recovery, float end, boolean noStateBind, InteractionHand hand, Joint joint, Collider collider) {
            this(isFinished, start, antic, preDelay, contact, recovery, end, noStateBind, hand, JointColliderPair.of(joint, collider));
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float preDelay, float contact, float recovery, float end, InteractionHand hand, JointColliderPair... colliders) {
            this(isFinished, start, antic, preDelay, contact, recovery, end,false, hand, colliders);
        }

        public ExecutionPhase(boolean isFinished, float start, float antic, float preDelay, float contact, float recovery, float end, boolean noStateBind, InteractionHand hand, JointColliderPair... colliders) {
            super(start, antic, preDelay, contact, recovery, end, noStateBind, hand, colliders);
            Set<TagKey<DamageType>> tags = new HashSet<>();
            tags.add(CEDamageTypeTags.EXECUTION);
            tags.add(DamageTypeTags.BYPASSES_ARMOR);
            tags.add(DamageTypeTags.BYPASSES_ENCHANTMENTS);
            tags.add(DamageTypeTags.BYPASSES_EFFECTS);
            if (isFinished) {
                tags.add(CEDamageTypeTags.EXECUTION_FINISHED);
            }
            this.addProperty(AnimationProperty.AttackPhaseProperty.SOURCE_TAG, tags);
        }


        public <V> ExecutionPhase addProperty(AnimationProperty.AttackPhaseProperty<V> propertyType, V value) {
            super.addProperty(propertyType, value);
            return this;
        }
    }
}
