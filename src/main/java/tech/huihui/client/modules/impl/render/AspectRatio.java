package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.render.EventAspectRatio;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "AspectRatio",
   category = Category.RENDER,
   description = "Меняет соотношение сторон изображения"
)
public final class AspectRatio extends Module {
   public static final AspectRatio INSTANCE = new AspectRatio();
   private final ModeSetting mode = new ModeSetting("Пресет", "Авто", "16:9", "4:3", "21:9", "Квадрат", "Кастом");
   private final NumberSetting custom = new NumberSetting("Ратио", 1.0F, 0.5F, 2.5F, 0.01F);

   @EventTarget
   private void onAspect(EventAspectRatio event) {
      event.setRatio(this.getRatio());
      event.setCancelled(true);
   }

   private float getRatio() {
      switch (this.mode.get()) {
         case "16:9":
            return 16.0F / 9.0F;
         case "4:3":
            return 4.0F / 3.0F;
         case "21:9":
            return 21.0F / 9.0F;
         case "Квадрат":
            return 1.0F;
         case "Кастом":
            return this.custom.getCurrent();
         default:
            return (float) mc.getWindow().getFramebufferWidth() / (float) mc.getWindow().getFramebufferHeight();
      }
   }
}