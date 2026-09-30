package net.shelmarow.combat_evolution.bossbar.client.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.shelmarow.combat_evolution.bossbar.BossData;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBossBarType {
    protected final String name;
    protected final ResourceLocation defaultTexture;

    protected AbstractBossBarType(String id, ResourceLocation defaultTexture) {
        this.name = id;
        this.defaultTexture = defaultTexture;
    }

    public abstract void render(CustomizeGuiOverlayEvent.BossEventProgress event, BossData bossData);

    public String getName() {
        return name;
    }

    public ResourceLocation getDefaultTexture() {
        return defaultTexture;
    }

    public int getPreviewWidth() {
        return 256;
    }

    public int getPreviewHeight() {
        return 20;
    }

    public void renderPreview(GuiGraphics graphics, int x, int y, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(defaultTexture, 0, 0, 0, 0, getPreviewWidth(), getPreviewHeight(), 256, 256);
        graphics.pose().popPose();
    }
}
