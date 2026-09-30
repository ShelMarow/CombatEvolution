package net.shelmarow.combat_evolution.client.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.CombatEvolution;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.BufferUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public final class CEShaderManager {
    private static final int GL_BLEND_COLOR = 0x8005;
    private static final int RESTORED_TEXTURE_SLOTS = 12;
    @FunctionalInterface
    public interface Effect {
        void render(int tick, float partialTick, RenderTarget source, RenderTarget target);
    }

    private static final Map<ResourceLocation, CEShaderEffect> EFFECTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, TextureTarget> EFFECT_TARGETS = new LinkedHashMap<>();
    private static TextureTarget pipelineSource;
    private static int pipelineWidth;
    private static int pipelineHeight;
    private static boolean frameActive;
    private static boolean particlePhase;

    static {
        register(ExecutionShaderEffect.INSTANCE);
    }

    private CEShaderManager() {
    }

    public static void register(ResourceLocation id, int priority, Effect effect) {
        register(id, priority, () -> true, effect);
    }

    public static void register(ResourceLocation id, int priority,
                                BooleanSupplier active, Effect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(active, "active");
        Objects.requireNonNull(effect, "effect");
        register(new CEShaderEffect(id, priority) {
            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }

            @Override
            public void render(CEShaderContext context) {
                effect.render(
                        context.tick(), context.partialTick(),
                        context.source(), context.target()
                );
            }
        });
    }

    public static void register(CEShaderEffect effect) {
        Objects.requireNonNull(effect, "effect");
        CEShaderEffect previous = EFFECTS.put(effect.id(), effect);
        if (previous != null && previous != effect) {
            previous.close();
        }
    }

    public static boolean unregister(ResourceLocation id) {
        CEShaderEffect removedEffect = EFFECTS.remove(id);
        if (removedEffect != null) {
            removedEffect.close();
        }
        TextureTarget target = EFFECT_TARGETS.remove(id);
        if (target != null) {
            target.destroyBuffers();
        }
        return removedEffect != null;
    }

    public static void beginFrame() {
        frameActive = true;
        particlePhase = true;
    }

    public static void finishParticlePhase() {
        particlePhase = false;
    }

    public static boolean isParticlePhase() {
        return frameActive && particlePhase;
    }

    public static RenderTarget getFrameSource() {
        Minecraft minecraft = Minecraft.getInstance();
        return getInputTarget(minecraft.getMainRenderTarget());
    }

    public static void render(int tick, float partialTick) {
        List<CEShaderEffect> effects = new ArrayList<>(EFFECTS.values());
        effects.removeIf(effect -> !isActive(effect));
        if (effects.isEmpty()) {
            frameActive = false;
            particlePhase = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        RenderTarget inputTarget = getInputTarget(mainTarget);
        effects.sort(Comparator
                .comparingInt(CEShaderEffect::priority)
                .thenComparing(effect -> effect.id().toString()));

        int width = mainTarget.width;
        int height = mainTarget.height;
        RenderState frameState = RenderState.capture();
        if (!ensureTargets(width, height, effects)) {
            frameState.restore();
            return;
        }

        try {
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            GL11C.glColorMask(true, true, true, true);
            copyFramebuffer(
                    inputTarget.frameBufferId,
                    GL30C.GL_COLOR_ATTACHMENT0,
                    pipelineSource.frameBufferId,
                    GL30C.GL_COLOR_ATTACHMENT0,
                    inputTarget.width,
                    inputTarget.height,
                    width,
                    height
            );

            RenderTarget source = pipelineSource;
            for (CEShaderEffect registered : effects) {
                TextureTarget target = EFFECT_TARGETS.get(registered.id());
                copyFramebuffer(
                        source.frameBufferId,
                        GL30C.GL_COLOR_ATTACHMENT0,
                        target.frameBufferId,
                        GL30C.GL_COLOR_ATTACHMENT0,
                        width,
                        height,
                        width,
                        height
                );

                RenderState state = RenderState.capture();
                try {
                    registered.render(new CEShaderContext(tick, partialTick, source, target));
                } catch (RuntimeException exception) {
                    CombatEvolution.LOGGER.error("Shader effect {} failed", registered.id(), exception);
                } finally {
                    state.restore();
                }
                source = target;
            }

            copyFramebuffer(
                    source.frameBufferId,
                    GL30C.GL_COLOR_ATTACHMENT0,
                    mainTarget.frameBufferId,
                    GL30C.GL_COLOR_ATTACHMENT0,
                    width,
                    height,
                    mainTarget.width,
                    mainTarget.height
            );
        } finally {
            frameState.restore();
            mainTarget.bindWrite(false);
            frameActive = false;
            particlePhase = false;
        }
    }

    private static boolean isActive(CEShaderEffect effect) {
        try {
            return effect.isActive();
        } catch (RuntimeException exception) {
            CombatEvolution.LOGGER.error("Shader effect {} activity check failed", effect.id(), exception);
            return false;
        }
    }

    private static RenderTarget getInputTarget(RenderTarget mainTarget) {
        if (Minecraft.getInstance().levelRenderer instanceof net.shelmarow.combat_evolution.mixins.client.LevelRendererAccessor accessor
                && accessor.combatEvolution$getTransparencyChain() != null) {
            return Minecraft.getInstance().levelRenderer.getParticlesTarget();
        }
        return mainTarget;
    }

    private static boolean ensureTargets(int width, int height, List<CEShaderEffect> effects) {
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (pipelineSource == null || pipelineWidth != width || pipelineHeight != height) {
            destroyTargets();
            pipelineWidth = width;
            pipelineHeight = height;
            pipelineSource = createTarget(width, height);
        }

        for (CEShaderEffect effect : effects) {
            TextureTarget target = EFFECT_TARGETS.get(effect.id());
            if (target == null || target.width != width || target.height != height) {
                if (target != null) {
                    target.destroyBuffers();
                }
                EFFECT_TARGETS.put(effect.id(), createTarget(width, height));
            }
        }
        EFFECT_TARGETS.entrySet().removeIf(entry -> {
            boolean keep = effects.stream().anyMatch(effect -> effect.id().equals(entry.getKey()));
            if (!keep) {
                entry.getValue().destroyBuffers();
            }
            return !keep;
        });
        return true;
    }

    private static TextureTarget createTarget(int width, int height) {
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setFilterMode(GL11C.GL_LINEAR);
        return target;
    }

    private static void destroyTargets() {
        if (pipelineSource != null) {
            pipelineSource.destroyBuffers();
            pipelineSource = null;
        }
        for (TextureTarget target : EFFECT_TARGETS.values()) {
            target.destroyBuffers();
        }
        EFFECT_TARGETS.clear();
    }

    static void copyFramebuffer(int sourceFramebuffer, int sourceReadBuffer,
                                int destinationFramebuffer, int destinationDrawBuffer,
                                int sourceWidth, int sourceHeight,
                                int destinationWidth, int destinationHeight) {
        int previousReadFramebuffer = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int previousDrawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousReadBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER);
        int previousDrawBuffer = GL11C.glGetInteger(GL11C.GL_DRAW_BUFFER);
        boolean previousScissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        int[] previousScissorBox = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, previousScissorBox);
        boolean[] previousColorMask = new boolean[4];
        ByteBuffer colorMaskBuffer = BufferUtils.createByteBuffer(4);
        GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMaskBuffer);
        for (int index = 0; index < previousColorMask.length; index++) {
            previousColorMask[index] = colorMaskBuffer.get(index) != 0;
        }

        try {
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            GL11C.glColorMask(true, true, true, true);
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, sourceFramebuffer);
            GL11C.glReadBuffer(sourceReadBuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, destinationFramebuffer);
            GL11C.glDrawBuffer(destinationDrawBuffer);
            GlStateManager._glBlitFrameBuffer(
                    0, 0, sourceWidth, sourceHeight,
                    0, 0, destinationWidth, destinationHeight,
                    GL11C.GL_COLOR_BUFFER_BIT,
                    GL11C.GL_NEAREST
            );
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
            GL11C.glReadBuffer(previousReadBuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, previousDrawFramebuffer);
            GL11C.glDrawBuffer(previousDrawBuffer);
            GL11C.glColorMask(previousColorMask[0], previousColorMask[1], previousColorMask[2], previousColorMask[3]);
            if (previousScissor) {
                GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
            } else {
                GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            }
            GL11C.glScissor(previousScissorBox[0], previousScissorBox[1], previousScissorBox[2], previousScissorBox[3]);
        }
    }

    public static void copy(RenderTarget source, RenderTarget destination) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        copyFramebuffer(
                source.frameBufferId,
                GL30C.GL_COLOR_ATTACHMENT0,
                destination.frameBufferId,
                GL30C.GL_COLOR_ATTACHMENT0,
                source.width,
                source.height,
                destination.width,
                destination.height
        );
    }

    private static final class RenderState {
        private final int readFramebuffer;
        private final int drawFramebuffer;
        private final int readBuffer;
        private final int drawBuffer;
        private final int[] viewport = new int[4];
        private final int activeTexture;
        private final int[] shaderTextures = new int[RESTORED_TEXTURE_SLOTS];
        private final int currentProgram;
        private final ShaderInstance currentShader;
        private final int vertexArray;
        private final int arrayBuffer;
        private final int elementArrayBuffer;
        private final boolean depthTest;
        private final int depthFunc;
        private final boolean blend;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int blendEquationRgb;
        private final int blendEquationAlpha;
        private final float[] blendColor = new float[4];
        private final boolean cull;
        private final boolean stencil;
        private final boolean depthMask;
        private final boolean[] colorMask = new boolean[4];
        private final boolean scissor;
        private final int[] scissorBox = new int[4];
        private final boolean polygonOffsetFill;
        private final float polygonOffsetFactor;
        private final float polygonOffsetUnits;
        private final int stencilFunc;
        private final int stencilRef;
        private final int stencilValueMask;
        private final int stencilWriteMask;
        private final int stencilFail;
        private final int stencilDepthFail;
        private final int stencilDepthPass;
        private final float[] shaderColor;
        private final float lineWidth;
        private final boolean logicOp;
        private final int logicOpMode;
        private final float[] depthRange = new float[2];

        private RenderState() {
            readFramebuffer = GL30C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
            drawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
            readBuffer = GL11C.glGetInteger(GL11C.GL_READ_BUFFER);
            drawBuffer = GL11C.glGetInteger(GL11C.GL_DRAW_BUFFER);
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);

            activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
            for (int unit = 0; unit < shaderTextures.length; unit++) {
                shaderTextures[unit] = getTextureBinding(GL13C.GL_TEXTURE0 + unit);
            }
            GL13C.glActiveTexture(activeTexture);

            currentProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
            currentShader = RenderSystem.getShader();
            vertexArray = GL30C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
            arrayBuffer = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
            elementArrayBuffer = GL11C.glGetInteger(GL15C.GL_ELEMENT_ARRAY_BUFFER_BINDING);
            depthTest = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            depthFunc = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
            blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
            blendSrcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
            blendDstRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
            blendSrcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
            blendDstAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
            blendEquationRgb = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_RGB);
            blendEquationAlpha = GL11C.glGetInteger(GL20C.GL_BLEND_EQUATION_ALPHA);
            FloatBuffer blendColorBuffer = BufferUtils.createFloatBuffer(4);
            GL11C.glGetFloatv(GL_BLEND_COLOR, blendColorBuffer);
            for (int index = 0; index < blendColor.length; index++) {
                blendColor[index] = blendColorBuffer.get(index);
            }
            cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
            stencil = GL11C.glIsEnabled(GL11C.GL_STENCIL_TEST);
            depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            ByteBuffer colorMaskBuffer = BufferUtils.createByteBuffer(4);
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, colorMaskBuffer);
            for (int index = 0; index < colorMask.length; index++) {
                colorMask[index] = colorMaskBuffer.get(index) != 0;
            }
            scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
            GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, scissorBox);
            polygonOffsetFill = GL11C.glIsEnabled(GL11C.GL_POLYGON_OFFSET_FILL);
            polygonOffsetFactor = GL11C.glGetFloat(GL11C.GL_POLYGON_OFFSET_FACTOR);
            polygonOffsetUnits = GL11C.glGetFloat(GL11C.GL_POLYGON_OFFSET_UNITS);
            stencilFunc = GL11C.glGetInteger(GL11C.GL_STENCIL_FUNC);
            stencilRef = GL11C.glGetInteger(GL11C.GL_STENCIL_REF);
            stencilValueMask = GL11C.glGetInteger(GL11C.GL_STENCIL_VALUE_MASK);
            stencilWriteMask = GL11C.glGetInteger(GL11C.GL_STENCIL_WRITEMASK);
            stencilFail = GL11C.glGetInteger(GL11C.GL_STENCIL_FAIL);
            stencilDepthFail = GL11C.glGetInteger(GL11C.GL_STENCIL_PASS_DEPTH_FAIL);
            stencilDepthPass = GL11C.glGetInteger(GL11C.GL_STENCIL_PASS_DEPTH_PASS);
            shaderColor = RenderSystem.getShaderColor().clone();
            lineWidth = RenderSystem.getShaderLineWidth();
            logicOp = GL11C.glIsEnabled(GL11C.GL_COLOR_LOGIC_OP);
            logicOpMode = GL11C.glGetInteger(GL11C.GL_LOGIC_OP_MODE);
            FloatBuffer depthRangeBuffer = BufferUtils.createFloatBuffer(2);
            GL11C.glGetFloatv(GL11C.GL_DEPTH_RANGE, depthRangeBuffer);
            for (int index = 0; index < depthRange.length; index++) {
                depthRange[index] = depthRangeBuffer.get(index);
            }
        }

        private static RenderState capture() {
            return new RenderState();
        }

        private void restore() {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL11C.glReadBuffer(readBuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL11C.glDrawBuffer(drawBuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);

            for (int unit = 0; unit < shaderTextures.length; unit++) {
                RenderSystem.setShaderTexture(unit, shaderTextures[unit]);
            }
            RenderSystem.activeTexture(activeTexture);

            if (currentShader != null) {
                RenderSystem.setShader(() -> currentShader);
            } else {
                GL20C.glUseProgram(currentProgram);
            }
            setEnabled(GL11C.GL_DEPTH_TEST, depthTest);
            GL11C.glDepthFunc(depthFunc);
            setEnabled(GL11C.GL_BLEND, blend);
            GL14C.glBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            GL20C.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GL14C.glBlendColor(blendColor[0], blendColor[1], blendColor[2], blendColor[3]);
            setEnabled(GL11C.GL_CULL_FACE, cull);
            setEnabled(GL11C.GL_STENCIL_TEST, stencil);
            RenderSystem.depthMask(depthMask);
            GL11C.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            setEnabled(GL11C.GL_SCISSOR_TEST, scissor);
            GL11C.glScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            setEnabled(GL11C.GL_POLYGON_OFFSET_FILL, polygonOffsetFill);
            GL11C.glPolygonOffset(polygonOffsetFactor, polygonOffsetUnits);
            GL11C.glStencilFunc(stencilFunc, stencilRef, stencilValueMask);
            GL11C.glStencilMask(stencilWriteMask);
            GL11C.glStencilOp(stencilFail, stencilDepthFail, stencilDepthPass);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            RenderSystem.lineWidth(lineWidth);
            if (logicOp) {
                GL11C.glEnable(GL11C.GL_COLOR_LOGIC_OP);
            } else {
                GL11C.glDisable(GL11C.GL_COLOR_LOGIC_OP);
            }
            GL11C.glLogicOp(logicOpMode);
            GL11C.glDepthRange(depthRange[0], depthRange[1]);
            GL30C.glBindVertexArray(vertexArray);
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, arrayBuffer);
            if (vertexArray != 0) {
                GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
            }
            BufferUploader.invalidate();
        }

        private static int getTextureBinding(int textureUnit) {
            GL13C.glActiveTexture(textureUnit);
            return GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) {
                GL11C.glEnable(capability);
            } else {
                GL11C.glDisable(capability);
            }
        }
    }
}
