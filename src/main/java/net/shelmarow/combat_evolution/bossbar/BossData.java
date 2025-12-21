package net.shelmarow.combat_evolution.bossbar;

import net.shelmarow.combat_evolution.ai.StaminaStatus;

public class BossData {
    public String displayType = "";
    public float stamina = 1F;
    public float staminaO = 1F;
    public long staminaSetTime = 0;
    public StaminaStatus staminaStatus = StaminaStatus.EMPTY;
}