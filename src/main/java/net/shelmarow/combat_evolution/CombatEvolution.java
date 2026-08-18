package net.shelmarow.combat_evolution;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.shelmarow.combat_evolution.ai.CEConditions;
import net.shelmarow.combat_evolution.ai.CEExpandedEntityDataAccessors;
import net.shelmarow.combat_evolution.ai.attribute.CEAttributes;
import net.shelmarow.combat_evolution.api.event.RegisterCustomExecutionEvent;
import net.shelmarow.combat_evolution.api.event.RegisterHUDTypeEvent;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.command.CEEntityCommand;
import net.shelmarow.combat_evolution.command.CEExecutionCommand;
import net.shelmarow.combat_evolution.command.CEParticleCommand;
import net.shelmarow.combat_evolution.config.CEClientConfig;
import net.shelmarow.combat_evolution.config.CECommonConfig;
import net.shelmarow.combat_evolution.config.screen.CombatEvolutionConfigScreen;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.event.ForgeEvent;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.item.CECreativeTab;
import net.shelmarow.combat_evolution.item.CEItems;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import net.shelmarow.combat_evolution.sounds.CESounds;
import net.shelmarow.combat_evolution.skill.CESkills;
import org.slf4j.Logger;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.api.event.EpicFightEventHooks;

@Mod(CombatEvolution.MOD_ID)
public class CombatEvolution {
    public static final String MOD_ID = "combat_evolution";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CombatEvolution(IEventBus modEventBus, ModContainer modContainer, Dist dist){
        NeoForge.EVENT_BUS.register(this);
        EpicFightEventHooks.Entity.ON_STUNNED.registerEvent(ForgeEvent::onStunApply, MOD_ID);
        net.shelmarow.combat_evolution.example.event.ModEvent.registerEpicFightEvents();
        if (dist == Dist.CLIENT) {
            net.shelmarow.combat_evolution.example.event.ModEventClient.registerEpicFightEvents();
        }
        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CENetworkHandler::registerPackets);

        CEAttributes.ATTRIBUTES.register(modEventBus);
        CEExpandedEntityDataAccessors.REGISTRY.register(modEventBus);
        CEMobEffects.EFFECTS.register(modEventBus);
        CEParticles.PARTICLE_TYPES.register(modEventBus);
        CEEntities.ENTITY_TYPES.register(modEventBus);
        CESounds.SOUNDS.register(modEventBus);
        CEConditions.CONDITIONS.register(modEventBus);
        CESkills.SKILLS.register(modEventBus);
        CEItems.ITEMS.register(modEventBus);
        CECreativeTab.CREATIVE_TAB.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, CECommonConfig.COMMON_SPEC);

        if(dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, CEClientConfig.CLIENT_SPEC);
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, CombatEvolutionConfigScreen::createConfigScreen);
        }

    }

    private void constructMod(final FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            ModLoader.postEvent(new RegisterHUDTypeEvent());
        });
    }


    private void commonSetup(final FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            CombatEvolution.registerArmatures();
            ModLoader.postEvent(new RegisterCustomExecutionEvent());
        });
    }

    public static void registerArmatures() {
        Armatures.registerEntityTypeArmature(CEEntities.SHELMAROW.get(),Armatures.BIPED);
    }



    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CEParticleCommand.register(event.getDispatcher());
        CEExecutionCommand.register(event.getDispatcher());
        CEEntityCommand.register(event.getDispatcher());
    }

}
