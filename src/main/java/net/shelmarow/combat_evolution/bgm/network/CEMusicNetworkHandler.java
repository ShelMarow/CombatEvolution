package net.shelmarow.combat_evolution.bgm.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.bgm.CEMusic;
import net.shelmarow.combat_evolution.bgm.CEMusicManager;
import net.shelmarow.combat_evolution.network.CENetworkHandler;

import java.util.UUID;

public class CEMusicNetworkHandler {

    /*服务端向客户端发送的请求*/

    //播放请求

    //纯参数
    public static void sendRequestPlayPacket(ServerPlayer serverPlayer, ResourceLocation musicPath, SoundSource source, UUID requestUUID, float volume, int duration, boolean loop, boolean canAddToList, int fadeIn, int fadeOut) {
        sendRequestPlayPacket(serverPlayer, false, musicPath, source, requestUUID, volume, duration, loop, canAddToList, fadeIn, fadeOut);
    }

    public static void sendRequestPlayPacket(ServerPlayer serverPlayer, boolean isSoftChange, ResourceLocation musicPath, SoundSource source, UUID requestUUID, float volume, int duration, boolean loop, boolean canAddToList, int fadeIn, int fadeOut) {
        sendRequestPlayPacket(serverPlayer, new CEMusicPacket(isSoftChange, requestUUID, musicPath, source, volume, duration, loop, canAddToList, fadeIn, fadeOut));
    }

    public static void sendRequestPlayPacket(ServerPlayer serverPlayer, CEMusicPacket packet) {
        CENetworkHandler.sendToPlayer(serverPlayer, new S2CRequestMusicPacket(packet));
    }

    //移除请求
    public static void sendRemoveMusicPacket(ServerPlayer serverPlayer, UUID requestUUID, boolean forceRemove) {
        CENetworkHandler.sendToPlayer(serverPlayer, new S2CRemoveMusicPacket(requestUUID, forceRemove));
    }


    /*客户端实际执行的内容*/
    @OnlyIn(Dist.CLIENT)
    protected static void requestMusicPlay(boolean softChange, CEMusicPacket music) {
        CEMusic ceMusic = CEMusicPacket.toCEMusic(music);
        requestMusicPlay(softChange, ceMusic);
    }

    @OnlyIn(Dist.CLIENT)
    protected static void requestMusicPlay(boolean softChange, CEMusic music) {
        if (softChange) {
            CEMusicManager.softChangePlay(music);
        } else {
            CEMusicManager.requestPlay(music);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected static void removeMusic(UUID requestUUID, boolean forceRemove) {
        CEMusicManager.removeMusic(requestUUID, forceRemove);
    }
}
