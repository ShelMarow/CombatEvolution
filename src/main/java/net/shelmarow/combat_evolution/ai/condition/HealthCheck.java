package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Locale;

public class HealthCheck extends Condition.EntityPatchCondition {
    private float health;
    private Comparator comparator;

    public HealthCheck() {
        this.health = 0.0F;
    }

    public HealthCheck(float health, Comparator comparator) {
        this.health = health;
        this.comparator = comparator;
    }

    public HealthCheck read(CompoundTag tag) {
        this.health = this.assertTag("health", "decimal", tag, NumericTag.class, CompoundTag::getFloat);
        this.comparator = this.assertEnumTag("comparator", Comparator.class, tag);
        return this;
    }

    public CompoundTag serializePredicate() {
        CompoundTag tag = new CompoundTag();
        tag.putString("comparator", this.comparator.toString().toLowerCase(Locale.ROOT));
        tag.putFloat("health", this.health);
        return tag;
    }

    public boolean predicate(LivingEntityPatch<?> target) {
        switch (this.comparator) {
            case LESS_ABSOLUTE ->{
                return this.health > target.getOriginal().getHealth();
            }
            case LESS_ABSOLUTE_CONTAIN -> {
                return this.health >= target.getOriginal().getHealth();
            }
            case GREATER_ABSOLUTE -> {
                return this.health < target.getOriginal().getHealth();
            }
            case GREATER_ABSOLUTE_CONTAIN -> {
                return this.health <= target.getOriginal().getHealth();
            }
            case LESS_RATIO -> {
                return this.health > target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
            }
            case LESS_RATIO_CONTAIN -> {
                return this.health >= target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
            }
            case GREATER_RATIO -> {
                return this.health < target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
            }
            case GREATER_RATIO_CONTAIN -> {
                return this.health <= target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
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

