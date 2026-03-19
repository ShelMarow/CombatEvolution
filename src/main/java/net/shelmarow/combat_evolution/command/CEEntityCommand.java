package net.shelmarow.combat_evolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class CEEntityCommand {


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat_evolution")
                .then(Commands.literal("entity")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.literal("stamina")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                                        .executes(ctx->{
                                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                                            EpicFightCapabilities.getUnparameterizedEntityPatch(target, LivingEntityPatch.class).ifPresent(entityPatch -> {
                                                                CEPatchUtils.setStamina(entityPatch, CEPatchUtils.getStamina(entityPatch) + FloatArgumentType.getFloat(ctx, "amount"));
                                                            });
                                                            return 1;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                                        .executes(ctx->{
                                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                                            EpicFightCapabilities.getUnparameterizedEntityPatch(target, LivingEntityPatch.class).ifPresent(entityPatch -> {
                                                                CEPatchUtils.setStamina(entityPatch, FloatArgumentType.getFloat(ctx, "amount"));
                                                            });
                                                            return 1;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("damage")
                                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                                        .executes(ctx->{
                                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                                            CEHumanoidPatch<?> ceHumanoidPatch = EpicFightCapabilities.getEntityPatch(target, CEHumanoidPatch.class);
                                                            if(ceHumanoidPatch != null) {
                                                                ceHumanoidPatch.dealStaminaDamage(null, FloatArgumentType.getFloat(ctx, "amount"));
                                                            }
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("phase")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                                        .executes(ctx->{
                                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                                            EpicFightCapabilities.getUnparameterizedEntityPatch(target, LivingEntityPatch.class).ifPresent(livingEntityPatch -> {
                                                                CEPatchUtils.setPhase(livingEntityPatch,IntegerArgumentType.getInteger(ctx, "index"));
                                                            });
                                                            return 1;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                                        .executes(ctx->{
                                                            Entity target = EntityArgument.getEntity(ctx, "target");
                                                            EpicFightCapabilities.getUnparameterizedEntityPatch(target, LivingEntityPatch.class).ifPresent(livingEntityPatch -> {
                                                                CEPatchUtils.setPhase(livingEntityPatch, CEPatchUtils.getPhase(livingEntityPatch) + IntegerArgumentType.getInteger(ctx, "index"));
                                                            });
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
