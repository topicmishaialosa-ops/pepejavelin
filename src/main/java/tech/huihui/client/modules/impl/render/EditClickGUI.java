package tech.huihui.client.modules.impl.render;

import lombok.Generated;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "EditClickGUI",
   category = Category.RENDER,
   description = "Настройки кликгуи"
)
public final class EditClickGUI extends Module {
   public static final EditClickGUI INSTANCE = new EditClickGUI();

   private final ModeSetting mode = new ModeSetting("Режим", "Dropdown1", "Dropdown2");
   private final NumberSetting width = new NumberSetting("Ширина", 120.0F, 80.0F, 220.0F, 1.0F);
   private final NumberSetting height = new NumberSetting("Высота", 300.0F, 100.0F, 500.0F, 1.0F);
   private final NumberSetting opacity = new NumberSetting("Прозрачность", 210.0F, 40.0F, 255.0F, 1.0F);
   private final NumberSetting gap = new NumberSetting("Отступ", 10.0F, 2.0F, 40.0F, 1.0F);
   private final NumberSetting radius = new NumberSetting("Радиус", 7.0F, 0.0F, 12.0F, 1.0F);
   private final NumberSetting scale = new NumberSetting("Масштаб", 1.0F, 0.5F, 2.0F, 0.05F);

   private EditClickGUI() {
   }

   public boolean isDropdown2() {
      return this.mode.is("Dropdown2");
   }

   @Generated
   public ModeSetting getMode() {
      return this.mode;
   }

   public void toggle() {
   }

   public void setToggled(boolean state) {
   }

   @Generated
   public NumberSetting getWidth() {
      return this.width;
   }

   @Generated
   public NumberSetting getHeight() {
      return this.height;
   }

   @Generated
   public NumberSetting getOpacity() {
      return this.opacity;
   }

   @Generated
   public NumberSetting getGap() {
      return this.gap;
   }

   @Generated
   public NumberSetting getRadius() {
      return this.radius;
   }

   @Generated
   public NumberSetting getScale() {
      return this.scale;
   }
}
