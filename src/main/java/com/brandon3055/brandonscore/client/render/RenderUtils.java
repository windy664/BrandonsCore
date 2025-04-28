package com.brandon3055.brandonscore.client.render;

import codechicken.lib.gui.modular.lib.GuiRender;
import codechicken.lib.gui.modular.sprite.Material;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

/**
 * Created by brandon3055 on 16/10/18.
 */
public class RenderUtils {

    public static RenderType FAN_TYPE = RenderType.create("tri_fan_type", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN, 256, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
//            .setAlphaState(RenderStateShard.NO_ALPHA)
//            .setTexturingState(new RenderStateShard.TexturingStateShard("lighting", RenderSystem::disableLighting, SneakyUtils.none()))
                    .createCompositeState(false)
    );

//    /**
//     * * @return The buffer source used for GUI rendering. You must ALWAYS call endBatch on this when you are done with it.
//     */
//    @Deprecated
//    public static MultiBufferSource.BufferSource getGuiBuffers() {
//        return MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//    }

//    @Deprecated
//    public static MultiBufferSource.BufferSource getBuffers() {
//        return Minecraft.getInstance().renderBuffers().bufferSource();
//    }

    @Deprecated
    public static void endBatch(MultiBufferSource getter) {
        if (getter instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) getter).endBatch();
        }
    }

    public static void drawPieProgress(GuiRender render, double x, double y, double diameter, double progress, double offsetAngle, int colour) {
        drawPieProgress(render, x, y, diameter, progress, offsetAngle, colour, colour);
    }

    public static void drawPieProgress(GuiRender render, double x, double y, double diameter, double progress, double offsetAngle, int innerColour, int outerColour) {
        float radius = (float) diameter / 2;
        VertexConsumer builder = new TransformingVertexConsumer(render.buffers().getBuffer(FAN_TYPE), render.pose());
        builder.addVertex((float) (x + radius), (float) (y + radius), 0).setColor(innerColour);
        for (double d = 0; d <= 1; d += 1D / 30D) {
            float angle = (float) ((d * progress) + 0.5F - progress);
            angle *= (float) Math.PI * 2;
            angle += (float) MathHelper.torad * (float) offsetAngle;
            float vertX = (float) (x + radius + Math.sin(angle) * radius);
            float vertY = (float) (y + radius + Math.cos(angle) * radius);
            builder.addVertex(vertX, vertY, 0).setColor(outerColour);
        }
    }

    public static Material fromRawTexture(ResourceLocation texture) {
        return new Material(texture, texture, FullSprite::new);
    }

    private static class FullSprite extends TextureAtlasSprite {
        private FullSprite(ResourceLocation location) {
            super(location, new SpriteContents(location, new FrameSize(1, 1), new NativeImage(1, 1, false), ResourceMetadata.EMPTY), 1, 1, 0, 0);
        }

        @Override
        public float getU(float u)
        {
            return u / 16;
        }

        @Override
        public float getV(float v)
        {
            return v / 16;
        }
    }
}