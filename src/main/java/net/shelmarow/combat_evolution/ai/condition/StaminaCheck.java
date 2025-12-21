package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Locale;

public class StaminaCheck extends Condition.EntityPatchCondition {
    private float stamina;
    private Comparator comparator;

    public StaminaCheck() {
        this.stamina = 0.0F;
    }

    public StaminaCheck(float health, Comparator comparator) {
        this.stamina = health;
        this.comparator = comparator;
    }

    public StaminaCheck read(CompoundTag tag) {
        this.stamina = this.assertTag("stamina", "decimal", tag, NumericTag.class, CompoundTag::getFloat);
        this.comparator = this.assertEnumTag("comparator", Comparator.class, tag);
        return this;
    }

    public CompoundTag serializePredicate() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("stamina", this.stamina);
        tag.putString("comparator", this.comparator.toString().toLowerCase(Locale.ROOT));
        return tag;
    }

    public boolean predicate(LivingEntityPatch<?> target) {
        float currentStamina = CEPatchUtils.getStamina(target);
        float staminaRatio = CEPatchUtils.getStaminaPercent(target);
        switch (this.comparator) {
            case LESS_ABSOLUTE ->{
                return this.stamina > currentStamina;
            }
            case LESS_ABSOLUTE_CONTAIN -> {
                return this.stamina >= currentStamina;
            }
            case GREATER_ABSOLUTE -> {
                return this.stamina < currentStamina;
            }
            case GREATER_ABSOLUTE_CONTAIN -> {
                return this.stamina <= currentStamina;
            }
            case LESS_RATIO -> {
                return this.stamina > staminaRatio;
            }
            case LESS_RATIO_CONTAIN -> {
                return this.stamina >= staminaRatio;
            }
            case GREATER_RATIO -> {
                return this.stamina < staminaRatio;
            }
            case GREATER_RATIO_CONTAIN -> {
                return this.stamina <= staminaRatio;
            }
            default -> {
                return true;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public List<ParameterEditor> getAcceptingParameters(Screen screen) {
        return null;
    }

    public enum Comparator {
        GREATER_ABSOLUTE,
        GREATER_ABSOLUTE_CONTAIN,

        LESS_ABSOLUTE,
        LESS_ABSOLUTE_CONTAIN,

        GREATER_RATIO,
        GREATER_RATIO_CONTAIN,

        LESS_RATIO,
        LESS_RATIO_CONTAIN,
    }
}

