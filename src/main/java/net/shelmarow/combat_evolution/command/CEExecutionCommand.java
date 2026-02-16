package net.shelmarow.combat_evolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.sun.jdi.connect.Connector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;

public class CEExecutionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat_evolution")
                .then(Commands.literal("execution")
                        .then(Commands.argument("executor", EntityArgument.entity())
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("require_guard_break", BoolArgumentType.bool())
                                                .executes(ctx->{
                                                    Entity executor = EntityArgument.getEntity(ctx, "executor");
                                                    Entity target = EntityArgument.getEntity(ctx, "target");
                                                    boolean requireGuardBreak = BoolArgumentType.getBool(ctx, "require_guard_break");
                                                    if(executor instanceof LivingEntity executorLiving && target instanceof LivingEntity targetLiving) {
                                                        boolean success = ExecutionHandler.entityForceExecute(executorLiving, targetLiving, requireGuardBreak);
                                                        if(!success) {
                                                            ctx.getSource().sendFailure(Component.translatable("command.combat_evolution.execution_failed"));
                                                        }
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }
}
