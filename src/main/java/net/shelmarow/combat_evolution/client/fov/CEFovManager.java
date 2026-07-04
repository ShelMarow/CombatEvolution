package net.shelmarow.combat_evolution.client.fov;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.tickTask.TickTask;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CEFovManager {

    private static float fovModifier = 0.0F;

    private static TickTask task = null;

    @SubscribeEvent
    public static void onClientEvent(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            if(task != null) {
                if(!task.isFinished()){
                    task.tick();
                }
                else if(fovModifier != 0){
                    resetFovModifier(Minecraft.getInstance().player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onFOVUpdate(ComputeFovModifierEvent event) {
        double originalFov = event.getNewFovModifier();
        double fovOffset = fovModifier;
        event.setNewFovModifier((float) (originalFov + fovOffset));
    }

    public static void setFovModifierTask(LocalPlayer player, float fovModifier, int maxDuration) {
        if(player == Minecraft.getInstance().player) {
            if(task != null && !task.isFinished()) {
                task.onFinish();
            }
            task = new TickTask(maxDuration) {
                @Override
                public void onStart() {
                    addFovModifier(fovModifier);
                }

                @Override
                public void onTick() {}

                @Override
                public void onFinish() {
                    addFovModifier(-fovModifier);
                }
            };
            task.onStart();
        }
    }


    public static void addFovModifier(float modifier) {
        fovModifier += modifier;
    }

    public static void resetFovModifier(LocalPlayer player) {
        if(player == Minecraft.getInstance().player) {
            task = null;
            fovModifier = 0.0F;
        }
    }


}
