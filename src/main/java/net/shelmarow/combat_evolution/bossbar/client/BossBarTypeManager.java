package net.shelmarow.combat_evolution.bossbar.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.bossbar.client.types.AbstractBossBarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class BossBarTypeManager {
    private static final BossBarTypeManager INSTANCE = new BossBarTypeManager();

    private final Map<String, AbstractBossBarType> bossBarTypeMap = new HashMap<>();

    private BossBarTypeManager(){}

    public static BossBarTypeManager getInstance() {
        return INSTANCE;
    }


    public void register(String name, AbstractBossBarType bossBarType){
        bossBarTypeMap.put(name, bossBarType);
    }


    public AbstractBossBarType getBossBarType(String name){
        return bossBarTypeMap.get(name);
    }

    public List<AbstractBossBarType> getBossBarTypes(){
        return new ArrayList<>(bossBarTypeMap.values());
    }
}
