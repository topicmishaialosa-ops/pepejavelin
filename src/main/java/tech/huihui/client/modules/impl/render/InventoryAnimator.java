package tech.huihui.client.modules.impl.render;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "InventoryAnimator",
   category = Category.RENDER,
   description = "Анимация предметов в инвентаре"
)
public final class InventoryAnimator extends Module {
   public static final InventoryAnimator INSTANCE = new InventoryAnimator();
   private final ModeSetting mode = new ModeSetting("Режим", "Оба", "Слайд", "Масштаб");
   private final NumberSetting duration = new NumberSetting("Длительность (мс)", 350.0F, 100.0F, 1500.0F, 25.0F);
   private final NumberSetting stagger = new NumberSetting("Задержка на слот (мс)", 8.0F, 0.0F, 50.0F, 1.0F);
   private Object lastScreen;
   private long openTime;

   private InventoryAnimator() {
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.lastScreen = null;
   }

   public void onSlotDraw(Object screen, long now) {
      if (this.lastScreen != screen) {
         this.lastScreen = screen;
         this.openTime = now;
      }
   }

   public float getProgress(int slotId, long now) {
      long elapsed = now - this.openTime - (long)slotId * (long)this.stagger.getCurrent();
      if (elapsed <= 0L) {
         return 0.0F;
      }
      float p = (float)elapsed / this.duration.getCurrent();
      if (p > 1.0F) {
         return 1.0F;
      }
      return p;
   }

   public boolean isSlide() {
      return this.mode.is("Слайд") || this.mode.is("Оба");
   }

   public boolean isScale() {
      return this.mode.is("Масштаб") || this.mode.is("Оба");
   }
}
