package net.shelmarow.combat_evolution.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.shelmarow.combat_evolution.CombatEvolution;

public class CESounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CombatEvolution.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCE = registerSound("misc.silence");
    public static final DeferredHolder<SoundEvent, SoundEvent> COUNTER = registerSound("skill.counter");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXECUTION_1 = registerSound("skill.execution1");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXECUTION_2 = registerSound("skill.execution2");


    public static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        ResourceLocation res = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(res));
    }
}
