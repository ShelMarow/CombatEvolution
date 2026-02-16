package net.shelmarow.combat_evolution.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.ai.util.CEParticleUtils;
import net.shelmarow.combat_evolution.client.particle.CEParticles;
import net.shelmarow.combat_evolution.client.particle.follow.CEFollowParticleOptions;

public class CEParticleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combat_evolution")
                .then(Commands.literal("particle")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("offset", Vec3Argument.vec3(false))
                                        .then(Commands.literal("bypass_dodge")
                                                .executes(ctx-> spawnParticle(ctx, CEParticles.BYPASS_DODGE_WARNING))
                                        )
                                        .then(Commands.literal("bypass_guard")
                                                .executes(ctx-> spawnParticle(ctx, CEParticles.BYPASS_GUARD_WARNING))
                                        )
                                )
                        )
                )
        );
    }

    private static int spawnParticle(CommandContext<CommandSourceStack> ctx, RegistryObject<ParticleType<CEFollowParticleOptions>> bypassDodgeWarning) throws CommandSyntaxException {
        CEParticleUtils.spawnWarningParticle(bypassDodgeWarning.get(), EntityArgument.getEntity(ctx, "target"), Vec3Argument.getVec3(ctx, "offset"));
        return 1;
    }
}
