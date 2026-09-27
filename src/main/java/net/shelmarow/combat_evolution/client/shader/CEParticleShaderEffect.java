package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;


@OnlyIn(Dist.CLIENT)
public abstract class CEParticleShaderEffect extends CEShaderEffect {
    private TextureTarget sceneTarget;
    private TextureTarget currentTarget;
    private TextureTarget maskTarget;
    private BufferBuilder blurBuilder;
    private BufferBuilder effectBuilder;
    private BufferBuilder.RenderedBuffer blurGeometry;
    private BufferBuilder.RenderedBuffer effectGeometry;
    private int maskVertexCount;
    private int effectVertexCount;
    private ShaderInstance particleShader;
    private int captureDrawFramebuffer;
    private int captureReadFramebuffer;
    private int captureDrawBuffer;
    private int captureReadBuffer;
    private int captureSourceFramebuffer;
    private int captureSourceBuffer;
    private int captureSourceWidth;
    private int captureSourceHeight;
    private int previousActiveTexture;
    private int previousTexture0;
    private int previousTexture1;
    private int previousTexture2;
    private int previousTexture3;
    private Matrix4f captureModelView;
    private Matrix4f captureProjection;
    private boolean compositeGeometryStarted;
    private final int[] captureViewport = new int[4];
    private boolean captureStarted;

    protected CEParticleShaderEffect(ResourceLocation id, int priority) {
        super(id, priority);
    }

    public final void setShader(ShaderInstance shader) {
        particleShader = shader;
    }

    protected final ShaderInstance shader() {
        return particleShader;
    }

    public final boolean hasPendingEffect() {
        return blurGeometry != null
                && particleShader != null
                && sceneTarget != null
                && currentTarget != null
                && maskTarget != null;
    }

    @Override
    public final boolean isActive() {
        return hasPendingEffect();
    }

    @Override
    public final void beginParticleCapture() {
        beginCapture();
    }

    @Override
    public final void endParticleCapture() {
        finishCapture();
    }

    @Override
    public final void render(CEShaderContext context) {
        applyPending(context);
    }

    public void beginCapture() {
        Minecraft minecraft = Minecraft.getInstance();
        captureDrawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        captureReadFramebuffer = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        captureDrawBuffer = GL11C.glGetInteger(GL11C.GL_DRAW_BUFFER);
        captureReadBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER);
        RenderTarget sceneSource = CEShaderManager.getFrameSource();
        int width = sceneSource.width;
        int height = sceneSource.height;
        captureSourceFramebuffer = sceneSource.frameBufferId;
        captureSourceBuffer = GL30C.GL_COLOR_ATTACHMENT0;
        captureSourceWidth = sceneSource.width;
        captureSourceHeight = sceneSource.height;
        previousActiveTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        previousTexture0 = getTextureBinding(GL13C.GL_TEXTURE0);
        previousTexture1 = getTextureBinding(GL13C.GL_TEXTURE1);
        previousTexture2 = getTextureBinding(GL13C.GL_TEXTURE2);
        previousTexture3 = getTextureBinding(GL13C.GL_TEXTURE3);
        GL13C.glActiveTexture(previousActiveTexture);
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, captureViewport);
        captureModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        captureProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        captureStarted = false;
        blurBuilder = null;
        effectBuilder = null;
        blurGeometry = null;
        effectGeometry = null;
        compositeGeometryStarted = false;
        maskVertexCount = 0;
        effectVertexCount = 0;

        if (!ensureTargets(width, height)) {
            restoreTextureBindings();
            return;
        }

        restoreCaptureFramebuffers();
        CEShaderManager.copyFramebuffer(
                captureSourceFramebuffer,
                captureSourceBuffer,
                sceneTarget.frameBufferId,
                GL30C.GL_COLOR_ATTACHMENT0,
                captureSourceWidth,
                captureSourceHeight,
                sceneTarget.width,
                sceneTarget.height
        );

        currentTarget.clear(Minecraft.ON_OSX);
        currentTarget.copyDepthFrom(sceneSource);
        maskTarget.clear(Minecraft.ON_OSX);
        currentTarget.bindWrite(false);
        RenderSystem.viewport(0, 0, currentTarget.width, currentTarget.height);

        blurBuilder = new BufferBuilder(256);
        blurBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        effectBuilder = new BufferBuilder(256);
        effectBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        captureStarted = true;
    }

    public void recordMaskVertex(float x, float y, float z) {
        if (blurBuilder == null || captureModelView == null || captureProjection == null) {
            return;
        }

        Vector4f clip = new Vector4f(x, y, z, 1.0F);
        captureModelView.transform(clip);
        captureProjection.transform(clip);
        if (clip.w() == 0.0F) {
            return;
        }

        float inverseW = 1.0F / clip.w();
        blurBuilder.vertex(clip.x() * inverseW, clip.y() * inverseW, clip.z() * inverseW)
                .color(255, 255, 255, 255)
                .endVertex();
        if (!compositeGeometryStarted) {
            effectBuilder.vertex(-1.0F, -1.0F, 0.0F).endVertex();
            effectBuilder.vertex(1.0F, -1.0F, 0.0F).endVertex();
            effectBuilder.vertex(1.0F, 1.0F, 0.0F).endVertex();
            effectBuilder.vertex(-1.0F, 1.0F, 0.0F).endVertex();
            compositeGeometryStarted = true;
        }
        maskVertexCount++;
        effectVertexCount++;
    }

    public void recordMaskVertex(float x, float y, float z, float u, float v) {
        recordMaskVertex(x, y, z);
    }

    public void finishCapture() {
        if (blurBuilder == null || effectBuilder == null) {
            return;
        }

        if (maskVertexCount >= 3) {
            blurGeometry = blurBuilder.end();
        } else {
            blurBuilder.end();
            blurGeometry = null;
        }
        if (effectVertexCount >= 3) {
            effectGeometry = effectBuilder.end();
        } else {
            effectBuilder.end();
            effectGeometry = null;
        }
        blurBuilder = null;
        effectBuilder = null;
        compositeGeometryStarted = false;
        try {
            if (blurGeometry != null) {
                renderMask();
            }
        } finally {
            maskVertexCount = 0;
            effectVertexCount = 0;
            restoreCaptureState();
        }
    }

    private void applyPending(CEShaderContext context) {
        RenderTarget source = context.source();
        RenderTarget target = context.target();
        if (effectGeometry == null || particleShader == null || sceneTarget == null
                || currentTarget == null || maskTarget == null) {
            resetBatchState();
            return;
        }

        int applyDrawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int applyReadFramebuffer = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int applyDrawBuffer = GL11C.glGetInteger(GL11C.GL_DRAW_BUFFER);
        int applyReadBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER);
        int applyActiveTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        int applyTexture0 = getTextureBinding(GL13C.GL_TEXTURE0);
        int applyTexture1 = getTextureBinding(GL13C.GL_TEXTURE1);
        int applyTexture2 = getTextureBinding(GL13C.GL_TEXTURE2);
        int applyTexture3 = getTextureBinding(GL13C.GL_TEXTURE3);
        int[] previousViewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, previousViewport);
        boolean previousBlend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean previousDepthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        boolean previousCull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        boolean previousDepthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);

        try {
            target.bindWrite(false);
            GL11C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
            RenderSystem.viewport(0, 0, target.width, target.height);

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, currentTarget.getColorTextureId());
            RenderSystem.setShaderTexture(1, sceneTarget.getColorTextureId());
            RenderSystem.setShaderTexture(2, maskTarget.getColorTextureId());
            RenderSystem.setShaderTexture(3, source.getColorTextureId());
            particleShader.setSampler("ParticleSampler", currentTarget.getColorTextureId());
            particleShader.setSampler("SceneSampler", sceneTarget.getColorTextureId());
            particleShader.setSampler("MaskSampler", maskTarget.getColorTextureId());
            particleShader.setSampler("SourceSampler", source.getColorTextureId());
            configureShader(context);
            RenderSystem.setShader(() -> particleShader);
            particleShader.apply();
            BufferUploader.drawWithShader(effectGeometry);
        } finally {
            particleShader.clear();
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, applyReadFramebuffer);
            GL11C.glReadBuffer(applyReadBuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, applyDrawFramebuffer);
            GL11C.glDrawBuffer(applyDrawBuffer);
            RenderSystem.viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
            restoreTextureBindings(applyActiveTexture, applyTexture0, applyTexture1, applyTexture2, applyTexture3);
            setEnabled(GL11C.GL_CULL_FACE, previousCull);
            setEnabled(GL11C.GL_DEPTH_TEST, previousDepthTest);
            setEnabled(GL11C.GL_BLEND, previousBlend);
            RenderSystem.depthMask(previousDepthMask);
            resetBatchState();
        }
    }

    @Override
    public void close() {
        if (captureStarted) {
            restoreCaptureState();
        }
        resetBatchState();
        destroyTargets();
        particleShader = null;
    }

    protected void configureShader(CEShaderContext context) {
    }

    protected final void setShaderSampler(String samplerName, int textureId) {
        if (particleShader != null) {
            particleShader.setSampler(samplerName, textureId);
        }
    }

    private void resetBatchState() {
        blurBuilder = null;
        effectBuilder = null;
        blurGeometry = null;
        effectGeometry = null;
        maskVertexCount = 0;
        effectVertexCount = 0;
        compositeGeometryStarted = false;
        captureStarted = false;
    }

    private void restoreCaptureState() {
        if (!captureStarted) {
            return;
        }
        restoreCaptureFramebuffers();
        restoreTextureBindings();
        RenderSystem.viewport(captureViewport[0], captureViewport[1], captureViewport[2], captureViewport[3]);
    }

    private boolean ensureTargets(int width, int height) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (sceneTarget != null && currentTarget != null && maskTarget != null
                && sceneTarget.width == width && sceneTarget.height == height) {
            return true;
        }

        destroyTargets();
        sceneTarget = new TextureTarget(width, height, false, false);
        sceneTarget.setFilterMode(GL11C.GL_LINEAR);
        currentTarget = new TextureTarget(width, height, true, false);
        currentTarget.setFilterMode(GL11C.GL_LINEAR);
        currentTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        maskTarget = new TextureTarget(width, height, false, false);
        maskTarget.setFilterMode(GL11C.GL_NEAREST);
        maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        return true;
    }

    private void destroyTargets() {
        if (sceneTarget != null) {
            sceneTarget.destroyBuffers();
        }
        if (currentTarget != null) {
            currentTarget.destroyBuffers();
        }
        if (maskTarget != null) {
            maskTarget.destroyBuffers();
        }
        sceneTarget = null;
        currentTarget = null;
        maskTarget = null;
    }

    private void renderMask() {
        int previousReadFramebuffer = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER);
        int previousDrawBuffer = GL11C.glGetInteger(GL11C.GL_DRAW_BUFFER);
        int[] previousViewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, previousViewport);
        boolean previousBlend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        boolean previousDepthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        boolean previousCull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        boolean previousDepthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        int previousVertexArray = GL30C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        int previousArrayBuffer = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
        int previousElementArrayBuffer = GL11C.glGetInteger(GL15C.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        ShaderInstance previousShader = RenderSystem.getShader();
        float[] previousShaderColor = RenderSystem.getShaderColor().clone();
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting previousVertexSorting = RenderSystem.getVertexSorting();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();

        try {
            modelViewStack.pushPose();
            modelViewStack.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
            maskTarget.bindWrite(false);
            RenderSystem.viewport(0, 0, maskTarget.width, maskTarget.height);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(blurGeometry);
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL11C.glReadBuffer(previousReadBuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL11C.glDrawBuffer(previousDrawBuffer);
            RenderSystem.viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
            setEnabled(GL11C.GL_CULL_FACE, previousCull);
            setEnabled(GL11C.GL_DEPTH_TEST, previousDepthTest);
            setEnabled(GL11C.GL_BLEND, previousBlend);
            RenderSystem.depthMask(previousDepthMask);
            GL30C.glBindVertexArray(previousVertexArray);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, previousArrayBuffer);
            if (previousVertexArray != 0) {
                GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, previousElementArrayBuffer);
            }
            BufferUploader.invalidate();
            if (previousShader != null) {
                RenderSystem.setShader(() -> previousShader);
            }
            RenderSystem.setShaderColor(
                    previousShaderColor[0], previousShaderColor[1],
                    previousShaderColor[2], previousShaderColor[3]
            );
            RenderSystem.setProjectionMatrix(previousProjection, previousVertexSorting);
            modelViewStack.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private void restoreCaptureFramebuffers() {
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, captureDrawFramebuffer);
        GL11C.glDrawBuffer(captureDrawBuffer);
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, captureReadFramebuffer);
        GL11C.glReadBuffer(captureReadBuffer);
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            switch (capability) {
                case GL11C.GL_BLEND -> RenderSystem.enableBlend();
                case GL11C.GL_DEPTH_TEST -> RenderSystem.enableDepthTest();
                case GL11C.GL_CULL_FACE -> RenderSystem.enableCull();
                default -> {
                }
            }
        } else {
            switch (capability) {
                case GL11C.GL_BLEND -> RenderSystem.disableBlend();
                case GL11C.GL_DEPTH_TEST -> RenderSystem.disableDepthTest();
                case GL11C.GL_CULL_FACE -> RenderSystem.disableCull();
                default -> {
                }
            }
        }
    }

    private void restoreTextureBindings() {
        restoreTextureBindings(
                previousActiveTexture, previousTexture0, previousTexture1,
                previousTexture2, previousTexture3
        );
    }

    private static void restoreTextureBindings(
            int activeTexture, int texture0, int texture1, int texture2, int texture3
    ) {
        RenderSystem.activeTexture(GL13C.GL_TEXTURE0);
        RenderSystem.bindTexture(texture0);
        RenderSystem.activeTexture(GL13C.GL_TEXTURE1);
        RenderSystem.bindTexture(texture1);
        RenderSystem.activeTexture(GL13C.GL_TEXTURE2);
        RenderSystem.bindTexture(texture2);
        RenderSystem.activeTexture(GL13C.GL_TEXTURE3);
        RenderSystem.bindTexture(texture3);
        RenderSystem.activeTexture(activeTexture);
    }

    private static int getTextureBinding(int textureUnit) {
        GL13C.glActiveTexture(textureUnit);
        return GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
    }
}
