package net.shelmarow.combat_evolution.mixins;

import net.shelmarow.combat_evolution.ai.iml.IDamageSourceData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

import java.util.List;

@Mixin(EpicFightDamageSource.class)
public abstract class EpicFightDamageSourceMixin implements IDamageSourceData {

    @Shadow(remap = false)
    public abstract AnimationManager.AnimationAccessor<? extends StaticAnimation> getAnimation();

    @Unique
    private AttackAnimation.Phase combatEvolution$phase;

    @Override
    public void setSourcePhase(AttackAnimation.Phase phase) {
        this.combatEvolution$phase = phase;
    }

    @Override
    public int getSourcePhaseIndex() {
        if(combatEvolution$phase != null && getAnimation().get() instanceof AttackAnimation attackAnimation){
            return List.of(attackAnimation.phases).indexOf(combatEvolution$phase);
        }
        return -1;
    }
}
