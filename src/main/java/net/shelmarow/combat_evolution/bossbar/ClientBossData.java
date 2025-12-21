package net.shelmarow.combat_evolution.bossbar;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.ai.StaminaStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientBossData {

    private static final Map<UUID, BossData> BOSS_DATA_MAP = new ConcurrentHashMap<>();

    public static boolean hasBossData(UUID uuid){
        return BOSS_DATA_MAP.containsKey(uuid);
    }

    public static BossData getBossData(UUID bossId) {
        return BOSS_DATA_MAP.computeIfAbsent(bossId,k -> new BossData());
    }

    // 当boss条被移除时清理数据
    public static void removeBoss(UUID bossId) {
        BOSS_DATA_MAP.remove(bossId);
    }

    //更新全部数据
    public static void updateData(UUID bossId, BossData bossData) {
        BOSS_DATA_MAP.put(bossId, bossData);
    }

    //更新耐力
    public static void updateStaminaData(UUID uuid, float stamina, StaminaStatus staminaStatus) {
        BossData data = getBossData(uuid);
        data.staminaStatus = staminaStatus;
        data.staminaO = getStaminaProgress(uuid);
        data.stamina = stamina;
        data.staminaSetTime = Util.getMillis();
    }

    public static float getStaminaProgress(UUID bossId) {
        BossData data = getBossData(bossId);
        long elapsed = Util.getMillis() - data.staminaSetTime;
        float duration = 100f;
        float t = Mth.clamp((float) elapsed / duration, 0F, 1.0F);
        float displayed = Mth.lerp(t, data.staminaO, data.stamina);
        if (t >= 1.0f) {
            data.staminaO = data.stamina;
        }
        return displayed;
    }
}