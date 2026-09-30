package net.shelmarow.combat_evolution.mixins.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import org.lwjgl.glfw.GLFW;
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
        InputConstants.Key inputconstants$key = InputConstants.getKey(pKey, pScanCode);
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (Minecraft.getInstance().screen == null && localPlayer != null
                && !inputconstants$key.equals(EpicFightKeyMappings.LOCK_ON.getKey())
                && pKey != GLFW.GLFW_KEY_ESCAPE) {
            if(localPlayer.hasEffect(CEMobEffects.ON_EXECUTION.get())){
                ci.cancel();

                if(!isBlockedActionKey(inputconstants$key)) {
                    boolean held = pAction != 0;
                    KeyMapping.set(inputconstants$key, held);
                    if(held){
                        KeyMapping.click(inputconstants$key);
                    }
                }
            }
        }
    }

    private static boolean isBlockedActionKey(InputConstants.Key key) {
        return key.equals(EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey())
                || key.equals(EpicFightKeyMappings.ATTACK.getKey())
                || key.equals(EpicFightKeyMappings.DODGE.getKey())
                || key.equals(EpicFightKeyMappings.GUARD.getKey());
    }
}
