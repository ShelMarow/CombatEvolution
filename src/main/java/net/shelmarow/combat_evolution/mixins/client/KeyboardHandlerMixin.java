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

import java.util.HashSet;
import java.util.Set;

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
        if (Minecraft.getInstance().screen == null && localPlayer != null && pKey != EpicFightKeyMappings.LOCK_ON.getKey().getValue() && pKey != GLFW.GLFW_KEY_ESCAPE) {
            if(localPlayer.hasEffect(CEMobEffects.ON_EXECUTION.get())){
                ci.cancel();

                Set<Integer> bannedKey = new HashSet<>(Set.of(
                        EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey().getValue(),
                        EpicFightKeyMappings.ATTACK.getKey().getValue(),
                        EpicFightKeyMappings.DODGE.getKey().getValue(),
                        EpicFightKeyMappings.GUARD.getKey().getValue()
                ));

                if(!bannedKey.contains(pKey)) {
                    boolean held = pAction != 0;
                    KeyMapping.set(inputconstants$key, held);
                    if(held){
                        KeyMapping.click(inputconstants$key);
                    }
                }
            }
        }
    }
}
