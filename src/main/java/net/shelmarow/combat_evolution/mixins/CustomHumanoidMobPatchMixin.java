package net.shelmarow.combat_evolution.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.world.capabilities.entitypatch.CustomHumanoidMobPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Map;

@Mixin(value = CustomHumanoidMobPatch.class)
public class CustomHumanoidMobPatchMixin {

    @Shadow(remap = false)
    @Final
    private MobPatchReloadListener.CustomHumanoidMobPatchProvider provider;

    @Inject(
            method = "getHitAnimation(Lyesman/epicfight/world/damagesource/StunType;)Lyesman/epicfight/api/animation/AnimationManager$AnimationAccessor;",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true,
            remap = false
    )
    private void onGetHitAnimation(StunType stunType, CallbackInfoReturnable<AnimationManager.AnimationAccessor<? extends StaticAnimation>> cir){
        cir.cancel();
        Map<StunType, AnimationManager.AnimationAccessor<? extends StaticAnimation>> map = this.provider.getStunAnimations();
        if(map != null){
            cir.setReturnValue(map.get(stunType));
        }
        cir.setReturnValue(null);
    }
}
