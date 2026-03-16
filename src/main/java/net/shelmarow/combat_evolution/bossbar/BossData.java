package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BossData {
    public String displayType = "";
    public float stamina = 1F;
    public float staminaO = 1F;
    public long staminaSetTime = 0;
    public @NonNull StaminaStatus staminaStatus = StaminaStatus.EMPTY;
    public @NonNull ResourceLocation bossBarTexture = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/bossbar/ce_boss_bar.png");

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("displayType", displayType);
        tag.putFloat("stamina", stamina);
        tag.putFloat("staminaO", staminaO);
        tag.putLong("staminaSetTime", staminaSetTime);
        tag.putString("staminaStatus", staminaStatus.name());
        tag.putString("bossBarTexture", bossBarTexture.toString());
        return tag;
    }

    public void fromTag(@Nullable CompoundTag tag) {
        if(tag == null) return;
        displayType = tag.getString("displayType");
        stamina = tag.getFloat("stamina");
        staminaO = tag.getFloat("staminaO");
        staminaSetTime = tag.getLong("staminaSetTime");
        staminaStatus = StaminaStatus.valueOf(tag.getString("staminaStatus"));
        ResourceLocation texture = ResourceLocation.tryParse(tag.getString("bossBarTexture"));
        texture = texture == null ? ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/bossbar/ce_boss_bar.png") : texture;
        bossBarTexture = texture;
    }
}