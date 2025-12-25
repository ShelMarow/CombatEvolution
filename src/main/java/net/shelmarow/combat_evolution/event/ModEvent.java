package net.shelmarow.combat_evolution.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.api.event.RegisterHUDTypeEvent;
import net.shelmarow.combat_evolution.client.execution.types.DefaultType;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.client.particle.warning.BypassDodgeParticle;
import net.shelmarow.combat_evolution.client.particle.warning.BypassGuardParticle;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEvent {

    @SubscribeEvent
    public static void onRegisterHUDType(RegisterHUDTypeEvent event) {
        event.registerHUDType(CombatEvolution.MOD_ID, new DefaultType());
    }

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CEParticles.BYPASS_DODGE_WARNING.get(), BypassDodgeParticle.Provider::new);
        event.registerSpriteSet(CEParticles.BYPASS_GUARD_WARNING.get(), BypassGuardParticle.Provider::new);
    }
}
