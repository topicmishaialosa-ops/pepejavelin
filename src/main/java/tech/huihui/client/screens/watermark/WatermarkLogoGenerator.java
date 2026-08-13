package tech.huihui.client.screens.watermark;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WatermarkLogoGenerator {

   private static final int SAMPLE = 8;

   private record FontSpec(String family, int style, float threshold) {
   }

   private static final FontSpec[] SPECS = {
      new FontSpec(Font.SANS_SERIF, Font.BOLD, 0.40F),
      new FontSpec(Font.SANS_SERIF, Font.BOLD, 0.55F),
      new FontSpec(Font.SANS_SERIF, Font.PLAIN, 0.45F),
      new FontSpec(Font.SERIF, Font.BOLD, 0.40F),
      new FontSpec(Font.SERIF, Font.PLAIN, 0.50F),
      new FontSpec(Font.MONOSPACED, Font.BOLD, 0.40F),
      new FontSpec(Font.MONOSPACED, Font.PLAIN, 0.55F),
      new FontSpec(Font.DIALOG, Font.BOLD, 0.45F),
      new FontSpec(Font.DIALOG, Font.ITALIC, 0.40F),
      new FontSpec(Font.SANS_SERIF, Font.ITALIC, 0.50F)
   };

   private WatermarkLogoGenerator() {
   }

   public static List<WatermarkLogo> generate(String text, int width, int height) {
      List<WatermarkLogo> variants = new ArrayList<>();
      if (text == null || text.isEmpty()) {
         return variants;
      }
      String best = text.trim();
      if (best.isEmpty()) {
         return variants;
      }
      Set<String> seen = new HashSet<>();
      for (FontSpec spec : SPECS) {
         if (variants.size() >= 12) {
            break;
         }
         boolean[][] grid = rasterize(best, width, height, spec.family(), spec.style(), spec.threshold());
         WatermarkLogo logo = new WatermarkLogo(width, height);
         for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
               logo.set(x, y, grid[x][y]);
            }
         }
         if (logo.isEmpty()) {
            continue;
         }
         String key = logo.serialize();
         if (seen.add(key)) {
            variants.add(logo);
         }
      }
      return variants;
   }

   private static boolean[][] rasterize(String text, int width, int height, String family, int style, float threshold) {
      int sampleW = width * SAMPLE;
      int sampleH = height * SAMPLE;
      BufferedImage image = new BufferedImage(sampleW, sampleH, 2);
      Graphics2D g = image.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, sampleW, sampleH);

      int fontSize = (int) (sampleH * 1.5F);
      Font font = new Font(family, style, fontSize);
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics(font);
      int textWidth = metrics.stringWidth(text);
      int ascent = metrics.getAscent();
      int descent = metrics.getDescent();

      float fontScale = Math.min((float) sampleW / (float) Math.max(textWidth, 1), (float) (sampleH * 0.8F) / (float) Math.max(ascent + descent, 1));
      fontScale = Math.min(fontScale, 1.6F);
      Font scaled = font.deriveFont((float) fontSize * fontScale);
      metrics = g.getFontMetrics(scaled);
      textWidth = metrics.stringWidth(text);
      ascent = metrics.getAscent();
      descent = metrics.getDescent();

      int x = (sampleW - textWidth) / 2;
      int y = (sampleH - (ascent + descent)) / 2 + ascent;
      g.setColor(Color.WHITE);
      g.drawString(text, x, y);
      g.dispose();

      boolean[][] grid = new boolean[width][height];
      for (int cx = 0; cx < width; cx++) {
         int startX = cx * SAMPLE;
         int endX = startX + SAMPLE;
         for (int cy = 0; cy < height; cy++) {
            int startY = cy * SAMPLE;
            int endY = startY + SAMPLE;
            int lit = 0;
            for (int px = startX; px < endX; px++) {
               for (int py = startY; py < endY; py++) {
                  if (px < sampleW && py < sampleH && (image.getRGB(px, py) & 0xFFFFFF) != 0) {
                     lit++;
                  }
               }
            }
            grid[cx][cy] = (float) lit / (float) (SAMPLE * SAMPLE) >= threshold;
         }
      }
      return grid;
   }
}