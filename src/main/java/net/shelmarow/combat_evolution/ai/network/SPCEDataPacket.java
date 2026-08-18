package net.shelmarow.combat_evolution.ai.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEPatchReloadListener;

public class SPCEDataPacket implements CustomPacketPayload {
    public static final Type<SPCEDataPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "ce_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SPCEDataPacket> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), SPCEDataPacket::decode);
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

    public static void handle(SPCEDataPacket msg, IPayloadContext ctx){
        ctx.enqueueWork(() -> {
            CEPatchReloadListener.processServerPacket(msg);
        });
    }

    public CompoundTag[] getTags() {
        return tags;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
