package tech.huihui.utility.render.display.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class ShapeRenderer {
   private static final float CELL = 1.2F;
   private static final float AA = 0.5F;
   private static final int MAX_TOTAL = 2500;
   private static final int MAX_CELLS = 240;

   private static float radiusAA(float radius) {
      float aaf = Math.min(AA, radius * 0.4F);
      return Math.max(0.2F, Math.min(aaf, Math.max(radius, 0.0F)));
   }

   private static float computeCell(float width, float height) {
      float minDim = Math.min(width, height);
      float cell = minDim <= 28.0F ? 0.5F : (minDim <= 60.0F ? 0.75F : CELL);
      float area = width * height;
      if (area > (float) MAX_TOTAL * cell * cell) {
         float budgetCell = (float) Math.sqrt(area / (float) MAX_TOTAL);
         if (budgetCell > cell) {
            cell = budgetCell;
         }
      }
      return Math.min(cell, 14.0F);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F) {
         return;
      }
      baseSetup();
      final float rr = Math.min(Math.min(radius.topLeftRadius(), radius.topRightRadius()), Math.min(radius.bottomLeftRadius(), radius.bottomRightRadius()));
      final float cr = Math.min(rr, Math.min(width, height) * 0.5F);
      final float covAA = radiusAA(cr);
      final int rgb = color.getRGB() & 0xFFFFFF;
      final float ar = color.getAlpha();
      drawCells(matrices.peek().getPositionMatrix(), x, y, width, height, cr, covAA, (px, py, u, v, cov) -> {
         return pack(rgb, ar * cov);
      });
   }

   public static void drawRoundedBorder(MatrixStack matrices, float x, float y, float width, float height, float thickness, BorderRadius radius, ColorRGBA borderColor) {
      if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F) {
         return;
      }
      baseSetup();
      final float outerR = Math.min(Math.min(radius.topLeftRadius(), radius.topRightRadius()), Math.min(radius.bottomLeftRadius(), radius.bottomRightRadius()));
      final float cr = Math.min(outerR, Math.min(width, height) * 0.5F);
      final float ir = Math.max(cr - thickness, 0.0F);
      final float outerAA = radiusAA(cr);
      final float innerAA = radiusAA(ir);
      final float ix = x + thickness;
      final float iy = y + thickness;
      final float iw = Math.max(width - thickness * 2.0F, 0.0F);
      final float ih = Math.max(height - thickness * 2.0F, 0.0F);
      final int rgb = borderColor.getRGB() & 0xFFFFFF;
      final float ar = borderColor.getAlpha();
      drawBorderCells(matrices.peek().getPositionMatrix(), x, y, width, height, cr, outerAA, ir, innerAA, ix, iy, iw, ih, rgb, ar);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float v1, float u2, float v2, BorderRadius radius, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F) {
         return;
      }
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, identifier);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      final float cr = Math.min(Math.min(radius.topLeftRadius(), radius.topRightRadius()), Math.min(radius.bottomLeftRadius(), radius.bottomRightRadius()));
      final float rr = Math.min(cr, Math.min(width, height) * 0.5F);
      final float covAA = radiusAA(rr);
      final int rgb = color.getRGB() & 0xFFFFFF;
      final float ar = color.getAlpha();
      drawTexCells(matrices.peek().getPositionMatrix(), x, y, width, height, u1, v1, u2, v2, rr, covAA, (px, py, u, v, cov) -> {
         return pack(rgb, ar * cov);
      });
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
   }

   public static void drawGradientRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius radius, ColorRGBA tl, ColorRGBA bl, ColorRGBA br, ColorRGBA tr) {
      if (width <= 0.0F || height <= 0.0F) {
         return;
      }
      baseSetup();
      final float cr = Math.min(Math.min(radius.topLeftRadius(), radius.topRightRadius()), Math.min(radius.bottomLeftRadius(), radius.bottomRightRadius()));
      final float rr = Math.min(cr, Math.min(width, height) * 0.5F);
      final float covAA = radiusAA(rr);
      final int tlC = tl.getRGB() & 0xFFFFFF;
      final int blC = bl.getRGB() & 0xFFFFFF;
      final int brC = br.getRGB() & 0xFFFFFF;
      final int trC = tr.getRGB() & 0xFFFFFF;
      final float tlA = tl.getAlpha();
      final float blA = bl.getAlpha();
      final float brA = br.getAlpha();
      final float trA = tr.getAlpha();
      drawCells(matrices.peek().getPositionMatrix(), x, y, width, height, rr, covAA, (px, py, u, v, cov) -> {
         if (cov <= 0.001F) {
            return 0;
         }
         float topR = lerpf((tlC >> 16 & 255), (trC >> 16 & 255), u);
         float topG = lerpf((tlC >> 8 & 255), (trC >> 8 & 255), u);
         float topB = lerpf((tlC & 255), (trC & 255), u);
         float botR = lerpf((blC >> 16 & 255), (brC >> 16 & 255), u);
         float botG = lerpf((blC >> 8 & 255), (brC >> 8 & 255), u);
         float botB = lerpf((blC & 255), (brC & 255), u);
         float red = lerpf(topR, botR, v);
         float green = lerpf(topG, botG, v);
         float blue = lerpf(topB, botB, v);
         float alpha = lerpf(lerpf(tlA, trA, u), lerpf(blA, brA, u), v) * cov;
         return pack((int)(red + 0.5F), (int)(green + 0.5F), (int)(blue + 0.5F), (int)(alpha + 0.5F));
      });
   }

   public static void drawSoftRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius radius, float softness, ColorRGBA color) {
      if (width <= 0.0F || height <= 0.0F) {
         return;
      }
      baseSetup();
      final float glow = Math.max(2.0F, softness);
      final float rr = Math.min(Math.min(radius.topLeftRadius(), radius.topRightRadius()), Math.min(radius.bottomLeftRadius(), radius.bottomRightRadius()));
      final int rgb = color.getRGB() & 0xFFFFFF;
      final float ar = color.getAlpha();
      drawCells(matrices.peek().getPositionMatrix(), x, y, width, height, rr, glow, (px, py, u, v, cov) -> {
         return pack(rgb, ar * cov);
      });
   }

   private static void baseSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
   }

   private interface CellColor {
      int color(float px, float py, float u, float v, float cov);
   }

   private static void drawCells(Matrix4f matrix4f, float x, float y, float width, float height, float radius, float covAA, CellColor cellColor) {
      float cell = computeCell(width, height);
      int nx = Math.max(4, Math.min((int) Math.ceil(width / cell), MAX_CELLS));
      int ny = Math.max(4, Math.min((int) Math.ceil(height / cell), MAX_CELLS));
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      boolean any = false;
      float hw = width / (float) nx;
      float hh = height / (float) ny;
      float diag = hw + hh;
      float cx = x + width * 0.5F;
      float cy = y + height * 0.5F;
      float hwHalf = width * 0.5F - radius;
      float hhHalf = height * 0.5F - radius;

      for(int cx0 = 0; cx0 < nx; ++cx0) {
         float cellX = x + hw * (float) cx0;
         float nextX = cellX + hw;
         float u0 = (float) cx0 / (float) nx;
         float u1 = (float) (cx0 + 1) / (float) nx;

         for(int cy0 = 0; cy0 < ny; ++cy0) {
            float cellY = y + hh * (float) cy0;
            float nextY = cellY + hh;
            float v0 = (float) cy0 / (float) ny;
            float v1 = (float) (cy0 + 1) / (float) ny;
            float d = roundedSDF(cellX + hw * 0.5F, cellY + hh * 0.5F, cx, cy, hwHalf, hhHalf, radius);
            if (d >= covAA + diag) {
               continue;
            }
            int c0;
            int c1;
            int c2;
            int c3;
            if (d <= -covAA - diag) {
               c0 = cellColor.color(cellX, cellY, u0, v0, 1.0F);
               c1 = cellColor.color(cellX, nextY, u0, v1, 1.0F);
               c2 = cellColor.color(nextX, nextY, u1, v1, 1.0F);
               c3 = cellColor.color(nextX, cellY, u1, v0, 1.0F);
            } else {
               c0 = cellColor.color(cellX, cellY, u0, v0, roundedCoverage(cellX, cellY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c1 = cellColor.color(cellX, nextY, u0, v1, roundedCoverage(cellX, nextY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c2 = cellColor.color(nextX, nextY, u1, v1, roundedCoverage(nextX, nextY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c3 = cellColor.color(nextX, cellY, u1, v0, roundedCoverage(nextX, cellY, cx, cy, hwHalf, hhHalf, radius, covAA));
            }
            if ((c0 | c1 | c2 | c3) == 0) {
               continue;
            }
            builder.vertex(matrix4f, cellX, cellY, 0.0F).color(c0);
            builder.vertex(matrix4f, cellX, nextY, 0.0F).color(c1);
            builder.vertex(matrix4f, nextX, nextY, 0.0F).color(c2);
            builder.vertex(matrix4f, nextX, cellY, 0.0F).color(c3);
            any = true;
         }
      }

      if (any) {
         BufferRenderer.drawWithGlobalProgram(builder.end());
      }
   }

   private static void drawBorderCells(Matrix4f matrix4f, float x, float y, float width, float height, float outerR, float outerAA, float innerR, float innerAA, float ix, float iy, float iw, float ih, int rgb, float ar) {
      float cell = computeCell(width, height);
      int nx = Math.max(4, Math.min((int) Math.ceil(width / cell), MAX_CELLS));
      int ny = Math.max(4, Math.min((int) Math.ceil(height / cell), MAX_CELLS));
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      boolean any = false;
      float hw = width / (float) nx;
      float hh = height / (float) ny;
      float diag = hw + hh;
      float ocx = x + width * 0.5F;
      float ocy = y + height * 0.5F;
      float ohw = width * 0.5F - outerR;
      float ohh = height * 0.5F - outerR;
      boolean innerExists = iw > 0.0F && ih > 0.0F;
      float icx = ix + iw * 0.5F;
      float icy = iy + ih * 0.5F;
      float ihw = iw * 0.5F - innerR;
      float ihh = ih * 0.5F - innerR;

      for(int cx = 0; cx < nx; ++cx) {
         float cellX = x + hw * (float) cx;
         float nextX = cellX + hw;

         for(int cy = 0; cy < ny; ++cy) {
            float cellY = y + hh * (float) cy;
            float nextY = cellY + hh;
            float dOuter = roundedSDF(cellX + hw * 0.5F, cellY + hh * 0.5F, ocx, ocy, ohw, ohh, outerR);
            if (dOuter >= outerAA + diag) {
               continue;
            }
            int c0;
            int c1;
            int c2;
            int c3;
            if (dOuter <= -outerAA - diag && (!innerExists || roundedSDF(cellX + hw * 0.5F, cellY + hh * 0.5F, icx, icy, ihw, ihh, innerR) >= innerAA + diag)) {
               c0 = c1 = c2 = c3 = pack(rgb, ar);
            } else {
               float o0 = roundedCoverage(cellX, cellY, ocx, ocy, ohw, ohh, outerR, outerAA);
               float o1 = roundedCoverage(cellX, nextY, ocx, ocy, ohw, ohh, outerR, outerAA);
               float o2 = roundedCoverage(nextX, nextY, ocx, ocy, ohw, ohh, outerR, outerAA);
               float o3 = roundedCoverage(nextX, cellY, ocx, ocy, ohw, ohh, outerR, outerAA);
               float i0 = innerExists ? roundedCoverage(cellX, cellY, icx, icy, ihw, ihh, innerR, innerAA) : 0.0F;
               float i1 = innerExists ? roundedCoverage(cellX, nextY, icx, icy, ihw, ihh, innerR, innerAA) : 0.0F;
               float i2 = innerExists ? roundedCoverage(nextX, nextY, icx, icy, ihw, ihh, innerR, innerAA) : 0.0F;
               float i3 = innerExists ? roundedCoverage(nextX, cellY, icx, icy, ihw, ihh, innerR, innerAA) : 0.0F;
               c0 = pack(rgb, ar * o0 * (1.0F - i0));
               c1 = pack(rgb, ar * o1 * (1.0F - i1));
               c2 = pack(rgb, ar * o2 * (1.0F - i2));
               c3 = pack(rgb, ar * o3 * (1.0F - i3));
            }
            if ((c0 | c1 | c2 | c3) == 0) {
               continue;
            }
            builder.vertex(matrix4f, cellX, cellY, 0.0F).color(c0);
            builder.vertex(matrix4f, cellX, nextY, 0.0F).color(c1);
            builder.vertex(matrix4f, nextX, nextY, 0.0F).color(c2);
            builder.vertex(matrix4f, nextX, cellY, 0.0F).color(c3);
            any = true;
         }
      }

      if (any) {
         BufferRenderer.drawWithGlobalProgram(builder.end());
      }
   }

   private static void drawTexCells(Matrix4f matrix4f, float x, float y, float width, float height, float u1, float v1, float u2, float v2, float radius, float covAA, CellColor cellColor) {
      float cell = computeCell(width, height);
      int nx = Math.max(4, Math.min((int) Math.ceil(width / cell), MAX_CELLS));
      int ny = Math.max(4, Math.min((int) Math.ceil(height / cell), MAX_CELLS));
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      boolean any = false;
      float hw = width / (float) nx;
      float hh = height / (float) ny;
      float diag = hw + hh;
      float cx = x + width * 0.5F;
      float cy = y + height * 0.5F;
      float hwHalf = width * 0.5F - radius;
      float hhHalf = height * 0.5F - radius;

      for(int cx0 = 0; cx0 < nx; ++cx0) {
         float cellX = x + hw * (float) cx0;
         float nextX = x + hw * (float) (cx0 + 1);
         float u0 = u1 + (u2 - u1) * ((float) cx0 / (float) nx);
         float u3 = u1 + (u2 - u1) * ((float) (cx0 + 1) / (float) nx);

         for(int cy0 = 0; cy0 < ny; ++cy0) {
            float cellY = y + hh * (float) cy0;
            float nextY = y + hh * (float) (cy0 + 1);
            float v0 = v1 + (v2 - v1) * ((float) cy0 / (float) ny);
            float v3 = v1 + (v2 - v1) * ((float) (cy0 + 1) / (float) ny);
            float d = roundedSDF(cellX + hw * 0.5F, cellY + hh * 0.5F, cx, cy, hwHalf, hhHalf, radius);
            if (d >= covAA + diag) {
               continue;
            }
            int c0;
            int c1;
            int c2;
            int c3;
            if (d <= -covAA - diag) {
               c0 = cellColor.color(cellX, cellY, u0, v0, 1.0F);
               c1 = cellColor.color(cellX, nextY, u0, v3, 1.0F);
               c2 = cellColor.color(nextX, nextY, u3, v3, 1.0F);
               c3 = cellColor.color(nextX, cellY, u3, v0, 1.0F);
            } else {
               c0 = cellColor.color(cellX, cellY, u0, v0, roundedCoverage(cellX, cellY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c1 = cellColor.color(cellX, nextY, u0, v3, roundedCoverage(cellX, nextY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c2 = cellColor.color(nextX, nextY, u3, v3, roundedCoverage(nextX, nextY, cx, cy, hwHalf, hhHalf, radius, covAA));
               c3 = cellColor.color(nextX, cellY, u3, v0, roundedCoverage(nextX, cellY, cx, cy, hwHalf, hhHalf, radius, covAA));
            }
            if ((c0 | c1 | c2 | c3) == 0) {
               continue;
            }
            builder.vertex(matrix4f, cellX, cellY, 0.0F).texture(u0, v0).color(c0);
            builder.vertex(matrix4f, cellX, nextY, 0.0F).texture(u0, v3).color(c1);
            builder.vertex(matrix4f, nextX, nextY, 0.0F).texture(u3, v3).color(c2);
            builder.vertex(matrix4f, nextX, cellY, 0.0F).texture(u3, v0).color(c3);
            any = true;
         }
      }

      if (any) {
         BufferRenderer.drawWithGlobalProgram(builder.end());
      }
   }

   private static float roundedSDF(float px, float py, float cx, float cy, float hw, float hh, float radius) {
      float qx = Math.abs(px - cx) - hw;
      float qy = Math.abs(py - cy) - hh;
      float ox = Math.max(qx, 0.0F);
      float oy = Math.max(qy, 0.0F);
      return (float)(Math.sqrt((double)(ox * ox + oy * oy)) + (double)Math.min(Math.max(qx, qy), 0.0F)) - radius;
   }

   private static float roundedCoverage(float px, float py, float cx, float cy, float hw, float hh, float radius, float aaf) {
      return 1.0F - smoothstepf(-aaf, aaf, roundedSDF(px, py, cx, cy, hw, hh, radius));
   }

   private static float smoothstepf(float edge0, float edge1, float value) {
      float t = (value - edge0) / (edge1 - edge0);
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   private static float lerpf(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static int pack(int rgb, float alpha) {
      int a = Math.max(0, Math.min(255, Math.round(alpha)));
      return a << 24 | (rgb & 0xFFFFFF);
   }

   private static int pack(int red, int green, int blue, int alpha) {
      int a = Math.max(0, Math.min(255, alpha));
      int r = Math.max(0, Math.min(255, red));
      int g = Math.max(0, Math.min(255, green));
      int b = Math.max(0, Math.min(255, blue));
      return a << 24 | r << 16 | g << 8 | b;
   }

   private ShapeRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
