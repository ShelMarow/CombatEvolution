package net.shelmarow.combat_evolution.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
public class S2CRemoveBossDataPacket implements CustomPacketPayload {
    public static final Type<S2CRemoveBossDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "remove_boss_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRemoveBossDataPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CRemoveBossDataPacket::decode);
    private final UUID uuid;

    public S2CRemoveBossDataPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public static void encode(S2CRemoveBossDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeUUID(msg.uuid);
    }

    public static S2CRemoveBossDataPacket decode(FriendlyByteBuf buffer){
        UUID uuid = buffer.readUUID();
        return new S2CRemoveBossDataPacket(uuid);
    }

    public static void handle(S2CRemoveBossDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBossData.removeBoss(msg.uuid);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
