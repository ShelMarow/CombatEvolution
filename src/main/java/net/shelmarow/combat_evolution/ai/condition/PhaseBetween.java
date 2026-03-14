package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class PhaseBetween implements Condition<LivingEntityPatch<?>> {
    private int phaseMin;
    private int phaseMax;

    public PhaseBetween(int min, int max) {
        this.phaseMin = min;
        this.phaseMax = max;
    }

    public PhaseBetween() {

    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag compoundTag){
        phaseMin = compoundTag.getInt("min");
        phaseMax = compoundTag.getInt("max");
        return this;
    }
    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putInt("min", phaseMin);
        tag.putInt("max", phaseMax);
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        int phase = CEPatchUtils.getPhase(livingEntityPatch);
        return phaseMin <= phase && phase <= phaseMax;
    }
    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }
}
