package net.shelmarow.combat_evolution.example.entity.shelmarow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEBossEntity;
import net.shelmarow.combat_evolution.bgm.network.CEMusicNetworkHandler;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;
import yesman.epicfight.world.item.EpicFightItems;

import java.util.UUID;

public class ShelMarow extends CEBossEntity {

    private final UUID bgmUUID = UUID.randomUUID();

    public ShelMarow(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        ItemStack weapon = new ItemStack(EpicFightItems.IRON_LONGSWORD.get());
        setItemSlot(EquipmentSlot.MAINHAND,weapon);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)

                .add(EpicFightAttributes.IMPACT.get(),1.0D)
                .add(EpicFightAttributes.MAX_STAMINA.get(),40.0D)
                .add(EpicFightAttributes.STAMINA_REGEN.get(),1.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        super.startSeenByPlayer(pPlayer);
        CEMusicNetworkHandler.sendRequestPlayPacket(
                pPlayer,
                ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "music.test_bgm"),
                SoundSource.RECORDS, bgmUUID,
                0.5F,3500,
                true,true,
                40, 40
        );
    }

    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer pPlayer) {
        super.stopSeenByPlayer(pPlayer);
        CEMusicNetworkHandler.sendRemoveMusicPacket(pPlayer, bgmUUID, false);
    }
}
