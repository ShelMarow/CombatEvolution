package net.shelmarow.combat_evolution;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shelmarow.combat_evolution.api.event.RegisterHUDTypeEvent;
import net.shelmarow.combat_evolution.client.gui.CombatEvolutionConfigScreen;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.config.ClientConfig;
import net.shelmarow.combat_evolution.effect.CEMobEffects;
import net.shelmarow.combat_evolution.example.entity.CEEntities;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;
import org.slf4j.Logger;
import yesman.epicfight.gameasset.Armatures;

@Mod(CombatEvolution.MOD_ID)
public class CombatEvolution {
    public static final String MOD_ID = "combat_evolution";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    public CombatEvolution(FMLJavaModLoadingContext context){
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::constructMod);
        modEventBus.addListener(this::commonSetup);

        CEMobEffects.EFFECTS.register(modEventBus);
        CEParticles.PARTICLE_TYPES.register(modEventBus);
        CEEntities.ENTITY_TYPES.register(modEventBus);

        if(FMLEnvironment.dist == Dist.CLIENT) {
            context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);
        }

        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory(CombatEvolutionConfigScreen::new));

        registerPackets();
    }

    private void constructMod(final FMLConstructModEvent event) {
        event.enqueueWork(() -> {
            RegisterHUDTypeEvent registerHUDTypeEvent = new RegisterHUDTypeEvent();
            ModLoader.get().postEvent(registerHUDTypeEvent);
        });
    }


    private void commonSetup(final FMLCommonSetupEvent event){
        event.enqueueWork(CombatEvolution::registerArmatures);
    }

    public static void registerArmatures() {
        Armatures.registerEntityTypeArmature(CEEntities.SHELMAROW.get(),Armatures.BIPED);
    }

    private void registerPackets() {
        int packetId = 0;
        CHANNEL.registerMessage(packetId++, C2STryExecutionPacket.class, C2STryExecutionPacket::encode,C2STryExecutionPacket::decode, C2STryExecutionPacket::handle);
    }

}
