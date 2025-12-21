package net.shelmarow.combat_evolution.bossbar.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CRemoveBossDataPacket {
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

    public static void handle(S2CRemoveBossDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                ClientBossData.removeBoss(msg.uuid);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
