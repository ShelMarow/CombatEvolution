package net.shelmarow.combat_evolution.bgm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.shelmarow.combat_evolution.bgm.cesound.CEVariableVolumeSound;

import java.util.UUID;

public class CEMusic {

    private UUID requestUUID;
    private final CEVariableVolumeSound sound;
    private final ResourceLocation musicPath;
    private final SoundSource source;
    private final float volume;
    private final int duration;
    private final boolean loop;
    private final boolean canAddToList;
    private final int fadeIn;
    private final int fadeOut;

    public CEMusic(ResourceLocation musicPath, SoundSource source, float volume, int duration, boolean loop, boolean canAddToList) {
        this(musicPath, source, UUID.randomUUID(), volume, duration, loop, canAddToList);
    }

    public CEMusic(ResourceLocation musicPath, SoundSource source, UUID requestUUID, float volume, int duration, boolean loop, boolean canAddToList) {
        this(musicPath, source, requestUUID, volume, duration, loop, canAddToList, 0, 0);
    }

    public CEMusic(ResourceLocation musicPath, SoundSource source, UUID requestUUID, float volume, int duration, boolean loop, boolean canAddToList, int fadeIn, int fadeOut) {
        this.musicPath = musicPath;
        this.source = source;
        this.requestUUID = requestUUID;
        this.volume = volume;
        this.duration = duration;
        this.loop = loop;
        this.canAddToList = canAddToList;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.sound = new CEVariableVolumeSound(SoundEvent.createVariableRangeEvent(musicPath),source);
    }

    public ResourceLocation getMusicPath() {
        return musicPath;
    }

    public SoundSource getSource() {
        return source;
    }

    public int getFadeIn() {
        return fadeIn;
    }

    public int getFadeOut() {
        return fadeOut;
    }

    public int getDuration() {
        return duration;
    }

    public UUID getRequestUUID() {
        return requestUUID;
    }

    public void setRequestUUID(UUID requestUUID) {
        this.requestUUID = requestUUID;
    }

    public CEVariableVolumeSound getSound() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isCanAddToList() {
        return canAddToList;
    }
}
