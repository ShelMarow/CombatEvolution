package net.shelmarow.combat_evolution.network.server;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import yesman.epicfight.client.input.EpicFightKeyMappings;

import java.util.function.Supplier;

public class S2CReleaseSKillKeyPacket {

    public S2CReleaseSKillKeyPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static S2CReleaseSKillKeyPacket decode(FriendlyByteBuf buf) {
        return new S2CReleaseSKillKeyPacket();
    }

    public static void handle(S2CReleaseSKillKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(S2CReleaseSKillKeyPacket::handleOnClient);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleOnClient(){
//        for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
//            if (keyMapping.isDown()) {
//                keyMapping.setDown(false);
//            }
//        }
        KeyMapping.set(EpicFightKeyMappings.WEAPON_INNATE_SKILL.getKey(), false);
        KeyMapping.set(EpicFightKeyMappings.ATTACK.getKey(), false);
        KeyMapping.set(EpicFightKeyMappings.DODGE.getKey(), false);
        KeyMapping.set(EpicFightKeyMappings.GUARD.getKey(), false);
    }
}
