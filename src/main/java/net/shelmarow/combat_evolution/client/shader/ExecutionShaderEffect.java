package net.shelmarow.combat_evolution.client.shader;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.CombatEvolution;

@OnlyIn(Dist.CLIENT)
public final class ExecutionShaderEffect extends CEFullscreenShaderEffect {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "execution");
    public static final ExecutionShaderEffect INSTANCE = new ExecutionShaderEffect();

    private static final ResourceLocation PROGRAM =
            ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "impact_blur");

    private boolean active;
    private float progress;
    private float strength;
    private int startTime;
    private int totalTime;

    private ExecutionShaderEffect() {
        super(ID, 1000);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void trigger(float power, int time) {
        active = true;
        startTime = -1;
        totalTime = time;
        progress = 0.0F;
        strength = 0.1F * power;
    }

    @Override
    protected ResourceLocation shaderProgram() {
        return PROGRAM;
    }

    @Override
    protected void beforeRender(CEShaderContext context) {
        if (startTime == -1) {
            startTime = context.tick();
        }
        if (!net.minecraft.client.Minecraft.getInstance().isPaused()) {
            float duration = context.tick() + context.partialTick() - startTime;
            progress = totalTime <= 0
                    ? 1.0F
                    : Math.min(duration / totalTime, 1.0F);
        }
        if (progress >= 1.0F) {
            active = false;
        }
    }

    @Override
    protected void setupUniforms(CEShaderContext context) {
        super.setupUniforms(context);
        shader().safeGetUniform("center").set(0.5F, 0.5F);
        shader().safeGetUniform("strength").set(strength);
        shader().safeGetUniform("intensity").set(progress);
        shader().safeGetUniform("samples").set(10);
        shader().safeGetUniform("Time").set(context.partialTick());
    }
}
