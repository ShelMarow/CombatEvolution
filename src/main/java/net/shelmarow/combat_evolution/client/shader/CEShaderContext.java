package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CEShaderContext {
    private final int tick;
    private final float partialTick;
    private final RenderTarget source;
    private final RenderTarget target;

    CEShaderContext(int tick, float partialTick, RenderTarget source, RenderTarget target) {
        this.tick = tick;
        this.partialTick = partialTick;
        this.source = source;
        this.target = target;
    }

    public int tick() {
        return tick;
    }

    public float partialTick() {
        return partialTick;
    }

    public RenderTarget source() {
        return source;
    }

    public RenderTarget target() {
        return target;
    }

    public int width() {
        return target.width;
    }

    public int height() {
        return target.height;
    }
}
