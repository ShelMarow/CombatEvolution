package net.shelmarow.combat_evolution.mixins;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

@Mixin(ServerEntity.class)
public class ServerEntityMixin {

    @Shadow @Final private Entity entity;

    @Inject(
            method = "addPairing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;startSeenByPlayer(Lnet/minecraft/server/level/ServerPlayer;)V")
    )
    private void addPairing(ServerPlayer serverPlayer, CallbackInfo ci){
        EpicFightCapabilities.getUnparameterizedEntityPatch(this.entity, CEHumanoidPatch.class).ifPresent(entityPatch -> {
            entityPatch.startSeenByPlayer(serverPlayer);
        });
    }

    @Inject(
            method = "removePairing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;stopSeenByPlayer(Lnet/minecraft/server/level/ServerPlayer;)V")
    )
    private void removePairing(ServerPlayer serverPlayer, CallbackInfo ci){
        EpicFightCapabilities.getUnparameterizedEntityPatch(this.entity, CEHumanoidPatch.class).ifPresent(entityPatch -> {
            entityPatch.stopSeenByPlayer(serverPlayer);
        });
    }
}
