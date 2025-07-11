package com.brandon3055.brandonscore.client.gui;

import codechicken.lib.gui.modular.ModularGui;
import codechicken.lib.gui.modular.ModularGuiScreen;
import codechicken.lib.gui.modular.elements.GuiButton;
import codechicken.lib.gui.modular.elements.GuiElement;
import codechicken.lib.gui.modular.lib.*;
import codechicken.lib.gui.modular.lib.geometry.GuiParent;
import com.brandon3055.brandonscore.api.hud.AbstractHudElement;
import com.brandon3055.brandonscore.client.BCGuiTextures;
import com.brandon3055.brandonscore.client.hud.HudData;
import com.brandon3055.brandonscore.client.hud.HudManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.awt.*;

import static codechicken.lib.gui.modular.lib.Constraints.LayoutPos.*;


/**
 * Created by brandon3055 on 18/8/21
 */
public class HudConfigGui implements GuiProvider {
    protected GuiToolkit toolkit = new GuiToolkit("gui.brandonscore.hud_config");

    @Override
    public void buildGui(ModularGui gui) {
        gui.initFullscreenGui();
        gui.renderScreenBackground(false);
        gui.setGuiTitle(Component.translatable("gui.brandonscore.hud_config.name"));
        GuiElement<?> root = gui.getRoot();

        GuiElement<?> title = toolkit.floatingHeading(gui);
        Constraints.placeInside(title, root, TOP_CENTER, 0, 15);

        for (AbstractHudElement element : HudManager.getHudElements().values()) {
            new ElementHandler(root, element);
        }

        gui.onClose(HudData::saveIfDirty);
        gui.onKeyPressPost((key, scancode, modifiers) -> {
            InputConstants.Key input = InputConstants.getKey(key, scancode);
            if (gui.mc().options.keyInventory.isActiveAndMatches(input)) {
                gui.getScreen().onClose();
            }
        });
    }


    private class ElementHandler extends GuiElement<ElementHandler> implements BackgroundRender {
        private AbstractHudElement element;
        private float borderAnim = 0;
        private float bgAnim = 0;
        private int tick = 0;
        boolean dragging = false;
        private GuiButton settings;

        public ElementHandler(@NotNull GuiParent<?> parent, AbstractHudElement element) {
            super(parent);
            this.element = element;
            Constraints.size(this, element::width, element::height);
            Constraints.pos(this, element::xPos, element::yPos);

            settings = toolkit.createIconButton(this, 10, BCGuiTextures.getter("dark/gear"));
            settings.setTooltip(toolkit.translate("settings"));
            Constraints.placeInside(settings, this, TOP_RIGHT, -2, 2);

            settings.onPress(() -> element.createConfigDialog(settings)
                    .setNormalizedPos(settings.xMax(), settings.yMin())
                    .setCloseOnItemClicked(false)
            );

            String infoKey = String.format("hud.%s.%s.info", HudManager.HUD_REGISTRY.getKey(element).getNamespace(), HudManager.HUD_REGISTRY.getKey(element).getPath());
            String translatedInfo = I18n.get(infoKey);
            if (!infoKey.equals(translatedInfo)) {
                GuiButton infoButton = toolkit.createIconButton(this, 10, BCGuiTextures.getter("dark/info_icon"));
                Constraints.placeInside(infoButton, this, TOP_LEFT, 2, 2);
                infoButton.setTooltip(Component.literal(translatedInfo));
                infoButton.setTooltipDelay(0);
            }
        }

        @Override
        public void renderBackground(GuiRender render, double mouseX, double mouseY, float partialTicks) {
            int rgb = Color.HSBtoRGB((tick + partialTicks) / 200F, 1F, 1F);
            drawBackground(render, xMin(), yMin(), xSize(), ySize(), partialTicks, rgb);

            if (bgAnim > 0) {
                Component name = Component.translatable(String.format("hud.%s.%s.name", HudManager.HUD_REGISTRY.getKey(element).getNamespace(), HudManager.HUD_REGISTRY.getKey(element).getPath()));
                float bgAnim = Math.min(this.bgAnim + (partialTicks * 0.1F), 1);
                render.drawCenteredString(name, (float) xCenter(), (float) yCenter() - 2F, (0x00FFFFFF | ((int) (0xFF * bgAnim) << 24)), false);
            }
        }

        @Override
        public void tick(double mouseX, double mouseY) {
            if (borderAnim < 1) {
                borderAnim += 0.1F;
            } else if (bgAnim < 1) {
                bgAnim += 0.1F;
            }
            tick++;

            if (isMouseOver() && getChildren().stream().noneMatch(GuiElement::isMouseOver)) {
                getModularGui().setCursor(CursorHelper.DRAG);
            }

            super.tick(mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean captured = super.mouseClicked(mouseX, mouseY, button);
            if (!captured && isMouseOver()) {
                dragging = true;
                element.startMoving(mouseX, mouseY);
                return true;
            }
            return captured;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            if (dragging) {
                element.onDragged(mouseX, mouseY);
            }
            super.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                element.stopMoving();
                dragging = false;
            }
            return super.mouseReleased(mouseX, mouseY, button);
        }

        private void drawBackground(GuiRender render, double x, double y, double w, double h, float partialTicks, int colour) {
            float borderAnim = this.borderAnim + (partialTicks * 0.1F);
            float bgAnim = Math.min(this.bgAnim + (partialTicks * 0.1F), 1);
            render.rect(getRectangle(), ((int) (0x8F * bgAnim) << 24));

            VertexConsumer buffer = render.buffers().getBuffer(GuiRender.SOLID);
            Matrix4f mat = render.pose().last().pose();

            double boarderLength = w * 2 + h * 2;
            double bp = boarderLength * borderAnim;
            if (bp > 0) {
                double sw = Math.min(w, bp);
                //Top Boarder
                drawGradientQuad(buffer, mat, x, y, x + sw, y, x + sw - 1, y + 1, x + 1, y + 1, 0xFF000000, colour);
                drawGradientQuad(buffer, mat, x + 1, y + 1, x + sw - 1, y + 1, x + sw - 2, y + 2, x + 2, y + 2, colour, 0xFF000000);
            }

            if (bp - w > 0) {
                double sh = Math.min(h, bp - w);
                double p = sh / h;
                //Right Boarder
                drawGradientQuad(buffer, mat, x + w, y, x + w, y + sh, x + w - 1, y + sh - 1 * p, x + w - 1, y + 1, 0xFF000000, colour);
                drawGradientQuad(buffer, mat, x + w - 1, y + 1, x + w - 1, y + sh - 1, x + w - 2, y + sh - 2 * p, x + w - 2, y + 2, colour, 0xFF000000);
            }

            if (bp - w - h > 0) {
                double sw = Math.min(w, bp - w - h);
                double sx = x + w - sw;
                double p = sw / w;
                //Bottom Boarder
                drawGradientQuad(buffer, mat, sx + 2 * p, y + h - 2, sx + sw - 2, y + h - 2, sx + sw - 1, y + h - 1, sx + 1, y + h - 1, 0xFF000000, colour);
                drawGradientQuad(buffer, mat, sx + 1 * p, y + h - 1, sx + sw - 1, y + h - 1, sx + sw, y + h, sx, y + h, colour, 0xFF000000);
            }

            if (bp - w - h - w > 0) {
                double sh = Math.min(h, bp - w - h - w);
                double sy = y + h - sh;
                double p = sh / h;
                //Left Boarder
                drawGradientQuad(buffer, mat, x + 1, sy + 1 * p, x + 1, sy + sh - 1, x, sy + sh, x, sy, colour, 0xFF000000);
                drawGradientQuad(buffer, mat, x + 2, sy + 2 * p, x + 2, sy + sh - 2, x + 1, sy + sh - 1, x + 1, sy + 1 * p, 0xFF000000, colour);
            }

            render.flush();
        }

        private void drawGradientQuad(VertexConsumer buffer, Matrix4f mat, double p1A, double p1B, double p2A, double p2B, double p3A, double p3B, double p4A, double p4B, int startColor, int endColor) {
            if (startColor == endColor && endColor == 0) return;
            //@formatter:off
            float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
            float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
            float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
            float startBlue  = (float)(startColor       & 255) / 255.0F;
            float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
            float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
            float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
            float endBlue    = (float)(endColor         & 255) / 255.0F;

            buffer.addVertex(mat, (float) p4A, (float) p4B, 0).setColor(  endRed,   endGreen,   endBlue,   endAlpha);
            buffer.addVertex(mat, (float) p3A, (float) p3B, 0).setColor(  endRed,   endGreen,   endBlue,   endAlpha);
            buffer.addVertex(mat, (float) p2A, (float) p2B, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat, (float) p1A, (float) p1B, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            //@formatter:on
        }
    }

    //Dedicated screen class provided for better compatibility with other mods.
    public static class Screen extends ModularGuiScreen {
        public Screen() {
            super(new HudConfigGui());
        }
    }
}
