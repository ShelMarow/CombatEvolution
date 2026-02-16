package net.shelmarow.combat_evolution.client.particle.follow;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
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

    public static final Deserializer<CEFollowParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull CEFollowParticleOptions fromCommand(@NotNull ParticleType<CEFollowParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            int entityId = reader.readInt();
            reader.expect(' ');
            double offsetX = reader.readDouble();
            reader.expect(' ');
            double offsetY = reader.readDouble();
            reader.expect(' ');
            double offsetZ = reader.readDouble();
            return new CEFollowParticleOptions(type,entityId,new Vec3(offsetX,offsetY,offsetZ));
        }

        @Override
        public @NotNull CEFollowParticleOptions fromNetwork(@NotNull ParticleType<CEFollowParticleOptions> type, FriendlyByteBuf buf) {
            return new CEFollowParticleOptions(type,buf.readInt(),new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
    };

    public int getEntityID() { return entityID; }

    public Vec3 getStartPos() {
        return offset;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeDouble(offset.x);
        buf.writeDouble(offset.y);
        buf.writeDouble(offset.z);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format("%d %.2f %.2f %.2f %d", entityID, offset.x, offset.y, offset.z);
    }

    public static Codec<CEFollowParticleOptions> codec(ParticleType<CEFollowParticleOptions> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("entity_id").forGetter(CEFollowParticleOptions::getEntityID),
                Codec.DOUBLE.fieldOf("offset_x").forGetter(o -> o.getStartPos().x),
                Codec.DOUBLE.fieldOf("offset_y").forGetter(o -> o.getStartPos().y),
                Codec.DOUBLE.fieldOf("offset_z").forGetter(o -> o.getStartPos().z)
        ).apply(instance, (entityId, ox, oy, oz) ->
                new CEFollowParticleOptions(type, entityId,new Vec3(ox,oy,oz))
        ));
    }
}
