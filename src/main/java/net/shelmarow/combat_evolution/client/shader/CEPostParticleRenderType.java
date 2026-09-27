package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public abstract class CEPostParticleRenderType implements ParticleRenderType {
    private final CEShaderEffect effect;
    private boolean captureStarted;

    protected CEPostParticleRenderType(CEShaderEffect effect) {
        this.effect = effect;
    }

    protected final CEShaderEffect effect() {
        return effect;
    }

    @Override
    public final void begin(@NotNull BufferBuilder builder, @NotNull TextureManager textureManager) {
        effect.beginParticleCapture();
        captureStarted = true;
        beginParticle(builder, textureManager);
    }

    @Override
    public final void end(@NotNull Tesselator tesselator) {
        try {
            tesselator.end();
        } finally {
            try {
                if (captureStarted) {
                    effect.endParticleCapture();
                }
            } finally {
                captureStarted = false;
                endParticle();
            }
        }
    }

    protected abstract void beginParticle(BufferBuilder builder, TextureManager textureManager);

    protected void endParticle() {
    }
}
