package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CRemoveMusicPacket {
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

    public static void handle(S2CRemoveMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CEMusicNetworkHandler.removeMusic(msg.requestUUID, msg.forceRemove);
        });
        ctx.get().setPacketHandled(true);
    }
}
