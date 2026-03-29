package net.shelmarow.combat_evolution.bossbar.network.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CUpdateBossCustomDataPacket {
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

    public static void handle(S2CUpdateBossCustomDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                ClientBossData.updateCustomDate(msg.uuid,msg.tag);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
