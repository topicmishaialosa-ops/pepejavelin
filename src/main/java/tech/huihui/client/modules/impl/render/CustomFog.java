package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.render.EventFog;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "CustomFog",
   category = Category.RENDER,
   description = "Меняет туман"
)
public class CustomFog extends Module {
   public static final CustomFog INSTANCE = new CustomFog();
   public final NumberSetting distanceSetting = new NumberSetting("Дистанция тумана", 80.0F, 10.0F, 255.0F, 5.0F);

   @EventTarget
   public void onFog(EventFog e) {
      e.setDistance(this.distanceSetting.getCurrent());
      e.setColor(HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB());
      e.setCancelled(true);
   }
}
