package net.shelmarow.combat_evolution.mixins.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.shelmarow.combat_evolution.client.shader.CEShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "renderLevel",
            at = @At(value = "CONSTANT", args = "stringValue=particles")
    )
    private void combatEvolution$beginShaderFrame(CallbackInfo ci) {
        CEShaderManager.beginFrame();
    }
}
