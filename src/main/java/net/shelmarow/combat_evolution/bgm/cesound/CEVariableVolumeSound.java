package net.shelmarow.combat_evolution.bgm.cesound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CEVariableVolumeSound extends AbstractTickableSoundInstance {

    public CEVariableVolumeSound(SoundEvent soundEvent, SoundSource soundSource) {
        super(soundEvent, soundSource, RandomSource.create());
        this.volume = 0F;
    }

    public void adjustVolume(float volume) {
        this.volume = Math.max(0F, volume);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {

    }
}
