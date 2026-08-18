package net.shelmarow.combat_evolution.client.particle.follow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CEFollowParticleOptions implements ParticleOptions {

    private final ParticleType<?> type;
    private final int entityID;
    private final Vec3 offset;

    public CEFollowParticleOptions(ParticleType<?> type, int entityID, Vec3 offset) {
        this.type = type;
        this.entityID = entityID;
        this.offset = offset;
    }

    public int getEntityID() { return entityID; }

    public Vec3 getStartPos() {
        return offset;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return type;
    }

    public static MapCodec<CEFollowParticleOptions> codec(ParticleType<CEFollowParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("entity_id").forGetter(CEFollowParticleOptions::getEntityID),
                Codec.DOUBLE.fieldOf("offset_x").forGetter(o -> o.getStartPos().x),
                Codec.DOUBLE.fieldOf("offset_y").forGetter(o -> o.getStartPos().y),
                Codec.DOUBLE.fieldOf("offset_z").forGetter(o -> o.getStartPos().z)
        ).apply(instance, (entityId, ox, oy, oz) ->
                new CEFollowParticleOptions(type, entityId,new Vec3(ox,oy,oz))
        ));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, CEFollowParticleOptions> streamCodec(ParticleType<CEFollowParticleOptions> type) {
        return StreamCodec.of((buf, options) -> {
            buf.writeInt(options.entityID);
            buf.writeDouble(options.offset.x);
            buf.writeDouble(options.offset.y);
            buf.writeDouble(options.offset.z);
        }, buf -> new CEFollowParticleOptions(type, buf.readInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())));
    }
}
