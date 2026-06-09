package net.shelmarow.combat_evolution.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CUpdateBossBarTexture {
    private final UUID uuid;
    private final String texture;

    public S2CUpdateBossBarTexture(UUID uuid, String texture) {
        this.uuid = uuid;
        this.texture = texture;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(texture);
    }

    public static S2CUpdateBossBarTexture decode(FriendlyByteBuf buf) {
        return new S2CUpdateBossBarTexture(buf.readUUID(), buf.readUtf());
    }

    public static void handle(S2CUpdateBossBarTexture packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBossData.updateTexture(packet.uuid, packet.texture);
        });
        ctx.get().setPacketHandled(true);
    }
}
