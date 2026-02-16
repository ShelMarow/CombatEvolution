package net.shelmarow.combat_evolution.skill;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.util.BehaviorUtils;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.gameassets.CEEntityState;
import net.shelmarow.combat_evolution.gameassets.ShieldCounterAnimations;
import net.shelmarow.combat_evolution.sounds.CESounds;
import yesman.epicfight.api.forgeevent.EntityStunEvent;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.effect.EpicFightMobEffects;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

import java.util.Random;
import java.util.UUID;

public class CEShieldCounter extends Skill {
    private static final UUID EVENT_UUID = UUID.fromString("3ae19e9c-df47-4bc6-a3b4-781cf4a4a992");

    public CEShieldCounter(SkillBuilder<? extends Skill> builder) {
        super(builder.setCategory(SkillCategories.IDENTITY).setActivateType(ActivateType.TOGGLE).setResource(Resource.NONE));
    }


    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, EVENT_UUID, event -> {
            if (container.getExecutor().isLogicalClient()) {
                SkillCategory skillCategory = event.getSkillContainer().getSkill().getCategory();
                if(skillCategory == SkillCategories.WEAPON_INNATE && EpicFightKeyMappings.GUARD.isDown()) {
                    EpicFightCapabilities.getUnparameterizedEntityPatch(container.getExecutor().getOriginal(), LivingEntityPatch.class).ifPresent(entityPatch -> {
                        if (container.sendCastRequest(container.getClientExecutor(), ClientEngine.getInstance().controlEngine).isExecutable()) {
                            event.setCanceled(true);
                        }
                    });
                }
            }
        });

        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, event -> {
            Entity attacker = event.getDamageSource().getEntity();
            ServerPlayerPatch playerPatch = event.getPlayerPatch();

            boolean isFront = false;
            Vec3 sourceLocation = event.getDamageSource().getSourcePosition();
            if (sourceLocation != null) {
                Vec3 viewVector = event.getPlayerPatch().getOriginal().getViewVector(1.0F);
                Vec3 toSourceLocation = sourceLocation.subtract(event.getPlayerPatch().getOriginal().position()).normalize();
                if (toSourceLocation.dot(viewVector) > (double) 0.0F) {
                    isFront = true;
                }
            }

            if(isFront && canBlockDamage(event.getDamageSource()) && attacker != null && playerPatch.getEntityState().getState(CEEntityState.COUNTER_SUSSED)) {
                //取消伤害
                event.setResult(AttackResult.ResultType.BLOCKED);
                event.setParried(true);
                event.setCanceled(true);

                //音效
                spawnParryEffect(playerPatch.getOriginal());
                playerPatch.playSound(CESounds.COUNTER.get(), 0.5F,0, 0);

                //获得增益
                playerPatch.setStamina(playerPatch.getStamina() + playerPatch.getMaxStamina() * 0.35F);
                playerPatch.getOriginal().addEffect(new MobEffectInstance(CEMobEffects.FULL_STUN_IMMUNITY.get(), 40, 0));
                playerPatch.getOriginal().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 3));
                playerPatch.getOriginal().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));

                //对敌人造成耐力伤害和硬直
                float amount = 14F;
                LivingEntityPatch<?> attackerPatch = EpicFightCapabilities.getEntityPatch(attacker, LivingEntityPatch.class);
                if(attackerPatch instanceof PlayerPatch<?> playerAttacker && !playerAttacker.getOriginal().isCreative() && !playerAttacker.getOriginal().isSpectator()) {
                    float rest = playerAttacker.getStamina() - amount;
                    playerAttacker.setStamina(rest);
                    if(rest <= 0) {
                        EntityStunEvent entityStunEvent = new EntityStunEvent(null, attackerPatch, StunType.NEUTRALIZE);
                        if (!MinecraftForge.EVENT_BUS.post(entityStunEvent)) {
                            attackerPatch.applyStun(StunType.NEUTRALIZE, 0F);
                        }
                    }
                    else{
                        EntityStunEvent entityStunEvent = new EntityStunEvent(null, attackerPatch, StunType.HOLD);
                        if (!MinecraftForge.EVENT_BUS.post(entityStunEvent)) {
                            attackerPatch.playAnimationSynchronized(ShieldCounterAnimations.COUNTERED,0F);
                        }
                    }
                }
                else if (attackerPatch instanceof CEHumanoidPatch ceHumanoidPatch) {
                    if(!ceHumanoidPatch.dealStaminaDamage(null, amount)){
                        EntityStunEvent entityStunEvent = new EntityStunEvent(null, attackerPatch, StunType.HOLD);
                        if (!MinecraftForge.EVENT_BUS.post(entityStunEvent)) {
                            BehaviorUtils.stopCurrentBehavior(ceHumanoidPatch.getOriginal());
                            attackerPatch.playAnimationSynchronized(ShieldCounterAnimations.COUNTERED,0F);
                        }
                    }
                }
                else if (attackerPatch != null && attackerPatch.getArmature() instanceof HumanoidArmature) {
                    EntityStunEvent entityStunEvent = new EntityStunEvent(null, attackerPatch, StunType.NEUTRALIZE);
                    if (!MinecraftForge.EVENT_BUS.post(entityStunEvent)) {
                        attackerPatch.applyStun(StunType.NEUTRALIZE, 0F);
                    }
                }

            }
        });

    }


    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);

        container.getExecutor().getEventListener().removeListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, EVENT_UUID);
        container.getExecutor().getEventListener().removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
    }



    private static boolean canBlockDamage(DamageSource source) {
        return !source.is(EpicFightDamageTypeTags.GUARD_PUNCTURE) && !source.is(EpicFightDamageTypeTags.UNBLOCKALBE);
    }

    public static void spawnParryEffect(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 look = player.getLookAngle().scale(2);
        Vec3 pos = player.getEyePosition().add(look);
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 25, 0, 0, 0, 0.25);
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        CapabilityItem capabilityItem = container.getExecutor().getHoldingItemCapability(InteractionHand.OFF_HAND);
        return super.canExecute(container) && capabilityItem.getWeaponCategory() == CapabilityItem.WeaponCategories.SHIELD && container.getExecutor().isOffhandItemValid();
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        container.getExecutor().getOriginal().addEffect(new MobEffectInstance(EpicFightMobEffects.STUN_IMMUNITY.get(), 10, 0, false, false, true));
        container.getExecutor().playAnimationSynchronized(ShieldCounterAnimations.SHIELD_COUNTER, 0F);
    }
}
