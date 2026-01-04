package net.shelmarow.combat_evolution.ai.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.client.particle.follow.CEFollowParticleOptions;

public class CEParticleUtils {

    public static void spawnWarningParticle(ParticleType<CEFollowParticleOptions> particle, Entity entity, Vec3 offset) {
        if (entity != null) {
            Level level = entity.level();
            Vec3 targetPos = entity.position();
            ParticleOptions options = new CEFollowParticleOptions(particle, entity.getId(), offset);
            if(level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(options, targetPos.x, targetPos.y, targetPos.z, 1, 0 ,0 ,0 ,0);
            }
            else {
                level.addParticle(options, targetPos.x, targetPos.y, targetPos.z, 0, 0, 0);
            }
        }
    }
}
