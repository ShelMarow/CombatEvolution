package net.shelmarow.combat_evolution.example.entity.shelmarow;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.shelmarow.combat_evolution.CombatEvolution;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ShelMarowRenderer extends HumanoidMobRenderer<ShelMarow, HumanoidModel<ShelMarow>> {

    public ShelMarowRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ShelMarow pEntity) {
        return ResourceLocation.fromNamespaceAndPath(CombatEvolution.MOD_ID, "textures/entity/shelmarow.png");
    }
}
