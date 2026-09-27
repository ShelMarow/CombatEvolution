package net.shelmarow.combat_evolution.mixins.client;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import com.mojang.blaze3d.vertex.PoseStack;
import net.shelmarow.combat_evolution.client.shader.CEShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void combatEvolution$finishParticlePhase(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            Frustum frustum,
            CallbackInfo ci
    ) {
        CEShaderManager.finishParticlePhase();
    }
}
