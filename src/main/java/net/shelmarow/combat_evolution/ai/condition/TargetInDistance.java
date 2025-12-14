package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class TargetInDistance extends Condition.EntityPatchCondition {
    private double min;
    private double max;

    public TargetInDistance() {
    }

    public TargetInDistance(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public TargetInDistance read(CompoundTag tag) {
        this.min = this.assertTag("min", "decimal", tag, NumericTag.class, CompoundTag::getDouble);
        this.max = this.assertTag("max", "decimal", tag, NumericTag.class, CompoundTag::getDouble);
        return this;
    }

    public CompoundTag serializePredicate() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("min", this.min);
        tag.putDouble("max", this.max);
        return tag;
    }

    public boolean predicate(LivingEntityPatch<?> target) {
        double distanceSqr = target.getOriginal().distanceToSqr(target.getTarget());
        return this.min * this.min <= distanceSqr && distanceSqr < this.max * this.max;
    }

    @OnlyIn(Dist.CLIENT)
    public List<ParameterEditor> getAcceptingParameters(Screen screen) {
        return null;
    }
}
