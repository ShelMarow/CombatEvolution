package net.shelmarow.combat_evolution.client.shader;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class CEShaderEffect {
    private final ResourceLocation id;
    private final int priority;

    protected CEShaderEffect(ResourceLocation id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public final ResourceLocation id() {
        return id;
    }

    public final int priority() {
        return priority;
    }

    public boolean isActive() {
        return true;
    }

    public abstract void render(CEShaderContext context);

    public void beginParticleCapture() {
    }

    public void endParticleCapture() {
    }

    public void close() {
    }
}
