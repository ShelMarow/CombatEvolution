package net.shelmarow.combat_evolution.config.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.combat_evolution.client.hud.element.ExecutionElement;
import net.shelmarow.combat_evolution.client.hud.execution.HUDAlignment;
import net.shelmarow.combat_evolution.config.CEClientConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HUDConfigScreen extends Screen {

    private final List<ExecutionElement> hudElements = new ArrayList<>();
    private ExecutionElement selectedHUDElement = null;
    private final Screen parent;

    protected HUDConfigScreen(Screen parent) {
        super(Component.empty());
        this.parent = parent;
    }

    @Override
    protected void init() {
        hudElements.clear();

        Vec2 pos = getRenderPos();

        ExecutionElement executionIcon = new ExecutionElement(
                (int) pos.x, (int) pos.y,
                32,32
        );

        hudElements.add(executionIcon);
    }

    private Vec2 getRenderPos() {
        int screenWidth = this.width;
        int screenHeight = this.height;

        int renderX = 0;
        int renderY = 0;
        int iconSize = 32;

        int posX = CEClientConfig.ICON_X.get().intValue();
        int posY = CEClientConfig.ICON_Y.get().intValue();

        HUDAlignment alignment = CEClientConfig.ICON_ALIGNMENT.get();
        switch (alignment) {
            case CENTER->{
                renderX = screenWidth / 2 + posX;
                renderY = screenHeight / 2 + posY;
            }
            case CENTER_LEFT -> {
                renderX = posX;
                renderY = screenHeight / 2 + posY;
            }
            case CENTER_RIGHT -> {
                renderX = screenWidth + posX;
                renderY = screenHeight / 2 + posY;
            }
            case TOP -> {
                renderX = screenWidth / 2 + posX;
                renderY = screenHeight + posY;
            }
            case TOP_LEFT -> {
                renderX = posX;
                renderY = screenHeight + posY;
            }
            case TOP_RIGHT-> {
                renderX = screenWidth + posX;
                renderY = screenHeight + posY;
            }
            case BOTTOM -> {
                renderX = screenWidth / 2 + posX;
                renderY = posY;
            }
            case BOTTOM_LEFT -> {
                renderX = posX;
                renderY = posY;
            }
            case BOTTOM_RIGHT -> {
                renderX = screenWidth + posX;
                renderY = posY;
            }
        }

        renderX = Mth.clamp(renderX, 0, screenWidth - iconSize);
        renderY = Mth.clamp(renderY, 0, screenHeight - iconSize);

        return new Vec2(renderX, renderY);
    }

    public Vec3 getOffsetPos(double posX, double posY) {
        HUDAlignment alignment = CEClientConfig.ICON_ALIGNMENT.get();
        switch (alignment) {
            case CENTER->{
                posX -= this.width / 2F;
                posY -= this.height / 2F;
            }
            case CENTER_LEFT -> {
                posY -= this.height / 2F;
            }
            case CENTER_RIGHT -> {
                posX -= this.width;
                posY -= this.height / 2F;
            }
            case TOP -> {
                posX -= this.width / 2F;
                posY -= this.height;
            }
            case TOP_LEFT -> {
                posY -= this.height;
            }
            case TOP_RIGHT-> {
                posX -= this.width;
                posY -= this.height;
            }
            case BOTTOM -> {
                posX -= this.width / 2F;
            }
            case BOTTOM_LEFT -> {
            }
            case BOTTOM_RIGHT -> {
                posX -= this.width;
            }
        }
        return new Vec3(posX, posY, 0);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics);

        for(ExecutionElement hudElement : hudElements){
            hudElement.renderExample(pGuiGraphics, pPartialTick);
        }


        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }


    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        for(ExecutionElement hudElement : hudElements){
            if(selectedHUDElement == null && hudElement.isMouseOver((int) pMouseX, (int) pMouseY)){
                selectedHUDElement = hudElement;
                selectedHUDElement.selected = true;
                break;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if(selectedHUDElement != null){
            Vec3 pos = getOffsetPos(selectedHUDElement.x, selectedHUDElement.y);
            CEClientConfig.ICON_X.set(pos.x);
            CEClientConfig.ICON_Y.set(pos.y);
            CEClientConfig.CLIENT_SPEC.save();

            selectedHUDElement.selected = false;
            selectedHUDElement = null;
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        if(selectedHUDElement != null){
            selectedHUDElement.x += pDragX;
            selectedHUDElement.y += pDragY;
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }


    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
        else {
            super.onClose();
        }
    }
}
