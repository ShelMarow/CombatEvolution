package net.shelmarow.combat_evolution.skill;

import yesman.epicfight.skill.SkillCategory;

public enum CESkillCategories implements SkillCategory {

    CE_SKILL_CATEGORY(false, false, false);

    final boolean shouldSave;
    final boolean shouldSynchronize;
    final boolean modifiable;
    final int id;

    CESkillCategories(boolean shouldSave, boolean shouldSynchronize, boolean modifiable) {
        this.shouldSave = shouldSave;
        this.shouldSynchronize = shouldSynchronize;
        this.modifiable = modifiable;
        this.id = SkillCategory.ENUM_MANAGER.assign(this);
    }

    @Override
    public boolean shouldSave() {
        return shouldSave;
    }

    @Override
    public boolean shouldSynchronize() {
        return shouldSynchronize;
    }

    @Override
    public boolean learnable() {
        return modifiable;
    }

    @Override
    public int universalOrdinal() {
        return id;
    }
}
