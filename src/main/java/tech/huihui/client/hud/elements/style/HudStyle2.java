package tech.huihui.client.hud.elements.style;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class HudStyle2 {
   public static final ColorRGBA CARD_BG = new ColorRGBA(18, 18, 26, 220);
   public static final ColorRGBA CARD_BORDER = new ColorRGBA(40, 40, 55, 180);
   public static final ColorRGBA TEXT_PRIMARY = new ColorRGBA(230, 230, 245, 255);
   public static final ColorRGBA TEXT_SECONDARY = new ColorRGBA(140, 140, 165, 200);
   public static final ColorRGBA TEXT_DIM = new ColorRGBA(90, 90, 115, 180);
   public static final ColorRGBA DIVIDER = new ColorRGBA(50, 50, 65, 120);
   public static final ColorRGBA ACCENT = new ColorRGBA(120, 140, 255, 200);
   public static final ColorRGBA GREEN = new ColorRGBA(100, 220, 140, 200);
   public static final ColorRGBA ORANGE = new ColorRGBA(255, 180, 100, 200);
   public static final ColorRGBA RED = new ColorRGBA(255, 100, 100, 200);
   public static final BorderRadius RADIUS = BorderRadius.all(8.0F);
   public static final BorderRadius RADIUS_SM = BorderRadius.all(4.0F);

   private HudStyle2() {
   }

   public static void drawCard(CustomDrawContext ctx, float x, float y, float w, float h, float alpha) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, RADIUS,
         CARD_BG.withAlpha(alpha));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, RADIUS,
         CARD_BORDER.withAlpha(alpha));
   }

   public static void drawCardNoBorder(CustomDrawContext ctx, float x, float y, float w, float h, float alpha) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, RADIUS,
         CARD_BG.withAlpha(alpha));
   }

   public static void drawHeader(CustomDrawContext ctx, String icon, String title, float x, float y, float w, float alpha, ColorRGBA accentColor) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, 18.0F,
         new BorderRadius(8.0F, 8.0F, 0.0F, 0.0F), CARD_BG.withAlpha(alpha));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, 18.0F, 1.0F,
         new BorderRadius(8.0F, 8.0F, 0.0F, 0.0F), CARD_BORDER.withAlpha(alpha));
      ctx.drawText(Fonts.ICONS2.getFont(7.0F), icon, x + 6.0F, y + 5.0F,
         accentColor.withAlpha(alpha));
      ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), title, x + 16.0F, y + 5.0F,
         TEXT_PRIMARY.withAlpha(alpha));
   }

   public static void drawItem(CustomDrawContext ctx, String icon, String text, String value,
                                float x, float y, float w, float h, float alpha, ColorRGBA accentColor) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, RADIUS_SM,
         CARD_BG.withAlpha((int)(alpha * 0.8F)));
      ctx.drawText(Fonts.ICONS2.getFont(5.5F), icon, x + 5.0F, y + (h - 5.5F) / 2.0F,
         accentColor.withAlpha(alpha));
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), text, x + 14.0F, y + (h - 6.5F) / 2.0F,
         TEXT_PRIMARY.withAlpha(alpha));
      if (value != null) {
         float valueWidth = Fonts.REGULAR.getWidth(value, 6.5F);
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), value, x + w - valueWidth - 5.0F, y + (h - 6.5F) / 2.0F,
            TEXT_SECONDARY.withAlpha(alpha));
      }
   }

   public static void drawItemWithDivider(CustomDrawContext ctx, String icon, String text, String value,
                                           float x, float y, float w, float h, float alpha, ColorRGBA accentColor) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, RADIUS_SM,
         CARD_BG.withAlpha((int)(alpha * 0.8F)));
      ctx.drawText(Fonts.ICONS2.getFont(5.5F), icon, x + 5.0F, y + (h - 5.5F) / 2.0F,
         accentColor.withAlpha(alpha));
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), text, x + 14.0F, y + (h - 6.5F) / 2.0F,
         TEXT_PRIMARY.withAlpha(alpha));
      if (value != null) {
         float divX = x + 14.0F + Fonts.REGULAR.getWidth(text, 6.5F) + 6.0F;
         DrawUtil.drawRect(ctx.getMatrices(), divX, y + 3.0F, 1.0F, h - 6.0F,
            DIVIDER.withAlpha(alpha));
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), value, divX + 6.0F, y + (h - 6.5F) / 2.0F,
            TEXT_SECONDARY.withAlpha(alpha));
      }
   }

   public static void drawKeyValue(CustomDrawContext ctx, String key, String value,
                                    float x, float y, float alpha) {
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), key, x, y,
         TEXT_DIM.withAlpha(alpha));
      float keyWidth = Fonts.REGULAR.getWidth(key, 6.0F);
      ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), value, x + keyWidth + 4.0F, y - 0.5F,
         TEXT_PRIMARY.withAlpha(alpha));
   }

   public static float calculateItemWidth(String icon, String text, String value) {
      float w = 24.0F + Fonts.REGULAR.getWidth(text, 6.5F) + 10.0F;
      if (value != null) {
         w += 8.0F + Fonts.REGULAR.getWidth(value, 6.5F);
      }
      return Math.max(w, 60.0F);
   }
}
