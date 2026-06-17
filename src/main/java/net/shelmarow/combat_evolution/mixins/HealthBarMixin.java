package net.shelmarow.combat_evolution.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.shelmarow.combat_evolution.CombatEvolution;
import net.shelmarow.combat_evolution.ai.CEHumanoidPatch;
import net.shelmarow.combat_evolution.ai.StaminaStatus;
import net.shelmarow.combat_evolution.ai.util.CEPatchUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.EntityUI;
import yesman.epicfight.client.gui.HealthBar;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.effect.VisibleMobEffect;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

@Mixin(value = HealthBar.class, remap = false)
public abstract class HealthBarMixin extends EntityUI {

    @Unique
    private static final ResourceLocation HEALTH_BAR = ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/gui/bossbar/ce_health_bar.png");

    @Inject(
            method = "draw",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onDraw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, PoseStack poseStack, MultiBufferSource buffers, float partialTicks, CallbackInfo ci) {
        if (entitypatch instanceof CEHumanoidPatch<?> ceHumanoidPatch) {
            ci.cancel();

            if(!ceHumanoidPatch.shouldDisplayHealthBar()){
                return;
            }

            Matrix4f modelViewMatrix = super.getModelViewMatrixAlignedToCamera(poseStack, entity, 0.0F, entity.getBbHeight() + 0.25F, 0.0F, true, partialTicks);
            Collection<MobEffectInstance> activeEffects = entity.getActiveEffects();

            //药水效果
            if (!activeEffects.isEmpty() && !entity.is(playerpatch.getOriginal())) {
                Iterator<MobEffectInstance> iter = activeEffects.iterator();
                int acives = activeEffects.size();
                int row = acives > 1 ? 1 : 0;
                int column = ((acives - 1) / 2);
                float startX = -0.8F + -0.3F * row;
                float startY = -0.15F + 0.15F * column;

                for (int i = 0; i <= column; i++) {
                    for (int j = 0; j <= row; j++) {
                        MobEffectInstance effectInstance = iter.next();
                        MobEffect effect = effectInstance.getEffect();
                        ResourceLocation rl = null;

                        if (effect instanceof VisibleMobEffect visibleMobEffect) {
                            rl = visibleMobEffect.getIcon(effectInstance);
                        } else {
                            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                            if (key != null) {
                                rl = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "textures/mob_effect/" + key.getPath() + ".png");
                            }
                        }

                        float x = startX + 0.3F * j;
                        float y = startY + -0.3F * i;

                        if (rl != null) {
                            drawUIAsLevelModel(modelViewMatrix, rl, buffers, x, y, x + 0.3F, y + 0.3F, 0, 0, 256, 256, 256);
                        }

                        if (!iter.hasNext()) {
                            break;
                        }
                    }
                }
            }

            final float maxHealth = entity.getMaxHealth();
            float healthPercent = Mth.clamp(entity.getHealth() / maxHealth, 0.0F, 1.0F);

            float scale = 1.0f;

            // ========== 血量 ==========
            int healthBGStartU = 3;
            int healthBGEndU = 83;
            int healthBGWidthPixels = healthBGEndU - healthBGStartU;  // 80

            int hpStartU = 6;
            int hpEndU = 80;
            int hpWidthPixels = hpEndU - hpStartU;  // 74

            // 血量显示尺寸
            float healthBGDisplayHeight = scale * (20.0f / healthBGWidthPixels);
            float healthBGLeftX = -scale / 2;

            float hpDisplayWidth = scale * ((float) hpWidthPixels / healthBGWidthPixels) * healthPercent;

            float offsetX = scale * ((float) (hpStartU - healthBGStartU) / healthBGWidthPixels);
            float hpLeftX = healthBGLeftX + offsetX;

            int hpCurrentEndU = (int) (hpStartU + hpWidthPixels * healthPercent);

            // 绘制血量背景
            EntityUI.drawUIAsLevelModel(
                    modelViewMatrix, HEALTH_BAR, buffers,
                    healthBGLeftX, -healthBGDisplayHeight / 2,
                    healthBGLeftX + scale, healthBGDisplayHeight / 2,
                    healthBGStartU, 0,
                    healthBGEndU, 20,
                    256
            );

            // 绘制血量进度
            EntityUI.drawUIAsLevelModel(
                    modelViewMatrix, HEALTH_BAR, buffers,
                    hpLeftX, -healthBGDisplayHeight / 2,
                    hpLeftX + hpDisplayWidth, healthBGDisplayHeight / 2,
                    hpStartU, 21,
                    hpCurrentEndU, 41,
                    256
            );

            // ========== 耐力 ==========
            float staminaPercent = CEPatchUtils.getStaminaPercent(ceHumanoidPatch);

            int staminaBGStartU = 6;
            int staminaBGEndU = 80;
            int staminaBGWidthPixels = staminaBGEndU - staminaBGStartU;  // 74

            int staminaStartU = 9;
            int staminaEndU = 77;
            int staminaWidthPixels = staminaEndU - staminaStartU;  // 68

            float staminaBGDisplayWidth = scale * ((float) staminaBGWidthPixels / healthBGWidthPixels);
            float staminaBGDisplayHeight = staminaBGDisplayWidth * (20.0f / staminaBGWidthPixels);
            float staminaBGLeftX = -staminaBGDisplayWidth / 2;

            float staminaDisplayWidth = staminaBGDisplayWidth * ((float) staminaWidthPixels / staminaBGWidthPixels) * staminaPercent;

            float staminaOffsetX = staminaBGDisplayWidth * ((float) (staminaStartU - staminaBGStartU) / staminaBGWidthPixels);
            float staminaLeftX = staminaBGLeftX + staminaOffsetX;

            int staminaCurrentEndU = (int) (staminaStartU + staminaWidthPixels * staminaPercent);

            EntityUI.drawUIAsLevelModel(
                    modelViewMatrix, HEALTH_BAR, buffers,
                    staminaBGLeftX, -staminaBGDisplayHeight / 2,
                    staminaBGLeftX + staminaBGDisplayWidth, staminaBGDisplayHeight / 2,
                    staminaBGStartU, 42,
                    staminaBGEndU, 62,
                    256
            );

            if (ceHumanoidPatch.getStaminaStatus() != StaminaStatus.BREAK) {
                EntityUI.drawUIAsLevelModel(
                        modelViewMatrix, HEALTH_BAR, buffers,
                        staminaLeftX, -staminaBGDisplayHeight / 2,
                        staminaLeftX + staminaDisplayWidth, staminaBGDisplayHeight / 2,
                        staminaStartU, 63,
                        staminaCurrentEndU, 83,
                        256
                );
            } else {
                float breakDisplayWidth = staminaBGDisplayWidth * ((float) staminaWidthPixels / staminaBGWidthPixels);
                EntityUI.drawUIAsLevelModel(
                        modelViewMatrix, HEALTH_BAR, buffers,
                        staminaLeftX, -staminaBGDisplayHeight / 2,
                        staminaLeftX + breakDisplayWidth, staminaBGDisplayHeight / 2,
                        staminaStartU, 84,
                        staminaEndU, 104,
                        256
                );
            }
        }

    }
}
