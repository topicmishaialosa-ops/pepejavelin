package tech.huihui.client.modules.impl.render;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "Optimization",
   category = Category.RENDER,
   description = "Оптимизация и повышение FPS"
)
public final class Optimization extends Module {
   public static final Optimization INSTANCE = new Optimization();
   private final BooleanSetting particles = new BooleanSetting("Отключить частицы", true);
   private final BooleanSetting weather = new BooleanSetting("Убрать погоду", true);
   private final BooleanSetting clouds = new BooleanSetting("Убрать облака", true);
   private final BooleanSetting shadows = new BooleanSetting("Убрать тени", true);
   private final NumberSetting entityDistance = new NumberSetting("Дистанция энтити", 64.0F, 16.0F, 256.0F, 8.0F);

   private Optimization() {
   }

   public boolean isParticlesEnabled() {
      return this.isEnabled() && this.particles.isEnabled();
   }

   public boolean isWeatherEnabled() {
      return this.isEnabled() && this.weather.isEnabled();
   }

   public boolean isCloudsEnabled() {
      return this.isEnabled() && this.clouds.isEnabled();
   }

   public boolean isShadowsEnabled() {
      return this.isEnabled() && this.shadows.isEnabled();
   }

   public float getEntityDistance() {
      return this.entityDistance.getCurrent();
   }
}
