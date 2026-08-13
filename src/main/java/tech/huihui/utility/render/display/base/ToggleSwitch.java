package tech.huihui.utility.render.display.base;

import tech.huihui.base.animations.base.Easing;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.base.color.ColorUtil;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class ToggleSwitch {
   private ToggleSwitch() {
   }

   public static void render(CustomDrawContext draw, float x, float y, float width, float height, float progress, ColorRGBA accent, ColorRGBA offColor) {
      render(draw, x, y, width, height, progress, accent, offColor, 1.0F);
   }

   public static void render(CustomDrawContext draw, float x, float y, float width, float height, float progress, ColorRGBA accent, ColorRGBA offColor, float alpha) {
      float p = Math.max(0.0F, Math.min(1.0F, progress));
      float eased = (float) Easing.BACK_OUT.ease(p, 0.0F, 1.0F, 1.0F);
      float radius = height / 2.0F;

      ColorRGBA track = ColorUtil.interpolate(offColor, accent, p);
      if (alpha < 1.0F) {
         track = track.withAlpha(track.getAlpha() * alpha);
      }

      DrawUtil.drawShadow(draw.getMatrices(), x, y, width, height, 4.0F, BorderRadius.all(radius), accent.withAlpha((int) (60.0F * p * alpha)));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, height, BorderRadius.all(radius), track);

      float pad = Math.max(1.5F, height * 0.12F);
      float knobSize = height - pad * 2.0F;
      float knobX = x + pad + eased * (width - pad * 2.0F - knobSize);
      float knobY = y + pad;
      float scale = 1.0F - 0.22F * (float) Math.sin(Math.min(p, 1.0F) * 3.141592653589793D);
      float kx = knobX - (knobSize * scale - knobSize) / 2.0F;
      float ky = knobY - (knobSize * scale - knobSize) / 2.0F;
      float ks = knobSize * scale;

      ColorRGBA knob = new ColorRGBA(245, 247, 252);
      if (alpha < 1.0F) {
         knob = knob.withAlpha(knob.getAlpha() * alpha);
      }
      DrawUtil.drawShadow(draw.getMatrices(), kx, ky, ks, ks, 3.0F, BorderRadius.all(ks / 2.0F), new ColorRGBA(0, 0, 0).withAlpha((int) (90.0F * alpha)));
      DrawUtil.drawRoundedRect(draw.getMatrices(), kx, ky, ks, ks, BorderRadius.all(ks / 2.0F), knob);

      if (p > 0.08F) {
         float ix = x + pad + 2.5F;
         float iy = y + pad + 2.0F;
         float is = Math.min(height - pad * 2.0F, 6.5F) * p;
         DrawUtil.drawRoundedRect(draw.getMatrices(), ix, iy, is * 0.55F, is * 0.32F, BorderRadius.all(2.0F), new ColorRGBA(255, 255, 255).withAlpha((int) (170.0F * alpha)));
      }
   }
}