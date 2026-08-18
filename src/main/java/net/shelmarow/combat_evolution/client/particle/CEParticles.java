package net.shelmarow.combat_evolution.client.particle;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.client.particle.follow.CEFollowParticleOptions;
import org.jetbrains.annotations.NotNull;

public class CEParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, CombatEvolution.MOD_ID);


    public static final DeferredHolder<ParticleType<?>, ParticleType<CEFollowParticleOptions>> BYPASS_DODGE_WARNING =
            PARTICLE_TYPES.register("bypass_dodge_warning", () ->
                    new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.@NotNull MapCodec<CEFollowParticleOptions> codec() {
                            return CEFollowParticleOptions.codec(this);
                        }

                        @Override
                        public net.minecraft.network.codec.@NotNull StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, CEFollowParticleOptions> streamCodec() {
                            return CEFollowParticleOptions.streamCodec(this);
                        }
                    });

    public static final DeferredHolder<ParticleType<?>, ParticleType<CEFollowParticleOptions>> BYPASS_GUARD_WARNING =
            PARTICLE_TYPES.register("bypass_guard_warning", () ->
                    new ParticleType<>(false) {
                        @Override
                        public com.mojang.serialization.@NotNull MapCodec<CEFollowParticleOptions> codec() {
                            return CEFollowParticleOptions.codec(this);
                        }

                        @Override
                        public net.minecraft.network.codec.@NotNull StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, CEFollowParticleOptions> streamCodec() {
                            return CEFollowParticleOptions.streamCodec(this);
                        }
                    });
}
