package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ExecutionShaderManager {
    private static final ExecutionShaderEffect EFFECT = new ExecutionShaderEffect();

    private ExecutionShaderManager() {
    }

    static ExecutionShaderEffect effect() {
        return EFFECT;
    }

    public static void trigger(float power, int time) {
        EFFECT.trigger(power, time);
    }

    public static boolean isActive() {
        return EFFECT.isActive();
    }

    public static boolean isInitialized() {
        return EFFECT.isShaderLoaded();
    }

    public static void init() {
        EFFECT.initialize();
    }

    public static void render(int tick, float partialTick, RenderTarget source, RenderTarget target) {
        EFFECT.render(new CEShaderContext(tick, partialTick, source, target));
    }
}
