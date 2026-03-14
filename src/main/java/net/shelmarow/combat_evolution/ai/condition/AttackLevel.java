package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class AttackLevel implements Condition<LivingEntityPatch<?>> {
    private int levelMin;
    private int levelMax;

    public AttackLevel(int levelMin, int levelMax) {
        this.levelMin = levelMin;
        this.levelMax = levelMax;
    }

    public AttackLevel() {

    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag compoundTag){
        levelMin = compoundTag.getInt("min");
        levelMax = compoundTag.getInt("max");
        return this;
    }
    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putInt("min", levelMin);
        tag.putInt("max", levelMax);
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
            return levelMin <= level && level <= levelMax;
        }
    }
    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }
}
