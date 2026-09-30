package net.shelmarow.combat_evolution.network.server;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.client.hud.execution.ExecutionHUD;

import java.util.function.Supplier;

public class S2CAssassinationEligibilityPacket {
    private final int targetEntityId;
    private final boolean eligible;

    public S2CAssassinationEligibilityPacket(int targetEntityId, boolean eligible) {
        this.targetEntityId = targetEntityId;
        this.eligible = eligible;
    }

    public static void encode(S2CAssassinationEligibilityPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.targetEntityId);
        buffer.writeBoolean(msg.eligible);
    }

    public static S2CAssassinationEligibilityPacket decode(FriendlyByteBuf buffer) {
        return new S2CAssassinationEligibilityPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(S2CAssassinationEligibilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> handleOnClient(msg));
        }
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleOnClient(S2CAssassinationEligibilityPacket msg) {
        if (Minecraft.getInstance().player != null) {
            ExecutionHUD.setServerAssassinationEligibility(msg.targetEntityId, msg.eligible);
        }
    }
}
