package net.shelmarow.combat_evolution;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.shelmarow.combat_evolution.ai.CEConditions;
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
import net.shelmarow.combat_evolution.enchantment.CEEnchantments;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.item.CECreativeTab;
import net.shelmarow.combat_evolution.item.CEItems;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import net.shelmarow.combat_evolution.sounds.CESounds;
import org.slf4j.Logger;
import yesman.epicfight.gameasset.Armatures;

@Mod(CombatEvolution.MOD_ID)
public class CombatEvolution {
    public static final String MOD_ID = "combat_evolution";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CombatEvolution(FMLJavaModLoadingContext context){
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);

        CEAttributes.ATTRIBUTES.register(modEventBus);
        CEMobEffects.EFFECTS.register(modEventBus);
        CEParticles.PARTICLE_TYPES.register(modEventBus);
        CEEntities.ENTITY_TYPES.register(modEventBus);
        CESounds.SOUNDS.register(modEventBus);
        CEConditions.CONDITIONS.register(modEventBus);
        CEEnchantments.ENCHANTMENTS.register(modEventBus);
        CEItems.ITEMS.register(modEventBus);
        CECreativeTab.CREATIVE_TAB.register(modEventBus);

        context.registerConfig(ModConfig.Type.COMMON, CECommonConfig.COMMON_SPEC);

        if(FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, CEClientConfig.CLIENT_SPEC);
        }

        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(CombatEvolutionConfigScreen::new));

        CENetworkHandler.registerPackets();
    }

    private void constructMod(final FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            ModLoader.get().postEvent(new RegisterHUDTypeEvent());
        });
    }


    private void commonSetup(final FMLCommonSetupEvent event){
        event.enqueueWork(()->{
            CombatEvolution.registerArmatures();
            ModLoader.get().postEvent(new RegisterCustomExecutionEvent());
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
