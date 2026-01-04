package net.shelmarow.combat_evolution.example.entity.shelmarow;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.example.entity.shelmarow.ai.ShelMarowCombatBehaviors;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.List;
import java.util.Set;

public class ShelMarowPatch extends CEHumanoidPatch {

    public ShelMarowPatch() {
        super(Factions.NEUTRAL);
    }

    @Override
    protected void setWeaponMotions() {
        this.weaponLivingMotions.put(CapabilityItem.WeaponCategories.LONGSWORD,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, Set.of(
                        Pair.of(LivingMotions.BLOCK, Animations.LONGSWORD_GUARD),
                        Pair.of(LivingMotions.IDLE, Animations.BIPED_HOLD_SPEAR),
                        Pair.of(LivingMotions.WALK, Animations.BIPED_WALK_SPEAR),
                        Pair.of(LivingMotions.RUN, Animations.BIPED_RUN_SPEAR),
                        Pair.of(LivingMotions.CHASE, Animations.BIPED_RUN_SPEAR),
                        Pair.of(LivingMotions.DEATH, Animations.BIPED_COMMON_NEUTRALIZED)
                ))
        );

        this.guardHitMotions.put(CapabilityItem.WeaponCategories.LONGSWORD,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, List.of(
                        Animations.LONGSWORD_GUARD_ACTIVE_HIT1,
                        Animations.LONGSWORD_GUARD_ACTIVE_HIT2,
                        Animations.SWORD_GUARD_ACTIVE_HIT1,
                        Animations.SWORD_GUARD_ACTIVE_HIT2,
                        Animations.SWORD_GUARD_ACTIVE_HIT3
                ))
        );

        this.weaponAttackMotions.put(
                CapabilityItem.WeaponCategories.LONGSWORD,
                ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, ShelMarowCombatBehaviors.COMMON)
        );
    }
}
