package net.shelmarow.combat_evolution.skill;

import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

public enum CESkillSlots implements SkillSlot {
    CE_SKILL_SLOT(CESkillCategories.CE_SKILL_CATEGORY);

    final SkillCategory skillCategory;
    final int id;

    CESkillSlots(SkillCategory skillCategory) {
        this.skillCategory = skillCategory;
        this.id = SkillSlot.ENUM_MANAGER.assign(this);
    }

    @Override
    public SkillCategory category() {
        return skillCategory;
    }

    @Override
    public int universalOrdinal() {
        return id;
    }
}
