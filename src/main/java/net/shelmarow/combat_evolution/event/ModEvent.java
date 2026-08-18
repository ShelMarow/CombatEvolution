package net.shelmarow.combat_evolution.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.api.event.RegisterHUDTypeEvent;
import net.shelmarow.combat_evolution.client.hud.execution.ExecutionHUD;
import net.shelmarow.combat_evolution.client.hud.execution.types.DefaultType;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.client.particle.warning.BypassDodgeParticle;
import net.shelmarow.combat_evolution.client.particle.warning.BypassGuardParticle;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEvent {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRegisterHUDType(RegisterHUDTypeEvent event) {
        event.registerHUDType(CombatEvolution.MOD_ID, new DefaultType());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "execution_hud"), ExecutionHUD.instance);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CEParticles.BYPASS_DODGE_WARNING.get(), BypassDodgeParticle.Provider::new);
        event.registerSpriteSet(CEParticles.BYPASS_GUARD_WARNING.get(), BypassGuardParticle.Provider::new);
    }
}
