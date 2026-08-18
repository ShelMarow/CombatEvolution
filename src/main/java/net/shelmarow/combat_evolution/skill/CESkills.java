package net.shelmarow.combat_evolution.skill;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.item.CECreativeTab;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;

public class CESkills {
    public static final DeferredRegister<Skill> SKILLS = DeferredRegister.create(EpicFightRegistries.Keys.SKILL, CombatEvolution.MOD_ID);
    public static Skill SHIELD_COUNTER;

    static {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "shield_counter");
        SKILLS.register("shield_counter", () -> SHIELD_COUNTER = Skill.createBuilder(CEShieldCounter::new)
                .setCreativeTab(CECreativeTab.CE_TAB.get()).build(id));
    }
}
