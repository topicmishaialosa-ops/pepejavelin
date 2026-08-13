package tech.huihui.client.screens.watermark;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

import java.awt.image.BufferedImage;

public final class WatermarkLogo {
   public static final int MIN_SIZE = 4;
   public static final int MAX_SIZE = 128;

   private final int width;
   private final int height;
   private final boolean[][] cells;

   public WatermarkLogo(int width, int height) {
      this.width = Math.max(MIN_SIZE, Math.min(MAX_SIZE, width));
      this.height = Math.max(MIN_SIZE, Math.min(MAX_SIZE, height));
      this.cells = new boolean[this.width][this.height];
   }

   private WatermarkLogo(int width, int height, boolean[][] cells) {
      this.width = width;
      this.height = height;
      this.cells = cells;
   }

   public boolean get(int x, int y) {
      return x >= 0 && x < this.width && y >= 0 && y < this.height && this.cells[x][y];
   }

   public void set(int x, int y, boolean value) {
      if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
         this.cells[x][y] = value;
      }
   }

   public void toggle(int x, int y) {
      if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
         this.cells[x][y] = !this.cells[x][y];
      }
   }

   public void clear() {
      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            this.cells[x][y] = false;
         }
      }
   }

   public boolean isEmpty() {
      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            if (this.cells[x][y]) {
               return false;
            }
         }
      }
      return true;
   }

   public WatermarkLogo resized(int newWidth, int newHeight) {      newWidth = Math.max(MIN_SIZE, Math.min(MAX_SIZE, newWidth));
      newHeight = Math.max(MIN_SIZE, Math.min(MAX_SIZE, newHeight));
      boolean[][] target = new boolean[newWidth][newHeight];
      for (int x = 0; x < Math.min(this.width, newWidth); x++) {
         System.arraycopy(this.cells[x], 0, target[x], 0, Math.min(this.height, newHeight));
      }
      return new WatermarkLogo(newWidth, newHeight, target);
   }

   public void renderFit(MatrixStack matrices, float x, float y, float slotWidth, float slotHeight, ColorRGBA color) {
      float cell = Math.min(slotWidth / (float) this.width, slotHeight / (float) this.height);
      if (cell <= 0.0F) {
         return;
      }
      if (cell >= 1.0F) {
         this.renderCells(matrices, x, y, slotWidth, slotHeight, cell, this.width, this.height, this.cells, color);
         return;
      }
      float targetCell = Math.max(1.5F, cell);
      int tw = Math.max(1, Math.min(this.width, (int) (slotWidth / targetCell)));
      int th = Math.max(1, Math.min(this.height, (int) (slotHeight / targetCell)));
      boolean[][] target = new boolean[tw][th];
      for (int cx = 0; cx < tw; cx++) {
         int startX = cx * this.width / tw;
         int endX = (cx + 1) * this.width / tw;
         for (int cy = 0; cy < th; cy++) {
            int startY = cy * this.height / th;
            int endY = (cy + 1) * this.height / th;
            int lit = 0;
            int total = 0;
            for (int sx = startX; sx < endX; sx++) {
               for (int sy = startY; sy < endY; sy++) {
                  total++;
                  if (this.cells[sx][sy]) {
                     lit++;
                  }
               }
            }
            target[cx][cy] = total > 0 && (float) lit / (float) total >= 0.35F;
         }
      }
      cell = Math.min(slotWidth / (float) tw, slotHeight / (float) th);
      this.renderCells(matrices, x, y, slotWidth, slotHeight, cell, tw, th, target, color);
   }

   private void renderCells(MatrixStack matrices, float x, float y, float slotWidth, float slotHeight, float cell, int gridW, int gridH, boolean[][] grid, ColorRGBA color) {
      float ox = x + (slotWidth - cell * (float) gridW) / 2.0F;
      float oy = y + (slotHeight - cell * (float) gridH) / 2.0F;
      for (int cx = 0; cx < gridW; cx++) {
         for (int cy = 0; cy < gridH; cy++) {
            if (grid[cx][cy]) {
               DrawUtil.drawRoundedRect(matrices, ox + (float) cx * cell, oy + (float) cy * cell, Math.max(cell, 0.5F), Math.max(cell, 0.5F), BorderRadius.all(0.75F), color);
            }
         }
      }
   }

   public static WatermarkLogo fromImage(BufferedImage image, int width, int height) {
      width = Math.max(MIN_SIZE, Math.min(MAX_SIZE, width));
      height = Math.max(MIN_SIZE, Math.min(MAX_SIZE, height));
      WatermarkLogo logo = new WatermarkLogo(width, height);
      for (int cy = 0; cy < height; cy++) {
         int startY = cy * image.getHeight() / height;
         int endY = (cy + 1) * image.getHeight() / height;
         for (int cx = 0; cx < width; cx++) {
            int startX = cx * image.getWidth() / width;
            int endX = (cx + 1) * image.getWidth() / width;
            int lit = 0;
            int total = 0;
            for (int py = startY; py < endY; py++) {
               for (int px = startX; px < endX; px++) {
                  int argb = image.getRGB(px, py);
                  int alpha = argb >>> 24;
                  if (alpha < 80) {
                     continue;
                  }
                  int r = (argb >> 16) & 0xFF;
                  int g = (argb >> 8) & 0xFF;
                  int b = argb & 0xFF;
                  int lum = (r + g + b) / 3;
                  total++;
                  if (lum < 128) {
                     lit++;
                  }
               }
            }
            logo.set(cx, cy, total > 0 && (float) lit / (float) total >= 0.35F);
         }
      }
      return logo;
   }

   public String serialize() {
      StringBuilder builder = new StringBuilder();
      builder.append(this.width).append('x').append(this.height).append(':');
      for (int y = 0; y < this.height; y++) {
         if (y > 0) {
            builder.append(',');
         }
         for (int x = 0; x < this.width; x++) {
            builder.append(this.cells[x][y] ? '1' : '0');
         }
      }
      return builder.toString();
   }

   public static WatermarkLogo deserialize(String data) {
      if (data == null || data.isEmpty()) {
         return null;
      }
      try {
         int colon = data.indexOf(':');
         if (colon <= 0) {
            return null;
         }
         String[] dims = data.substring(0, colon).split("x");
         int width = Integer.parseInt(dims[0]);
         int height = Integer.parseInt(dims[1]);
         if (width < MIN_SIZE || width > MAX_SIZE || height < MIN_SIZE || height > MAX_SIZE) {
            return null;
         }
         WatermarkLogo logo = new WatermarkLogo(width, height);
         String[] rows = data.substring(colon + 1).split(",");
         for (int y = 0; y < height && y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < width && x < row.length(); x++) {
               if (row.charAt(x) == '1') {
                  logo.set(x, y, true);
               }
            }
         }
         return logo;
      } catch (Exception e) {
         return null;
      }
   }

   @Generated
   public int getWidth() {
      return this.width;
   }

   @Generated
   public int getHeight() {
      return this.height;
   }
}
