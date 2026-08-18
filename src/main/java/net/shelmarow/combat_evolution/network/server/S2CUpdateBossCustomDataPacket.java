package net.shelmarow.combat_evolution.network.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
public class S2CUpdateBossCustomDataPacket implements CustomPacketPayload {
    public static final Type<S2CUpdateBossCustomDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "update_boss_custom_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateBossCustomDataPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CUpdateBossCustomDataPacket::decode);
    private final UUID uuid;
    private final CompoundTag tag;

    public S2CUpdateBossCustomDataPacket(UUID uuid, CompoundTag tag) {
        this.uuid = uuid;
        this.tag = tag;
    }

    public static void encode(S2CUpdateBossCustomDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeUUID(msg.uuid);
        buffer.writeNbt(msg.tag);

    }

    public static S2CUpdateBossCustomDataPacket decode(FriendlyByteBuf buffer){
        UUID uuid = buffer.readUUID();
        CompoundTag tag = buffer.readNbt();
        return new S2CUpdateBossCustomDataPacket(uuid, tag);
    }

    public static void handle(S2CUpdateBossCustomDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBossData.updateCustomDate(msg.uuid,msg.tag);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
