package net.shelmarow.combat_evolution.client.bossbar;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientBossData {

    private static final Map<UUID, BossData> BOSS_DATA_MAP = new ConcurrentHashMap<>();

    public static BossData getBossData(UUID bossId) {
        return BOSS_DATA_MAP.computeIfAbsent(bossId, k -> new BossData());
    }

    // 当boss条被移除时清理数据
    public static void removeBoss(UUID bossId) {
        BOSS_DATA_MAP.remove(bossId);
    }

    //更新全部数据
    public static void updateData(UUID bossId, BossData bossData) {
        BOSS_DATA_MAP.put(bossId, bossData);
    }

    //更新部分数据
    public static void partialUpdate(UUID bossId, BossDataUpdater updater) {
        BossData data = getBossData(bossId);
        updater.update(data);
    }

    //类型
    public static Component getDisplayType(UUID bossId) {
        return getBossData(bossId).displayType;
    }

    //耐力值
    public static float getStamina(UUID bossId) {
        return getBossData(bossId).stamina;
    }

    public static float getStaminaProgress(BossData data) {
        long elapsed = Util.getMillis() - data.staminaSetTime;
        float t = Mth.clamp((float) elapsed / 100,0F, 1.0F);
        return Mth.lerp(t, data.staminaO, data.stamina);
    }

    @FunctionalInterface
    public interface BossDataUpdater {
        void update(BossData data);
    }

    public static class BossData {
        public Component displayType = Component.empty();
        public float stamina = 1F;
        public float staminaO = 1F;
        public long staminaSetTime = 0;
    }
}