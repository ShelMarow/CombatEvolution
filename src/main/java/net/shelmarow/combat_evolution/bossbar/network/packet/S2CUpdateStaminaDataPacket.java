package net.shelmarow.combat_evolution.bossbar.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CUpdateStaminaDataPacket {
    private final UUID uuid;
    private final float stamina;
    private final StaminaStatus staminaStatus;

    public S2CUpdateStaminaDataPacket(UUID uuid, float stamina, StaminaStatus staminaStatus) {
        this.uuid = uuid;
        this.stamina = stamina;
        this.staminaStatus = staminaStatus;
    }

    public static void encode(S2CUpdateStaminaDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeUUID(msg.uuid);
        buffer.writeFloat(msg.stamina);
        buffer.writeEnum(msg.staminaStatus);
    }

    public static S2CUpdateStaminaDataPacket decode(FriendlyByteBuf buffer){
        UUID uuid = buffer.readUUID();
        float stamina = buffer.readFloat();
        StaminaStatus staminaStatus = buffer.readEnum(StaminaStatus.class);
        return new S2CUpdateStaminaDataPacket(uuid, stamina, staminaStatus);
    }

    public static void handle(S2CUpdateStaminaDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                ClientBossData.updateStaminaData(msg.uuid, msg.stamina, msg.staminaStatus);
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
