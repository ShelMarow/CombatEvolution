package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;

public class S2CRequestMusicPacket implements CustomPacketPayload {
    public static final Type<S2CRequestMusicPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "request_music"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRequestMusicPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CRequestMusicPacket::decode);
    private final CEMusicPacket packet;

    public S2CRequestMusicPacket(CEMusicPacket packet) {
        this.packet = packet;
    }

    public static void encode(S2CRequestMusicPacket msg, FriendlyByteBuf buf) {
        CEMusicPacket.encode(msg.packet, buf);
    }

    public static S2CRequestMusicPacket decode(FriendlyByteBuf buf) {
        return new S2CRequestMusicPacket(CEMusicPacket.decode(buf));
    }

    public static void handle(S2CRequestMusicPacket msg, IPayloadContext ctx){
        ctx.enqueueWork(() -> {
            CEMusicNetworkHandler.requestMusicPlay(msg.packet.isSoftChange(), msg.packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
