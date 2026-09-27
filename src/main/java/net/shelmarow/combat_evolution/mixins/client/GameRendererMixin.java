package net.shelmarow.combat_evolution.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.shelmarow.combat_evolution.client.shader.CEShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 900)
public class GameRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void render(float partialTick, long finishTimeNano, boolean renderLevel, CallbackInfo ci) {
        CEShaderManager.render(Minecraft.getInstance().levelRenderer.getTicks(), partialTick);
    }
}
