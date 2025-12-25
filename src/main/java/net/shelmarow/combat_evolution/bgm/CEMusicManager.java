package net.shelmarow.combat_evolution.bgm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class CEMusicManager {

    //正在播放的音乐
    private static CEMusic currentPlay = null;
    //等待播放的列表
    private static final List<CEMusic> playList = new ArrayList<>();

    //当前音乐状态
    //播放状态
    private static MusicPlayState playState = MusicPlayState.STOPPED;
    //需要过渡的目标音量
    private static float targetVolume = 1F;
    //是否循环
    private static boolean loop = false;
    //播放计时器
    private static int elapsedTick = 0;
    //过渡计时器
    private static int fadeTick = 0;

    /**
     * >=音乐播放逻辑=<：
     * -
     * 1.请求播放音乐
     * （1）如果当前没有播放，则设置播放信息
     * （2）如果当前正在播放，加入播放队列或者放弃
     * -
     * 2.强制播放音乐
     * （1）不过渡直接停止当前的音乐播放，替换成自身并开始播放
     * -
     * 3.软切换音乐
     * （1）比较当前正在播放的音乐请求UUID，如果一致，过渡结束当前播放，然后切换成新的音乐
     * （2）如果不一致，无法切换，直接退出
     * -
     * 4.停止播放音乐
     * （1）自然播放完毕，或者被切换后触发，根据情况进入过渡，随后结束播放
     * -
     * 5.切换下一首
     * （1）当前音乐结束后，如果等待队列存在音乐，取首个进行播放
     * （2）如果当前音乐未结束，根据情况决定是否要停止当前播放
     * -
     * 6.移除音乐
     * （1）如果请求移除的音乐正在播放，停止当前播放
     * （2）如果请求移除的音乐在队列中，移除队列的音乐
     */

    //音乐播放tick逻辑
    public static void playTick(){
        if(currentPlay == null) {
            return;
        }

        //正常计时
        if(playState != MusicPlayState.STOPPED){
            elapsedTick++;
        }

        if(playState == MusicPlayState.FADE_IN){
            if(fadeTick <= 0){
                currentPlay.getSound().adjustVolume(currentPlay.getVolume());
                changePlayState(MusicPlayState.PLAYING);
            }
            else{
                //调整音量
                int fadeInTick = currentPlay.getFadeIn();
                float progress = Mth.clamp((float)(fadeInTick - fadeTick) / (float)fadeInTick, 0F, 1F);
                currentPlay.getSound().adjustVolume(targetVolume * progress);
            }
            fadeTick--;
        }

        if(playState == MusicPlayState.PLAYING){
            if(elapsedTick >= currentPlay.getDuration() - currentPlay.getFadeOut()){
                changePlayState(MusicPlayState.FADE_OUT);
            }
        }

        if(playState == MusicPlayState.FADE_OUT){
            if(fadeTick <= 0){
                currentPlay.getSound().adjustVolume(0F);
                changePlayState(MusicPlayState.STOPPED);
            }
            else {
                //调整音量
                int fadeInTick = currentPlay.getFadeOut();
                float progress = Mth.clamp((float)fadeTick / (float)fadeInTick, 0F, 1F);
                currentPlay.getSound().adjustVolume(targetVolume * progress);
            }
            fadeTick--;
        }

        if(playState == MusicPlayState.STOPPED){
            if(!loop){
                //停止音乐并寻找下一个
                stopAndPlayNext(true);
            }
            else{
                //重新播放
                playLoop();
            }
        }

    }

    protected static void clearAllMusic(){
        if(currentPlay != null){
            Minecraft.getInstance().getSoundManager().stop(currentPlay.getSound());
            currentPlay = null;
        }
        playList.clear();
        playState = MusicPlayState.STOPPED;
        elapsedTick = 0;
        fadeTick = 0;
        loop = false;
    }

    private static void playMusic(CEMusic music){
        //System.out.println("播放音乐");
        loop = music.isLoop();
        currentPlay = music;
        currentPlay.getSound().adjustVolume(0.01F);
        Minecraft.getInstance().getSoundManager().play(currentPlay.getSound());
        changePlayState(MusicPlayState.FADE_IN);

        if(music.getSound().getSound() == SoundManager.EMPTY_SOUND) {
            //System.out.println("找不到音频文件，停止播放");
            stopAndPlayNext(true);
        }
    }

    private static void stopSound(){
        if(currentPlay == null) return;
        Minecraft.getInstance().getSoundManager().stop(currentPlay.getSound());
    }

    public static void stopAndPlayNext(boolean fullyStop){
        if(currentPlay == null) return;

        if(fullyStop){
            //System.out.println("完全停止音乐");
            stopSound();
            currentPlay = null;
            playNextMusic(false);
        }
        else {
            loop = false;
            //System.out.println("进入过渡，等待停止音乐");
            changePlayState(MusicPlayState.FADE_OUT);
        }
    }

    public static void requestPlay(CEMusic music){
        //System.out.println("请求播放音乐");
        if (currentPlay == null) {
            //System.out.println("当前没有正在播放的音乐，直接播放");
            playMusic(music);
        }
        else if (music.isCanAddToList()) {
            //System.out.println("当前正在播放音乐，加入播放队列");
            playList.add(music);
        }
    }

    public static void softChangePlay(CEMusic music){
        //正在播放并且请求UUID匹配
        if(currentPlay != null && currentPlay.getRequestUUID().equals(music.getRequestUUID())){
            //插入下一首播放
            playList.add(0, music);
            //停止当前播放
            stopAndPlayNext(false);
        }
        //没有播放的音乐，直接正常请求播放
        else if(currentPlay == null){
            requestPlay(music);
        }
        //都不满足，将后续会播放的原本音乐替换
        else{
            replacePlayListMusic(music);
        }
    }

    public static void playNextMusic(boolean forceChange){
        //System.out.println("尝试播放下一个音乐");
        if(!playList.isEmpty()){
            //如果当前有正在播放的音乐，强行停止
            if(currentPlay != null && forceChange) {
                stopAndPlayNext(true);
            }
            else{
                CEMusic music = playList.get(0);
                removeMusicFromList(music.getRequestUUID());
                requestPlay(music);
            }
        }
        else{
            //System.out.println("播放列表为空，播放状态切换成停止");
            changePlayState(MusicPlayState.STOPPED);
        }
    }

    private static void playLoop() {
        //System.out.println("循环播放");
        stopSound();
        playMusic(currentPlay);
        changePlayState(MusicPlayState.FADE_IN);
    }

    public static void replacePlayListMusic(CEMusic music){
        playList.replaceAll(existingMusic -> {
            if (existingMusic != null && music.getRequestUUID().equals(existingMusic.getRequestUUID())) {
                return music;
            }
            return existingMusic;
        });
    }

    public static void removeMusic(UUID requestUUID, boolean forceRemove){
        //检查当前播放是否是相同的音乐
        if(currentPlay != null && currentPlay.getRequestUUID().equals(requestUUID)){
            stopAndPlayNext(forceRemove);
        }

        //检查列表是否有相同的音乐
        if(!playList.isEmpty()){
            removeMusicFromList(requestUUID);
        }
    }

    public static void removeMusicFromList(UUID requestUUID){
        //System.out.println("从等待播放列表中移除音乐");
        List<CEMusic> removeMusics = playList.stream().filter(ceMusic -> ceMusic.getRequestUUID().equals(requestUUID)).toList();
        playList.removeAll(removeMusics);
    }

    private static void changePlayState(MusicPlayState musicPlayState){
        if(currentPlay == null) return;

        switch(musicPlayState){
            case FADE_IN -> {
                targetVolume = currentPlay.getVolume();
                fadeTick = currentPlay.getFadeIn();
            }
            case PLAYING -> {
                fadeTick = 0;
            }
            case FADE_OUT -> {
                targetVolume = currentPlay.getSound().getVolume();
                fadeTick = currentPlay.getFadeOut();
            }
            case STOPPED -> {
                elapsedTick = 0;
                fadeTick = 0;
            }
        }

        playState = musicPlayState;
    }
}
