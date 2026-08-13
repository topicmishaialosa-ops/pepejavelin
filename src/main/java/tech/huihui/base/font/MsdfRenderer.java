package tech.huihui.base.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class MsdfRenderer {
   public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, color, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      float thickness = 0.05F;
      float spacing = 0.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, font.getBakedTextureId());
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      font.applyGlyphs(matrix, builder, text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(MsdfFont font, String text, float size, int color, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd) {
      float maxWidth = font.getWidth(text, size) * 2.0F;
      renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, int alpha) {
      renderText(font, text, size, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F, alpha);
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      float thickness = 0.05F;
      float spacing = 0.0F;
      List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, ColorRGBA.WHITE.getRGB());
      float currentX = x;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, font.getBakedTextureId());
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      Iterator<FormattedTextProcessor.TextSegment> var16 = segments.iterator();
      while(var16.hasNext()) {
         FormattedTextProcessor.TextSegment segment = var16.next();
         font.applyGlyphs(matrix, builder, segment.text(), size, thickness * 0.5F * size, spacing - 0.3F, currentX - 0.75F, y + size * 0.7F, z, segment.color(), enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
         currentX += font.getWidth(segment.text(), size);
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth, int alpha) {
      float thickness = 0.05F;
      float spacing = 0.0F;
      List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text, ColorRGBA.WHITE.getRGB());
      float currentX = x;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, font.getBakedTextureId());
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      Iterator<FormattedTextProcessor.TextSegment> var17 = segments.iterator();
      while(var17.hasNext()) {
         FormattedTextProcessor.TextSegment segment = var17.next();
         int color = segment.color();
         if (alpha != 255) {
            color = color & 16777215 | (alpha & 255) << 24;
         }

         font.applyGlyphs(matrix, builder, segment.text(), size, thickness * 0.5F * size, spacing - 0.3F, currentX - 0.75F, y + size * 0.7F, z, color, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
         currentX += font.getWidth(segment.text(), size);
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(MsdfFont font, Text text, float size, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd) {
      float maxWidth = font.getTextWidth(text, size) * 2.0F;
      renderText(font, text, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   public static void renderText(MsdfFont font, String text, float size, Gradient color, Matrix4f matrix, float x, float y, float z) {
      renderText(font, text, size, color, matrix, x, y, z, false, 0.0F, 1.0F, 0.0F);
   }

   public static void renderText(MsdfFont font, String text, float size, Gradient color, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      text = text.replace("і", "i").replace("І", "I");
      float thickness = 0.05F;
      float spacing = 0.0F;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, font.getBakedTextureId());
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      font.applyGlyphs(matrix, builder, text, size, thickness * 0.5F * size, spacing, x - 0.75F, y + size * 0.7F, z, color, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void renderText(MsdfFont font, String text, float size, Gradient color, Matrix4f matrix, float x, float y, float z, boolean enableFadeout, float fadeoutStart, float fadeoutEnd) {
      float maxWidth = font.getWidth(text, size) * 2.0F;
      renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
   }

   @Generated
   private MsdfRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}