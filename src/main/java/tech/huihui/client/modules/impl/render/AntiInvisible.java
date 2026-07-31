package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import lombok.Generated;
import tech.huihui.base.events.impl.entity.EventEntityColor;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "Anti Invisible",
   category = Category.RENDER,
   description = "Видно инвизок"
)
public final class AntiInvisible extends Module {
   public static final AntiInvisible INSTANCE = new AntiInvisible();
   private final ColorSetting colorSetting;

   private AntiInvisible() {
      this.colorSetting = new ColorSetting("Цвет", ColorRGBA.WHITE.mulAlpha(0.5F));
   }

   @EventTarget
   public void onEntityColor(EventEntityColor e) {
      e.setColor(this.colorSetting.getColor().getRGB());
      e.cancel();
   }

   @Generated
   public ColorSetting getColorSetting() {
      return this.colorSetting;
   }
}
