package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.shelmarow.combat_evolution.bgm.CEMusic;

import java.util.UUID;

public record CEMusicPacket(boolean isSoftChange, UUID requestUUID, ResourceLocation musicPath, SoundSource source, float volume, int duration, boolean loop, boolean canAddToList, int fadeIn, int fadeOut) {

    public static void encode(CEMusicPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.isSoftChange());
        buf.writeUUID(packet.requestUUID());
        buf.writeResourceLocation(packet.musicPath());
        buf.writeEnum(packet.source());
        buf.writeFloat(packet.volume());
        buf.writeInt(packet.duration());
        buf.writeBoolean(packet.loop());
        buf.writeBoolean(packet.canAddToList());
        buf.writeInt(packet.fadeIn());
        buf.writeInt(packet.fadeOut());
    }

    public static CEMusicPacket decode(FriendlyByteBuf buf) {
        boolean isSoftChange = buf.readBoolean();
        UUID requestUUID = buf.readUUID();
        ResourceLocation musicPath = buf.readResourceLocation();
        SoundSource source = buf.readEnum(SoundSource.class);
        float volume = buf.readFloat();
        int duration = buf.readInt();
        boolean loop = buf.readBoolean();
        boolean canAddToList = buf.readBoolean();
        int fadeIn = buf.readInt();
        int fadeOut = buf.readInt();
        return new CEMusicPacket(isSoftChange, requestUUID, musicPath, source, volume, duration, loop, canAddToList, fadeIn, fadeOut);
    }

    public static CEMusicPacket fromCEMusic(boolean isSoftChange, CEMusic music) {
        return new CEMusicPacket(
                isSoftChange,music.getRequestUUID(),music.getMusicPath(),
                music.getSource(),music.getVolume(),music.getDuration(),
                music.isLoop(),music.isCanAddToList(),music.getFadeIn(),music.getFadeOut()
        );
    }

    public static CEMusic toCEMusic(CEMusicPacket packet) {
        return new CEMusic(
                packet.musicPath, packet.source, packet.requestUUID,
                packet.volume, packet.duration, packet.loop,
                packet.canAddToList, packet.fadeIn, packet.fadeOut
        );
    }

}
