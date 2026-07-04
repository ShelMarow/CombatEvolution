package net.shelmarow.combat_evolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.execution.ExecutionTypeManager;

public class CEExecutionCommand {

    private static final SuggestionProvider<CommandSourceStack> EXECUTION_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
                ExecutionTypeManager.getExecutionTypeKeys().stream().map(ResourceLocation::toString).map(s -> "\"" + s + "\"").toList(),
                builder
        );
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat_evolution")
                .then(Commands.literal("execution")
                        .requires(source -> source.hasPermission(2))
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
                                                .then(Commands.literal("type")
                                                        // 添加自动补全的type参数
                                                        .then(Commands.argument("execution_type", StringArgumentType.string())
                                                                .suggests(EXECUTION_TYPE_SUGGESTIONS)
                                                                .executes(ctx->{
                                                                    String typeKey = StringArgumentType.getString(ctx, "execution_type");
                                                                    ResourceLocation typeId = ResourceLocation.tryParse(typeKey);
                                                                    ExecutionTypeManager.Type type = ExecutionTypeManager.getExecutionType(typeId);

                                                                    Entity executor = EntityArgument.getEntity(ctx, "executor");
                                                                    Entity target = EntityArgument.getEntity(ctx, "target");
                                                                    boolean requireGuardBreak = BoolArgumentType.getBool(ctx, "require_guard_break");

                                                                    if(executor instanceof LivingEntity executorLiving && target instanceof LivingEntity targetLiving) {
                                                                        if (type != null) {
                                                                            boolean success = ExecutionHandler.entityForceExecute(executorLiving, targetLiving, requireGuardBreak, type);
                                                                            if(!success) {
                                                                                ctx.getSource().sendFailure(Component.translatable("command.combat_evolution.execution_failed"));
                                                                            }
                                                                        } else {
                                                                            ctx.getSource().sendFailure(Component.translatable("command.combat_evolution.invalid_execution_type"));
                                                                            return 0;
                                                                        }
                                                                    }
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
