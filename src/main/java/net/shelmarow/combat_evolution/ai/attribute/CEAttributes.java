package net.shelmarow.combat_evolution.ai.attribute;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;

import java.util.List;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CEAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, CombatEvolution.MOD_ID);

    public static final RegistryObject<Attribute> EXECUTION_DAMAGE_MULTIPLY = registerAttributes("execution_damage_multiply", 1, 0, Double.MAX_VALUE);
    public static final RegistryObject<Attribute> EXECUTION_REGEN_AMOUNT = registerAttributes("execution_regen_amount", 10, 0, Double.MAX_VALUE);
    public static final RegistryObject<Attribute> EXECUTION_REGEN_PERCENT = registerAttributes("execution_regen_percent", 0.5, 0, Double.MAX_VALUE);

    public static RegistryObject<Attribute> registerAttributes(String name, double value, double min, double max) {
        return ATTRIBUTES.register(name, () ->
                new RangedAttribute("attribute.name." + CombatEvolution.MOD_ID + "." + name, value, min, max)
                        .setSyncable(true)
        );
    }

    @SubscribeEvent
    public static void entityAttributeModificationsEvent(EntityAttributeModificationEvent event) {
        List<EntityType<? extends LivingEntity>> types = event.getTypes();

        for (EntityType<? extends LivingEntity> type : types) {
            common(event, type);
        }

        player(event);
    }
    private static void common(EntityAttributeModificationEvent event, EntityType<? extends LivingEntity> type) {
        event.add(type, EXECUTION_DAMAGE_MULTIPLY.get());
    }

    private static void player(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, EXECUTION_REGEN_AMOUNT.get());
        event.add(EntityType.PLAYER, EXECUTION_REGEN_PERCENT.get());
    }

}
