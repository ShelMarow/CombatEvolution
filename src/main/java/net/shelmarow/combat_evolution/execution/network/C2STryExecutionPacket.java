package net.shelmarow.combat_evolution.execution.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;

public class C2STryExecutionPacket implements CustomPacketPayload {
    public static final Type<C2STryExecutionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "try_execution"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2STryExecutionPacket> STREAM_CODEC = StreamCodec.of((buffer, msg) -> {}, buffer -> new C2STryExecutionPacket());

    public C2STryExecutionPacket() {

    }

    public static void encode(C2STryExecutionPacket msg, FriendlyByteBuf buffer){

    }

    public static C2STryExecutionPacket decode(FriendlyByteBuf buffer){
        return new C2STryExecutionPacket();
    }

    public static void handle(C2STryExecutionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer serverPlayer) {
                if(ExecutionHandler.tryExecute(serverPlayer)){
                    //CombatEvolution.LOGGER.info("Execution Successfully!");
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
