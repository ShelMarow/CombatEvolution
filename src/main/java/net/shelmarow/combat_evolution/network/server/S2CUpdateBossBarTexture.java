package net.shelmarow.combat_evolution.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bossbar.ClientBossData;

import java.util.UUID;
public class S2CUpdateBossBarTexture implements CustomPacketPayload {
    public static final Type<S2CUpdateBossBarTexture> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "update_boss_bar_texture"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CUpdateBossBarTexture> STREAM_CODEC = StreamCodec.ofMember(S2CUpdateBossBarTexture::encode, S2CUpdateBossBarTexture::decode);
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

    public static void handle(S2CUpdateBossBarTexture packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientBossData.updateTexture(packet.uuid, packet.texture);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
