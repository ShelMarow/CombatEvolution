package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.List;

public class PhaseContain implements Condition<LivingEntityPatch<?>> {
    private final List<Integer> phaseList = new ArrayList<>();

    public PhaseContain(Integer... phases) {
        this.phaseList.addAll(List.of(phases));
    }

    public PhaseContain() {

    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag compoundTag){
        phaseList.clear();
        ListTag list = compoundTag.getList("phases", Tag.TAG_INT);
        for (int i = 0; i < list.size(); i++) {
            phaseList.add(list.getInt(i));
        }
        return this;
    }
    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("phases", phaseList);
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        int phase = CEPatchUtils.getPhase(livingEntityPatch);
        return phaseList.contains(phase);
    }
    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }
}
