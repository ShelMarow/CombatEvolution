package net.shelmarow.combat_evolution.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

@Mixin(GuardSkill.class)
public interface GuardSkillInvoker {
    @Invoker(value = "getGuardMotion",remap = false)
    AnimationManager.AnimationAccessor<? extends StaticAnimation> invokeGetGuardMotion(
            SkillContainer container,
            PlayerPatch<?> playerpatch,
            CapabilityItem itemCapability,
            GuardSkill.BlockType blockType
    );
}
