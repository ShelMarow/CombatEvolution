package net.shelmarow.combat_evolution.ai.condition;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import yesman.epicfight.data.conditions.Condition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class EntityTag implements Condition<LivingEntityPatch<?>> {
    private String entity_tag;

    public EntityTag(String entity_tag) {
        this.entity_tag = entity_tag;
    }

    public EntityTag() {

    }

    @Override
    public Condition<LivingEntityPatch<?>> read(CompoundTag compoundTag){
        entity_tag = compoundTag.getString("entity_tag");
        return this;
    }
    @Override
    public CompoundTag serializePredicate(){
        CompoundTag tag = new CompoundTag();
        tag.putString("entity_tag", this.entity_tag);
        return tag;
    }

    @Override
    public boolean predicate(LivingEntityPatch<?> livingEntityPatch){
        if(livingEntityPatch.getOriginal() instanceof Mob mob){
            if(mob.getTarget()!=null) {
                return mob.getTarget().getTags().contains(this.entity_tag);
            }
        }
        return false;
    }
    @Override
    public List<ParameterEditor> getAcceptingParameters(Screen screen){
        return null;
    }
}
