package net.shelmarow.combat_evolution.skill;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.item.CECreativeTab;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class CESkills {
    public static Skill CE_SKILL_HANDLER;
    public static Skill SHIELD_COUNTER;

    @SubscribeEvent
    public static void buildSkillEvent(SkillBuildEvent build) {
        SkillBuildEvent.ModRegistryWorker modRegistry = build.createRegistryWorker(CombatEvolution.MOD_ID);

        CE_SKILL_HANDLER = modRegistry.build("ce_skill_handler", CECommonSkillHandler::new,
                CECommonSkillHandler.createBuilder().setCategory(CESkillCategories.CE_SKILL_CATEGORY));

        SHIELD_COUNTER = modRegistry.build("shield_counter", CEShieldCounter::new,
                CEShieldCounter.createBuilder().setCreativeTab(CECreativeTab.CE_TAB.get()));


    }
}
