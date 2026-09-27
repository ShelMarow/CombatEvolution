package net.shelmarow.combat_evolution.skill;

import net.shelmarow.combat_evolution.effect.CEMobEffects;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

public class CECommonSkillHandler extends Skill {

    private static final UUID EVENT_UUID = UUID.fromString("7f5b9775-5366-4d1d-a399-02f92c4e30f9");

    public CECommonSkillHandler(SkillBuilder<? extends Skill> builder) {
        super(builder.setCategory(CESkillCategories.CE_SKILL_CATEGORY).setResource(Resource.NONE));
    }


    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        PlayerPatch<?> executor = container.getExecutor();
        PlayerEventListener eventListener = executor.getEventListener();

        eventListener.addEventListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, EVENT_UUID, event -> {
            if(event.getPlayerPatch().getOriginal().hasEffect(CEMobEffects.ON_EXECUTION.get())){
                event.setCanceled(true);
            }
        },-1);


    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);

        PlayerPatch<?> executor = container.getExecutor();
        PlayerEventListener eventListener = executor.getEventListener();
        eventListener.removeListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, EVENT_UUID);
    }
}
