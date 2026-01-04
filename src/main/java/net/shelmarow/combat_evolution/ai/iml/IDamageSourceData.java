package net.shelmarow.combat_evolution.ai.iml;

import yesman.epicfight.api.animation.types.AttackAnimation;

public interface IDamageSourceData {
    void setSourcePhase(AttackAnimation.Phase phase);
    int getSourcePhaseIndex();
}
