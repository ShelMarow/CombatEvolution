package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.resources.ResourceLocation;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BossData {
    public String displayType = "";
    public float stamina = 1F;
    public float staminaO = 1F;
    public long staminaSetTime = 0;
    public @NonNull StaminaStatus staminaStatus = StaminaStatus.EMPTY;
    public @NonNull ResourceLocation bossBarTexture = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/bossbar/ce_boss_bar.png");
}