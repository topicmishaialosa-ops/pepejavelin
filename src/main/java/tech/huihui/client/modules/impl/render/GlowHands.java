package tech.huihui.client.modules.impl.render;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "GlowHands",
   category = Category.RENDER,
   description = "Прозрачные светящиеся руки"
)
public final class GlowHands extends Module {
   public static final GlowHands INSTANCE = new GlowHands();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Свечение и прозрачность", "Свечение", "Прозрачность"});
   private final ColorSetting color = new ColorSetting("Цвет", new ColorRGBA(0, 200, 255, 255));
   private final NumberSetting transparency = new NumberSetting("Прозрачность", 150.0F, 0.0F, 255.0F, 5.0F);
   private final NumberSetting glowIntensity = new NumberSetting("Свечение", 220.0F, 0.0F, 255.0F, 5.0F);
   private final NumberSetting glowSize = new NumberSetting("Размер свечения", 1.1F, 1.0F, 1.5F, 0.01F);
   private final BooleanSetting rainbow = new BooleanSetting("Радуга", false);
   private final NumberSetting rainbowSpeed = new NumberSetting("Скорость радуги", 2.0F, 0.1F, 10.0F, 0.1F);

   private GlowHands() {
   }

   public boolean isGlowMode() {
      return this.mode.is("Свечение") || this.mode.is("Свечение и прозрачность");
   }

   public boolean isTransparencyMode() {
      return this.mode.is("Прозрачность") || this.mode.is("Свечение и прозрачность");
   }

   public float getTransparency() {
      return this.transparency.getCurrent();
   }

   public float getGlowIntensity() {
      return this.glowIntensity.getCurrent();
   }

   public float getGlowSize() {
      return this.glowSize.getCurrent();
   }

   public ColorRGBA getCurrentColor() {
      if (this.rainbow.isEnabled()) {
         float speed = Math.max(0.1F, this.rainbowSpeed.getCurrent());
         float hue = (float)(System.currentTimeMillis() % (long)((int)(2000.0F / speed))) / (2000.0F / speed);
         ColorRGBA hsb = ColorRGBA.fromHSB(hue, 0.9F, 1.0F);
         return new ColorRGBA(hsb.getRed(), hsb.getGreen(), hsb.getBlue(), 255);
      } else {
         return this.color.getColor();
      }
   }
}
