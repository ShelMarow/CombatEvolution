package net.shelmarow.combat_evolution.skill;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.item.EpicFightCreativeTabs;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class CESkills {
    public static Skill SHIELD_COUNTER;

    @SubscribeEvent
    public static void buildSkillEvent(SkillBuildEvent build) {
        SkillBuildEvent.ModRegistryWorker modRegistry = build.createRegistryWorker(CombatEvolution.MOD_ID);

        SHIELD_COUNTER = modRegistry.build("shield_counter", CEShieldCounter::new, CEShieldCounter.createBuilder().setCreativeTab(EpicFightCreativeTabs.ITEMS.get()));
    }
}
