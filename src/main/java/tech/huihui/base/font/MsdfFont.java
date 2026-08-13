package tech.huihui.base.font;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Generated;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import tech.huihui.HuihuiClient;
import tech.huihui.utility.game.other.ReplaceUtil;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.Gradient;

public final class MsdfFont implements IMinecraft {
   private static final float BAKE_SMOOTHNESS = 2.5F;
   private static final float BAKE_THICKNESS = 0.05F;
   private final String name;
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Map<Integer, MsdfGlyph> glyphs;
   private final Map<Integer, Map<Integer, Float>> kernings;
   private final Identifier atlasIdentifier;
   private final Map<Float, Font> fontCache = new HashMap<>();
   private volatile AbstractTexture bakedTexture;

   private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings, Identifier atlasIdentifier) {
      this.name = name;
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
      this.atlasIdentifier = atlasIdentifier;
   }

   public int getTextureId() {
      return this.texture.getGlId();
   }

   public int getBakedTextureId() {
      AbstractTexture baked = this.bakedTexture;
      if (baked == null) {
         synchronized(this) {
            baked = this.bakedTexture;
            if (baked == null) {
               baked = this.bakeTexture();
               this.bakedTexture = baked;
            }
         }
      }

      return baked == null ? this.getTextureId() : baked.getGlId();
   }

   private AbstractTexture bakeTexture() {
      Optional<Resource> resource = mc.getResourceManager().getResource(this.atlasIdentifier);
      if (!resource.isPresent()) {
         return null;
      }

      try {
         InputStream inputStream = ((Resource)resource.get()).getInputStream();

         NativeImage src;
         try {
            src = NativeImage.read(inputStream);
         } catch (Throwable var13) {
            try {
               inputStream.close();
            } catch (Throwable var11) {
               var13.addSuppressed(var11);
            }

            throw var13;
         }

inputStream.close();
          NativeImageBackedTexture baked;
          try {
             int width = src.getWidth();
             int height = src.getHeight();
             float range = this.atlas.range();
             NativeImage out = new NativeImage(NativeImage.Format.RGBA, width, height, false);

             for(int y = 0; y < height; ++y) {
                for(int x = 0; x < width; ++x) {
                   int argb = src.getColorArgb(x, y);
                   float red = (float)(argb >> 16 & 255);
                   float green = (float)(argb >> 8 & 255);
                   float blue = (float)(argb & 255);
                   float median = median(red, green, blue) / 255.0F;
                   float distance = median - 0.5F + BAKE_THICKNESS;
                   float alpha = smoothstep(-BAKE_SMOOTHNESS, BAKE_SMOOTHNESS, distance * range);
                   out.setColorArgb(x, y, (Math.round(alpha * 255.0F) & 255) << 24 | 0xFFFFFF);
                }
             }

             baked = new NativeImageBackedTexture(out);
          } finally {
             src.close();
          }

         baked.setFilter(true, false);
         return baked;
      } catch (IOException var12) {
         return null;
      }
   }

   private static float median(float red, float green, float blue) {
      return Math.max(Math.min(red, green), Math.min(Math.max(red, green), blue));
   }

   private static float smoothstep(float edge0, float edge1, float value) {
      float t = (value - edge0) / (edge1 - edge0);
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
      applyGlyphs(matrix, consumer, text, size, thickness, spacing, x, y, z, color, false, 0.0F, 1.0F, 0.0F);
   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      this.texture.setFilter(true, true);
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      boolean skipNext = false;
      float startX = x;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c == 7424) {
            c = 1040;
         }

         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  x += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, fadeColor(color, x, startX, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth)) + thickness + spacing;
               prevChar = c;
            }
         }
      }

   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, Gradient color) {
      this.texture.setFilter(true, true);
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      boolean skipNext = false;
      float startX = x;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  x += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color, fadeFactor(x, startX, false, 0.0F, 1.0F, 0.0F)) + thickness + spacing;
               prevChar = c;
            }
         }
      }

   }

   public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, Gradient color, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      this.texture.setFilter(true, true);
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      boolean skipNext = false;
      float startX = x;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  x += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color, fadeFactor(x, startX, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth)) + thickness + spacing;
               prevChar = c;
            }
         }
      }

   }

   private static float fadeFactor(float glyphX, float startX, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      if (!enableFadeout || maxWidth <= 0.0F) {
         return 1.0F;
      }

      float relativeX = glyphX - startX;
      float normalizedX = relativeX / maxWidth;
      float alpha = 1.0F;
      if (normalizedX > fadeoutStart) {
         alpha = 1.0F - smoothstep(fadeoutStart, fadeoutEnd, normalizedX);
      }

      return Math.max(0.0F, Math.min(1.0F, alpha));
   }

   private static int fadeColor(int color, float glyphX, float startX, boolean enableFadeout, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      float alpha = fadeFactor(glyphX, startX, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
      int baseAlpha = color >>> 24 & 255;
      return color & 0xFFFFFF | (Math.round(baseAlpha * alpha) & 255) << 24;
   }

   public float getWidth(String text, float size) {
      text = ReplaceUtil.replaceSymbols(text);
      int prevChar = -1;
      float width = 0.0F;
      boolean skipNext = false;

      for(int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c == 7424) {
            c = 1040;
         }

         if (skipNext) {
            skipNext = false;
         } else if (c == 167) {
            skipNext = true;
         } else {
            MsdfGlyph glyph = (MsdfGlyph)this.glyphs.get(Integer.valueOf(c));
            if (glyph != null) {
               Map<Integer, Float> kerning = (Map)this.kernings.get(prevChar);
               if (kerning != null) {
                  width += (Float)kerning.getOrDefault(Integer.valueOf(c), 0.0F) * size;
               }

               width += glyph.getWidth(size);
               prevChar = c;
            }
         }
      }

      return width;
   }

   public float getTextWidth(Text text, float size) {
      return this.getWidth(text.getString(), size);
   }

   public Font getFont(float size) {
      Font cached = this.fontCache.get(size);
      if (cached == null) {
         cached = new Font(this, size);
         this.fontCache.put(size, cached);
      }
      return cached;
   }

   public static MsdfFont.Builder builder() {
      return new MsdfFont.Builder();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public FontData.AtlasData getAtlas() {
      return this.atlas;
   }

   @Generated
   public FontData.MetricsData getMetrics() {
      return this.metrics;
   }

   public static class Builder {
      private String name = "?";
      private Identifier dataIdentifer;
      private Identifier atlasIdentifier;

      private Builder() {
      }

      public MsdfFont.Builder name(String name) {
         this.name = name;
         return this;
      }

      public MsdfFont.Builder data(String dataFileName) {
         this.dataIdentifer = HuihuiClient.id("fonts/msdf/" + dataFileName + ".json");
         return this;
      }

      public MsdfFont.Builder atlas(String atlasFileName) {
         this.atlasIdentifier = HuihuiClient.id("fonts/msdf/" + atlasFileName + ".png");
         return this;
      }

      public MsdfFont build() {
         FontData data = (FontData)ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
         AbstractTexture texture = IMinecraft.mc.getTextureManager().getTexture(this.atlasIdentifier);
         if (data == null) {
            throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() + "; Are you sure this is json file? Try to check the correctness of its syntax.");
         } else {
            RenderSystem.recordRenderCall(() -> {
               texture.setFilter(true, false);
            });
            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = (Map)data.glyphs().stream().collect(Collectors.toMap(FontData.GlyphData::unicode, (glyphData) -> {
               return new MsdfGlyph(glyphData, aWidth, aHeight);
            }));
            Map<Integer, Map<Integer, Float>> kernings = new HashMap();
            data.kernings().forEach((kerning) -> {
               Map<Integer, Float> map = (Map)kernings.computeIfAbsent(kerning.leftChar(), (k) -> {
                  return new HashMap();
               });
               map.put(kerning.rightChar(), kerning.advance());
            });
            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings, this.atlasIdentifier);
         }
      }
   }
}