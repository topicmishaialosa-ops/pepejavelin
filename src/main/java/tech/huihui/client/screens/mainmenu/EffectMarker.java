package tech.huihui.client.screens.mainmenu;

import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class EffectMarker {
   private EffectMarker() {
   }

   public static void spawn(List<Marker> list, float x, float y) {
      if (list != null) {
         list.add(new Marker(x, y));
      }
   }

   public static void renderAll(MatrixStack matrices, float partialTicks, List<Marker> list) {
      if (list == null || list.isEmpty()) {
         return;
      }
      for (int i = list.size() - 1; i >= 0; i--) {
         if (list.get(i).update(matrices, partialTicks)) {
            list.remove(i);
         }
      }
   }

   public static final class Marker {
      private final float delay = System.nanoTime() + 300000000L;
      private final float x;
      private final float y;
      private boolean expired;
      private float value = 0.0F;
      private float prevValue = 0.0F;
      private long lastUpdate = System.currentTimeMillis();

      Marker(float x, float y) {
         this.x = x;
         this.y = y;
      }

      boolean update(MatrixStack matrices, float partialTicks) {
         prevValue = this.value;
         float direction = this.expired ? -1.0F : 1.0F;
         float delta = (float) (System.currentTimeMillis() - this.lastUpdate) / 1000.0F;
         this.value = clamp(this.value + (direction * 0.2F * 20.0F * delta));
         this.lastUpdate = System.currentTimeMillis();
         if (!this.expired && System.nanoTime() >= this.delay) {
            this.expired = true;
         }
         float progress = clamp(tech.huihui.utility.math.MathUtil.interpolate(this.prevValue, this.value, partialTicks));
         float scale = this.expired ? progress : easeScale(progress);
         float length = 4.0F * Math.max(1.0E-4F, scale);
         ColorRGBA color = new ColorRGBA(255, 255, 255, Math.round(250.0F * progress));
         matrices.push();
         matrices.translate(this.x, this.y, 0.0F);
         for (int i = 0; i < 4; i++) {
            drawMarker(matrices, length, 45.0F + (90.0F * i), length, color);
         }
         matrices.pop();
         return this.expired && progress <= 0.01F;
      }

      private void drawMarker(MatrixStack matrices, float length, float angleDeg, float offset, ColorRGBA color) {
         matrices.push();
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angleDeg));
         matrices.translate(offset, 0.0F, 0.0F);
         DrawUtil.drawRoundedRect(matrices, -length / 2.0F, -0.25F, length, 0.5F, BorderRadius.ZERO, color);
         matrices.pop();
      }

      private float easeScale(float scale) {
         if (scale <= 0.0F) {
            return 0.0F;
         }
         if (scale < 0.6F) {
            return scale / 0.6F;
         }
         if (scale < 0.8F) {
            return 1.0F + ((scale - 0.6F) / 0.5F) * 0.5F;
         }
         return 1.2F - ((scale - 0.8F) / 0.2F) * 0.2F;
      }

      private float clamp(float value) {
         return Math.max(0.0F, Math.min(1.0F, value));
      }
   }
}