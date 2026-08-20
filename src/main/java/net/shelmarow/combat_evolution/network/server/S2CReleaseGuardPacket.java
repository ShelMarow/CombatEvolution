package net.shelmarow.combat_evolution.network.server;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.client.input.EpicFightKeyMappings;

import java.util.function.Supplier;

public class S2CReleaseGuardPacket {

    public S2CReleaseGuardPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static S2CReleaseGuardPacket decode(FriendlyByteBuf buf) {
        return new S2CReleaseGuardPacket();
    }

    public static void handle(S2CReleaseGuardPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(S2CReleaseGuardPacket::handleOnClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(){
        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
            if (keyMapping.isDown()) {
                keyMapping.setDown(false);
            }
        }
        KeyMapping.set(EpicFightKeyMappings.GUARD.getKey(), false);
    }
}
