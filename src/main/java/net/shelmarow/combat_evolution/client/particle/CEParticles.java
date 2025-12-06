package net.shelmarow.combat_evolution.client.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.client.particle.follow.CEFollowParticleOptions;
import org.jetbrains.annotations.NotNull;

public class CEParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CombatEvolution.MOD_ID);


    public static final RegistryObject<ParticleType<CEFollowParticleOptions>> BYPASS_DODGE_WARNING =
            PARTICLE_TYPES.register("bypass_dodge_warning", () ->
                    new ParticleType<>(false, CEFollowParticleOptions.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.@NotNull Codec<CEFollowParticleOptions> codec() {
                            return CEFollowParticleOptions.codec(this);
                        }
                    });

    public static final RegistryObject<ParticleType<CEFollowParticleOptions>> BYPASS_GUARD_WARNING =
            PARTICLE_TYPES.register("bypass_guard_warning", () ->
                    new ParticleType<>(false, CEFollowParticleOptions.DESERIALIZER) {
                        @Override
                        public com.mojang.serialization.@NotNull Codec<CEFollowParticleOptions> codec() {
                            return CEFollowParticleOptions.codec(this);
                        }
                    });
}
