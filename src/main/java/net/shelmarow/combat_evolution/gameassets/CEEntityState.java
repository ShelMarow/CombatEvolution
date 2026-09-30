package net.shelmarow.combat_evolution.gameassets;

import yesman.epicfight.api.animation.types.EntityState;

public class CEEntityState {
    public static final EntityState.StateFactor<Boolean> COUNTER_SUSSED = new EntityState.StateFactor<>("counter_sussed", false);
    public static final EntityState.StateFactor<Boolean> CAN_ASSASSINATE = new EntityState.StateFactor<>("can_assassinate", true);
}
