package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.CombatEvolution;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public class ExecutionShaderManager {

    private static PostChain chain;

    private static boolean active;
    private static float progress;
    private static float strength;
    private static int startTime = 0;
    private static int totalTime = 0;
    private static float duration = 0;

    public static void init(){
        try {
            chain = new PostChain(
                    Minecraft.getInstance().getTextureManager(),
                    Minecraft.getInstance().getResourceManager(),
                    Minecraft.getInstance().getMainRenderTarget(),
                    ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "shaders/post/impact_blur.json")
            );
            resize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void resize() {
        if (chain == null) return;
        Window w = Minecraft.getInstance().getWindow();
        chain.resize(w.getWidth(), w.getHeight());
    }


    public static void trigger(float power, int time) {
        active = true;
        startTime = -1;
        totalTime = time;
        duration = 0;
        progress = 0.0f;
        strength = 0.1f * power;
        resize();
    }

    public static void tick(int tick, float partialTick) {
        if (chain == null) return;

        EffectInstance fx = chain.passes.get(0).getEffect();

        if (active) {
            if(startTime == -1){
                startTime = tick;
            }

            if (progress >= 1.0f) {
                active = false;
            }

            if(!Minecraft.getInstance().isPaused()){
                duration = tick + partialTick - startTime;
                progress = duration / totalTime;
            }

            fx.safeGetUniform("center").set(0.5f, 0.5f);
            fx.safeGetUniform("strength").set(strength);
            fx.safeGetUniform("intensity").set(progress);
            fx.safeGetUniform("samples").set(10);

            chain.process(partialTick);

            resize();
        }
    }

    public static boolean isInitialized() {
        return chain != null;
    }
}
