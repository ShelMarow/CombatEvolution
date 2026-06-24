package net.shelmarow.combat_evolution.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.api.event.RegisterCustomExecutionEvent;
import net.shelmarow.combat_evolution.api.event.RegisterHUDTypeEvent;
import net.shelmarow.combat_evolution.client.hud.execution.ExecutionHUD;
import net.shelmarow.combat_evolution.client.hud.execution.types.DefaultType;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.client.particle.warning.BypassDodgeParticle;
import net.shelmarow.combat_evolution.client.particle.warning.BypassGuardParticle;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvent {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRegisterHUDType(RegisterHUDTypeEvent event) {
        event.registerHUDType(CombatEvolution.MOD_ID, new DefaultType());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("execution_hud", ExecutionHUD.instance);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CEParticles.BYPASS_DODGE_WARNING.get(), BypassDodgeParticle.Provider::new);
        event.registerSpriteSet(CEParticles.BYPASS_GUARD_WARNING.get(), BypassGuardParticle.Provider::new);
    }
}
