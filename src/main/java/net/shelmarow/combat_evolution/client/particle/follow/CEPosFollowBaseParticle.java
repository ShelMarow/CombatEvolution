package net.shelmarow.combat_evolution.client.particle.follow;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.client.rendertype.CERenderTypes;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public abstract class CEPosFollowBaseParticle extends TextureSheetParticle {
    protected final SpriteSet sprites;
    protected Vec3 offset;
    protected int entityID;

    protected CEPosFollowBaseParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, int lifeTime, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.x = x;
        this.y = y;
        this.z = z;
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = lifeTime;
        this.quadSize = 1.0F;
        this.sprites = spriteSet;
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        Entity entity = level.getEntity(entityID);
        if (entity != null) {
            Vec3 pos = entity.position().add(offset);
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public boolean shouldCull() {
        return false;
    }


    @Override
    public void render(@NotNull VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();

        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x);
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y);
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z);

        Entity entity = level.getEntity(entityID);
        if(entity != null){
            Vec3 pos = entity.getPosition(partialTicks).add(offset);
            px = (float) ((float) pos.x - camPos.x);
            py = (float) ((float) pos.y - camPos.y);
            pz = (float) ((float) pos.z - camPos.z);
        }

        Quaternionf quaternionf = new Quaternionf(camera.rotation());
        quaternionf.rotateZ(this.roll);

        Vector3f[] avector3f = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        float f3 = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
            vector3f.rotate(quaternionf);
            vector3f.mul(f3);
            vector3f.add(px, py, pz);
        }

        int light = this.getLightColor(partialTicks);

        buffer.vertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z())
                .uv(this.getU1(), this.getV1())
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();

        buffer.vertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z())
                .uv(this.getU1(), this.getV0())
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();

        buffer.vertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z())
                .uv(this.getU0(), this.getV0())
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();

        buffer.vertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z())
                .uv(this.getU0(), this.getV1())
                .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return CERenderTypes.PARTICLE_SHEET_OPAQUE;
    }
}
