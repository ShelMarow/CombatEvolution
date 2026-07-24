package net.shelmarow.combat_evolution.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(
            method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void setDeltaMovement(Vec3 pDeltaMovement, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if(entity instanceof LivingEntity livingEntity) {
            if(ExecutionHandler.isExecutingTarget(livingEntity, livingEntity)){
                ci.cancel();
            }
        }
    }
}
