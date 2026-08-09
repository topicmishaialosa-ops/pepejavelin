package tech.huihui.client.modules.impl.render;

import lombok.Generated;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "EditClickGUI",
   category = Category.RENDER,
   description = "Настройки кликгуи"
)
public final class EditClickGUI extends Module {
   public static final EditClickGUI INSTANCE = new EditClickGUI();

   private final ModeSetting mode = new ModeSetting("Режим", "Dropdown1", "csgui1", "Dropdown3");
   private final NumberSetting width = new NumberSetting("Ширина", 120.0F, 80.0F, 220.0F, 1.0F);
   private final NumberSetting height = new NumberSetting("Высота", 300.0F, 100.0F, 500.0F, 1.0F);
   private final NumberSetting opacity = new NumberSetting("Прозрачность", 210.0F, 40.0F, 255.0F, 1.0F);
   private final NumberSetting gap = new NumberSetting("Отступ", 10.0F, 2.0F, 40.0F, 1.0F);
   private final NumberSetting radius = new NumberSetting("Радиус", 7.0F, 0.0F, 12.0F, 1.0F);
   private final NumberSetting scale = new NumberSetting("Масштаб", 1.0F, 0.5F, 2.0F, 0.05F);
   private final BooleanSetting blur = new BooleanSetting("Размытие фона", false);
   private final ColorSetting bgColor = new ColorSetting("Цвет фона", new ColorRGBA(15, 15, 15));
   private final BooleanSetting gradientEnabled = new BooleanSetting("Градиент фона", false);
   private final ColorSetting gradientColor = new ColorSetting("Второй цвет", new ColorRGBA(25, 26, 31), () -> {
      return this.gradientEnabled.isEnabled();
   });
   private final ColorSetting borderColor = new ColorSetting("Цвет рамки", new ColorRGBA(21, 21, 21));

   private EditClickGUI() {
   }

   public boolean isCsgui1() {
      return this.mode.is("csgui1");
   }

   public boolean isDropdown3() {
      return this.mode.is("Dropdown3");
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

   @Generated
   public BooleanSetting getBlur() {
      return this.blur;
   }

   @Generated
   public ColorSetting getBgColor() {
      return this.bgColor;
   }

   @Generated
   public BooleanSetting getGradientEnabled() {
      return this.gradientEnabled;
   }

   @Generated
   public ColorSetting getGradientColor() {
      return this.gradientColor;
   }

   @Generated
   public ColorSetting getBorderColor() {
      return this.borderColor;
   }
}
