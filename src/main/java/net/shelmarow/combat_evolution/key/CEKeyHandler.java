package net.shelmarow.combat_evolution.key;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.bgm.CEMusic;
import net.shelmarow.combat_evolution.bgm.cesound.CEVariableVolumeSound;
import net.shelmarow.combat_evolution.execution.ExecutionHandler;
import net.shelmarow.combat_evolution.execution.network.C2STryExecutionPacket;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.html.parser.Entity;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = CombatEvolution.MOD_ID,bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CEKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && mc.player != null) {
            if(CEKeyMappings.EXECUTION.consumeClick()) {
                LivingEntity entity = ExecutionHandler.getEntityLookedAt(mc.player, ExecutionHandler.EXECUTION_DISTANCE);
                if(entity != null) {
                    CombatEvolution.CHANNEL.sendToServer(new C2STryExecutionPacket());
                }
            }
        }
    }
}
