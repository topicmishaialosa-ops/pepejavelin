package tech.huihui.utility.render.display.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import tech.huihui.HuihuiClient;
import tech.huihui.utility.interfaces.IWindow;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.VulkanCompatibility;
import tech.huihui.utility.render.display.Render2DUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomSprite;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class DrawUtil implements IWindow {
   public static final float DEFAULT_SMOOTHNESS = 0.8F;

   public static void initializeShaders() {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.initializeShaders();
      }
   }

   public static void updateBuffer() {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.updateBuffer();
      }
   }

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, from.x, from.y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, to.x, to.y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }

   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      matrices.push();

      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

         for(int i = 0; i <= resolution; ++i) {
            float t = (float)i / (float)resolution;
            float x = (float)MathUtil.cubicBezier((double)t, (double)p0.x, (double)p1.x, (double)p2.x, (double)p3.x);
            float y = (float)MathUtil.cubicBezier((double)t, (double)p0.y, (double)p1.y, (double)p2.y, (double)p3.y);
            builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
         drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawHueBar(MatrixStack matrices, float x, float y, float width, float height, float alpha) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      int segments = 32;
      float segmentH = height / (float) segments;
      int prev = ColorRGBA.fromHSB(0.0F, 1.0F, 1.0F).withAlpha(alpha).getRGB();

      for (int i = 1; i <= segments; ++i) {
         int curr = ColorRGBA.fromHSB((float) i / (float) segments, 1.0F, 1.0F).withAlpha(alpha).getRGB();
         float y0 = y + (float) (i - 1) * segmentH;
         float y1 = y + (float) i * segmentH;
         builder.vertex(matrix4f, x, y1, 0.0F).color(curr);
         builder.vertex(matrix4f, x + width, y1, 0.0F).color(curr);
         builder.vertex(matrix4f, x + width, y0, 0.0F).color(prev);
         builder.vertex(matrix4f, x, y0, 0.0F).color(prev);
         prev = curr;
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      matrices.pop();
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawSquircle(matrices, x, y, width, height, squirt, borderRadius, color);
         return;
      }
      ShapeRenderer.drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
   }

   public static void drawLoadingRect(MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawLoadingRect(matrices, x, y, width, height, progress, borderRadius, color);
         return;
      }
      float p = Math.max(0.0F, Math.min(1.0F, progress));
      ShapeRenderer.drawRoundedRect(matrices, x, y, width * p, height, borderRadius, color);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
         return;
      }
      ShapeRenderer.drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color1, ColorRGBA color2, ColorRGBA color3, ColorRGBA color4) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedRect(matrices, x, y, width, height, borderRadius, color1, color2, color3, color4);
         return;
      }
      ShapeRenderer.drawGradientRoundedRect(matrices, x, y, width, height, borderRadius, color1, color2, color3, color4);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      drawRoundedRect(matrices, x, y, width, height, borderRadius, gradient.getTopLeftColor(), gradient.getBottomLeftColor(), gradient.getBottomRightColor(), gradient.getTopRightColor());
   }

   public static void drawRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      if (borderThickness <= 0.0F) {
         drawRoundedRect(matrices, x, y, width, height, borderRadius, borderColor);
         return;
      }
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedBorder(matrices, x, y, width, height, borderThickness, borderRadius, borderColor);
         return;
      }
      ShapeRenderer.drawRoundedBorder(matrices, x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public static void drawMetanoise(MatrixStack matrices, float x, float y, float w, float h, float time, float radius, ColorRGBA bgColor, ColorRGBA outlineColor) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawMetanoise(matrices, x, y, w, h, time, radius, bgColor, outlineColor);
         return;
      }
      drawRoundedRect(matrices, x, y, w, h, BorderRadius.all(radius), bgColor);
   }

   public static void drawRoundedCorner(MatrixStack matrices, float x, float y, float width, float height, float borderThikenes, float delta, ColorRGBA color, BorderRadius radius) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedCorner(matrices, x, y, width, height, borderThikenes, delta, color, radius);
         return;
      }
      x -= 0.3F;
      y -= 0.3F;
      width += 0.6F;
      height += 0.6F;
      drawRoundedCornerOnly(matrices, x, y, delta, delta, borderThikenes, radius, color, 0.0F);
      drawRoundedCornerOnly(matrices, x + width - delta, y, delta, delta, borderThikenes, radius, color, 1.0F);
      drawRoundedCornerOnly(matrices, x, y + height - delta, delta, delta, borderThikenes, radius, color, 2.0F);
      drawRoundedCornerOnly(matrices, x + width - delta, y + height - delta, delta, delta, borderThikenes, radius, color, 3.0F);
   }

   public static void drawRoundedCornerOnly(MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor, float cornerIdex) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedCornerOnly(matrices, x, y, width, height, borderThickness, borderRadius, borderColor, cornerIdex);
         return;
      }
      if (borderThickness <= 0.0F) {
         drawRect(matrices, x, y, width, height, borderColor);
         return;
      }
      ShapeRenderer.drawRoundedBorder(matrices, x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, Gradient textureColor) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      drawSetup();
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getTopLeftColor().getRGB());
      builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getBottomLeftColor().getRGB());
      builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getBottomRightColor().getRGB());
      builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getTopRightColor().getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      matrices.push();
      int color = clor.getRGB();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float x2 = x + width;
      float y2 = y + height;
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
      builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
      builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
      builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
      drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
      RenderSystem.disableBlend();
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      drawTexture(matrices, sprite.getTexture(), x, y, width, height, 0.0F, 1.0F, 0.0F, 1.0F, color);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, ColorRGBA.WHITE);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, color);
         return;
      }
      ShapeRenderer.drawRoundedTexture(matrices, identifier, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, borderRadius, color);
   }

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawShadow(matrices, x, y, width, height, softness, borderRadius, color);
         return;
      }
      ShapeRenderer.drawSoftRoundedRect(matrices, x, y, width, height, borderRadius, softness, color);
   }

   public static void drawBlurHud(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawBlurHud(matrices, x, y, width, height, blurRadius, borderRadius, color);
      }
   }

   public static void drawBlurHudBooleanCheck(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color, boolean d, boolean f) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawBlurHudBooleanCheck(matrices, x, y, width, height, blurRadius, borderRadius, color, d, f);
      }
   }

   public static void drawGlow(MatrixStack matrixStack, float x, float y, float width, float height, int glowRadius) {
      Render2DUtil.drawGradientBlurredShadow(matrixStack, x, y, width, height, glowRadius, Gradient.of(HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor(), HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor(), HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getSecondColor(), HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getSecondColor()));
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawBlur(matrices, x, y, width, height, blurRadius, borderRadius, color);
         return;
      }
      drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      RenderSystem.setShaderTexture(0, identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float)x, (float)(y + height), (float)z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)(y + height), (float)z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float)(x + width), (float)y, (float)z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float)x, (float)y, (float)z).texture(0.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawPlayerHeadWithRoundedShader(MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
         return;
      }
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.125F, 0.125F, 0.25F, 0.25F);
   }

   private static void drawPlayerHatLayerWithRoundedShader(MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color, 0.625F, 0.125F, 0.75F, 0.25F);
      RenderSystem.disableBlend();
   }

   public static void drawRoundedTextureWithUV(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color, float u1, float v1, float u2, float v2) {
      if (!VulkanCompatibility.isVulkanModLoaded()) {
         GlDrawRenderer.drawRoundedTextureWithUV(matrices, identifier, x, y, width, height, borderRadius, color, u1, v1, u2, v2);
         return;
      }
      ShapeRenderer.drawRoundedTexture(matrices, identifier, x, y, width, height, u1, v1, u2, v2, borderRadius, color);
   }

   public static void drawTexturedQuad(MatrixStack matrices, Identifier texture, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float u1, float v1, float u2, float v2, ColorRGBA color) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderTexture(0, texture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      buffer.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1).color(color.getRGB());
      buffer.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v1).color(color.getRGB());
      buffer.vertex(matrix4f, x3, y3, 0.0F).texture(u2, v2).color(color.getRGB());
      buffer.vertex(matrix4f, x4, y4, 0.0F).texture(u1, v2).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
   }

   public static void drawSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void drawEnd() {
      RenderSystem.disableBlend();
   }

   @Generated
   private DrawUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}