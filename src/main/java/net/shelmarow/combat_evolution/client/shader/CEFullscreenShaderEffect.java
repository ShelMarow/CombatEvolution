package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public abstract class CEFullscreenShaderEffect extends CEShaderEffect {
    private EffectInstance shader;
    private Matrix4f projection;
    private int projectionWidth;
    private int projectionHeight;
    private boolean loadFailed;

    protected CEFullscreenShaderEffect(ResourceLocation id, int priority) {
        super(id, priority);
    }

    protected abstract ResourceLocation shaderProgram();

    protected boolean replacesOutput() {
        return true;
    }

    protected final EffectInstance shader() {
        return shader;
    }

    public final boolean isShaderLoaded() {
        return shader != null;
    }

    public final void initialize() {
        ensureShader();
    }

    @Override
    public final void render(CEShaderContext context) {
        ensureShader();
        if (shader == null) {
            return;
        }

        beforeRender(context);
        RenderTarget source = context.source();
        RenderTarget target = context.target();
        updateProjection(target.width, target.height);
        setupUniforms(context);

        source.unbindWrite();
        target.bindWrite(false);
        RenderSystem.viewport(0, 0, target.width, target.height);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11C.GL_ALWAYS);
        shader.setSampler("DiffuseSampler", source::getColorTextureId);

        try {
            shader.apply();
            if (replacesOutput()) {
                RenderSystem.disableBlend();
            }
            BufferBuilder builder = new BufferBuilder(256);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.vertex(0.0D, 0.0D, 500.0D).endVertex();
            builder.vertex(target.width, 0.0D, 500.0D).endVertex();
            builder.vertex(target.width, target.height, 500.0D).endVertex();
            builder.vertex(0.0D, target.height, 500.0D).endVertex();
            BufferUploader.draw(builder.end());
        } finally {
            shader.clear();
            target.unbindWrite();
        }
    }

    protected void beforeRender(CEShaderContext context) {
    }

    protected void setupUniforms(CEShaderContext context) {
        shader.safeGetUniform("ProjMat").set(projection);
        shader.safeGetUniform("OutSize").set((float) context.width(), (float) context.height());
    }

    @Override
    public void close() {
        if (shader != null) {
            shader.close();
            shader = null;
        }
        projection = null;
        projectionWidth = 0;
        projectionHeight = 0;
        loadFailed = false;
    }

    private void ensureShader() {
        if (shader != null || loadFailed) {
            return;
        }
        try {
            shader = new EffectInstance(
                    Minecraft.getInstance().getResourceManager(),
                    shaderProgram().toString()
            );
        } catch (IOException exception) {
            loadFailed = true;
            throw new IllegalStateException("Unable to load CE shader effect " + id(), exception);
        }
    }

    private void updateProjection(int width, int height) {
        if (projection != null && projectionWidth == width && projectionHeight == height) {
            return;
        }
        projectionWidth = width;
        projectionHeight = height;
        projection = new Matrix4f().setOrtho(
                0.0F, (float) width,
                0.0F, (float) height,
                0.1F, 1000.0F
        );
    }
}
