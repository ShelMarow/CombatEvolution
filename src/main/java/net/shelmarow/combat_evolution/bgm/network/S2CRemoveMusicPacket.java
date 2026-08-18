package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;

import java.util.UUID;
public class S2CRemoveMusicPacket implements CustomPacketPayload {
    public static final Type<S2CRemoveMusicPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "remove_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRemoveMusicPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CRemoveMusicPacket::decode);
    private final UUID requestUUID;
    private final boolean forceRemove;

    public S2CRemoveMusicPacket(UUID requestUUID, boolean forceRemove) {
        this.requestUUID = requestUUID;
        this.forceRemove = forceRemove;
    }

    public static void encode(S2CRemoveMusicPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.requestUUID);
        buf.writeBoolean(msg.forceRemove);
    }

    public static S2CRemoveMusicPacket decode(FriendlyByteBuf buf) {
        return new S2CRemoveMusicPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(S2CRemoveMusicPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            CEMusicNetworkHandler.removeMusic(msg.requestUUID, msg.forceRemove);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
