package net.shelmarow.combat_evolution.client.particle.warning;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.client.particle.follow.CEFollowParticleOptions;
import net.shelmarow.combat_evolution.client.particle.follow.CEPosFollowBaseParticle;
import net.shelmarow.combat_evolution.client.rendertype.CERenderTypes;
import org.jetbrains.annotations.NotNull;

public class BypassGuardParticle extends CEPosFollowBaseParticle {
    protected BypassGuardParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, SpriteSet pSpriteSet)  {
        super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pSpriteSet);
        this.lifetime = 20;
        this.quadSize = 1.0F;
    }

    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet spriteSet) implements ParticleProvider<CEFollowParticleOptions> {
        @Override
        public Particle createParticle(@NotNull CEFollowParticleOptions typeIn, @NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            BypassGuardParticle particle = new BypassGuardParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, spriteSet);
            particle.entityID = typeIn.getEntityID();
            particle.offset = typeIn.getStartPos();
            return particle;
        }
    }
}
