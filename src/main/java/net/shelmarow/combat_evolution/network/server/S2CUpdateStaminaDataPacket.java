package net.shelmarow.combat_evolution.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
public class S2CUpdateStaminaDataPacket implements CustomPacketPayload {
    public static final Type<S2CUpdateStaminaDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "update_stamina_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateStaminaDataPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2CUpdateStaminaDataPacket::decode);
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

    public static void handle(S2CUpdateStaminaDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBossData.updateStaminaData(msg.uuid, msg.stamina, msg.staminaStatus);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
