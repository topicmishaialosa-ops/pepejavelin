package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.render.EventCamera;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;

@ModuleAnnotation(
   name = "NoRender",
   category = Category.RENDER,
   description = "Убирает лишние элементы с экрана"
)
public final class NoRender extends Module {
   public static final NoRender INSTANCE = new NoRender();
   private final MultiBooleanSetting settings = MultiBooleanSetting.create("Убрать", List.of("Огонь", "Плохие эффекты", "Камера клип", "Bob"));

   @Native
   public boolean isRemoveFire() {
      return this.isEnabled() && this.settings.isEnable(0);
   }

   @Native
   public boolean isRemoveBadEffect() {
      return this.isEnabled() && this.settings.isEnable(1);
   }

   @Native
   public boolean isRemoveBob() {
      return this.isEnabled() && this.settings.isEnable(3);
   }

   @EventTarget
   @Native
   private void onCamera(EventCamera e) {
      e.setCameraClip(this.settings.isEnable("Камера клип"));
      e.cancel();
   }
}
