package net.shelmarow.combat_evolution.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.network.CENetworkHandler;
import net.shelmarow.combat_evolution.network.server.S2CReleaseGuardPacket;

import java.util.function.Supplier;

public class C2STryExecutionPacket {

    public C2STryExecutionPacket() {

    }

    public static void encode(C2STryExecutionPacket msg, FriendlyByteBuf buffer){

    }

    public static C2STryExecutionPacket decode(FriendlyByteBuf buffer){
        return new C2STryExecutionPacket();
    }

    public static void handle(C2STryExecutionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer serverPlayer = ctx.get().getSender();
                if (serverPlayer != null) {
                    if(ExecutionHandler.tryExecute(serverPlayer)){
                        CENetworkHandler.sendToPlayer(serverPlayer, new S2CReleaseGuardPacket());
                    }
                }
            });
        }
        ctx.get().setPacketHandled(true);
    }
}
