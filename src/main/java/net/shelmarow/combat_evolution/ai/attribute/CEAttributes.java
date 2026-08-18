package net.shelmarow.combat_evolution.ai.attribute;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;

import java.util.List;

@EventBusSubscriber(modid = CombatEvolution.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CEAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, CombatEvolution.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> EXECUTION_DAMAGE_MULTIPLY = registerAttributes("execution_damage_multiply", 1, 0, Double.MAX_VALUE);
    public static final DeferredHolder<Attribute, Attribute> EXECUTION_REGEN_AMOUNT = registerAttributes("execution_regen_amount", 10, 0, Double.MAX_VALUE);
    public static final DeferredHolder<Attribute, Attribute> EXECUTION_REGEN_PERCENT = registerAttributes("execution_regen_percent", 0.5, 0, Double.MAX_VALUE);

    public static DeferredHolder<Attribute, Attribute> registerAttributes(String name, double value, double min, double max) {
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
        event.add(type, EXECUTION_DAMAGE_MULTIPLY);
    }

    private static void player(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, EXECUTION_REGEN_AMOUNT);
        event.add(EntityType.PLAYER, EXECUTION_REGEN_PERCENT);
    }

}
