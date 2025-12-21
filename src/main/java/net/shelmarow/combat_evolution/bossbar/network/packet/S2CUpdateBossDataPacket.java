package net.shelmarow.combat_evolution.bossbar.network.packet;

import com.google.gson.Gson;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.bossbar.BossData;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CUpdateBossDataPacket {
    private static final Gson GSON = new Gson();
    private final UUID uuid;
    private final BossData bossData;

    public S2CUpdateBossDataPacket(UUID uuid, BossData bossData) {
        this.uuid = uuid;
        this.bossData = bossData;
    }

    public static void encode(S2CUpdateBossDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeUUID(msg.uuid);
        buffer.writeUtf(GSON.toJson(msg.bossData, BossData.class));
    }

    public static S2CUpdateBossDataPacket decode(FriendlyByteBuf buffer){
        UUID uuid = buffer.readUUID();
        String json = buffer.readUtf();
        BossData bossData = GSON.fromJson(json, BossData.class);
        return new S2CUpdateBossDataPacket(uuid, bossData);
    }

    public static void handle(S2CUpdateBossDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                ClientBossData.updateData(msg.uuid,msg.bossData);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
