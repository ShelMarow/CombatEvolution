package net.shelmarow.combat_evolution.ai.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.shelmarow.combat_evolution.ai.CEPatchReloadListener;

import java.util.function.Supplier;

public class SPCEDataPacket {
    private final int size;
    private final CompoundTag[] tags;

    public SPCEDataPacket(int size, CompoundTag[] tags) {
        this.size = size;
        this.tags = tags;
    }

    public static void encode(SPCEDataPacket msg, FriendlyByteBuf buffer){
        buffer.writeInt(msg.size);
        for(CompoundTag tag : msg.tags){
            buffer.writeNbt(tag);
        }
    }

    public static SPCEDataPacket decode(FriendlyByteBuf buffer){
        int size = buffer.readInt();
        CompoundTag[] tags = new CompoundTag[size];
        for(int i = 0; i < size; i++){
            tags[i] = buffer.readNbt();
        }
        return new SPCEDataPacket(size, tags);
    }

    public static void handle(SPCEDataPacket msg, Supplier<NetworkEvent.Context> ctx){
        ctx.get().enqueueWork(() -> {
            CEPatchReloadListener.processServerPacket(msg);
        });
        ctx.get().setPacketHandled(true);
    }

    public CompoundTag[] getTags() {
        return tags;
    }
}
