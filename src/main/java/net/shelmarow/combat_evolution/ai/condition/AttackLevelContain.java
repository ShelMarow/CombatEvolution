package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttackLevelContain implements Condition<LivingEntityPatch<?>> {
    private final List<Integer> attackLevels = new ArrayList<>();

    public AttackLevelContain(Integer[] levels) {
        this.attackLevels.addAll(Arrays.asList(levels));
    }

    public AttackLevelContain(){

    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag compoundTag){
        attackLevels.clear();
        ListTag list = compoundTag.getList("levels", Tag.TAG_INT);
        for (int i = 0; i < list.size(); i++) {
            attackLevels.add(list.getInt(i));
        }
        return this;
    }
    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.getIntArray("levels");
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(livingEntityPatch.getTarget(), LivingEntityPatch.class);
        if (targetPatch == null) {
            return false;
        }
        else {
            int level = targetPatch.getEntityState().getLevel();
            return attackLevels.contains(level);
        }
    }
    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }
}
