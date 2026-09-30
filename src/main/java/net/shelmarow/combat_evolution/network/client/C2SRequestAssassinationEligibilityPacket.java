package net.shelmarow.combat_evolution.network.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import net.shelmarow.combat_evolution.network.server.S2CAssassinationEligibilityPacket;

import java.util.function.Supplier;

public class C2SRequestAssassinationEligibilityPacket {
    private final int targetEntityId;

    public C2SRequestAssassinationEligibilityPacket(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public static void encode(C2SRequestAssassinationEligibilityPacket msg, FriendlyByteBuf buffer) {
        buffer.writeVarInt(msg.targetEntityId);
    }

    public static C2SRequestAssassinationEligibilityPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestAssassinationEligibilityPacket(buffer.readVarInt());
    }

    public static void handle(C2SRequestAssassinationEligibilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    Entity entity = player.level().getEntity(msg.targetEntityId);
                    boolean eligible = entity instanceof LivingEntity target &&
                            ExecutionHandler.canPlayerAssassinate(player, target);
                    CENetworkHandler.sendToPlayer(player,
                            new S2CAssassinationEligibilityPacket(msg.targetEntityId, eligible));
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
