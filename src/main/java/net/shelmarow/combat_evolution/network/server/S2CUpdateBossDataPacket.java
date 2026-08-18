package net.shelmarow.combat_evolution.network.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.BossData;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
public class S2CUpdateBossDataPacket implements CustomPacketPayload {
    public static final Type<S2CUpdateBossDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "update_boss_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateBossDataPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CUpdateBossDataPacket::decode);
    private final UUID uuid;
    private final BossData bossData;

    public S2CUpdateBossDataPacket(UUID uuid, BossData bossData) {
        this.uuid = uuid;
        this.bossData = bossData;
    }

    public static void encode(S2CUpdateBossDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeUUID(msg.uuid);
        buffer.writeNbt(msg.bossData.toTag());

    }

    public static S2CUpdateBossDataPacket decode(FriendlyByteBuf buffer){
        UUID uuid = buffer.readUUID();
        CompoundTag tag = buffer.readNbt();
        BossData bossData = new BossData();
        bossData.fromTag(tag);
        return new S2CUpdateBossDataPacket(uuid, bossData);
    }

    public static void handle(S2CUpdateBossDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBossData.updateData(msg.uuid,msg.bossData);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
