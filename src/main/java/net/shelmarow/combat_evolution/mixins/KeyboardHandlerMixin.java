package net.shelmarow.combat_evolution.mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.input.EpicFightKeyMappings;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(
            method = "keyPress",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;getKey(II)Lcom/mojang/blaze3d/platform/InputConstants$Key;"),
            cancellable = true
    )
    private void onKeyPressHead(long pWindowPointer, int pKey, int pScanCode, int pAction, int pModifiers, CallbackInfo ci) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (Minecraft.getInstance().screen == null && localPlayer != null && pKey != EpicFightKeyMappings.LOCK_ON.getKey().getValue()) {
            if(localPlayer.hasEffect(CEMobEffects.ON_EXECUTION.get())){
                ci.cancel();
            }
        }
    }
}
