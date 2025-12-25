package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CRequestMusicPacket {
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

    public static void handle(S2CRequestMusicPacket msg, Supplier<NetworkEvent.Context> ctx){
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                CEMusicNetworkHandler.requestMusicPlay(msg.packet.isSoftChange(), CEMusicPacket.toCEMusic(msg.packet));
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
