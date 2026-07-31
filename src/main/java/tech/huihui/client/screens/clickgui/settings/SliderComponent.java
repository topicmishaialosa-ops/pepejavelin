package tech.huihui.client.screens.clickgui.settings;

import java.util.Locale;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class SliderComponent extends Component {
   private final NumberSetting setting;
   private float anim;
   private boolean dragging;

   public SliderComponent(NumberSetting setting) {
      this.setting = setting;
      this.setHeight(18.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      String value = this.formatValue();
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 5.0F, this.y + 3.5F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), value, this.x + this.width - 6.0F - Fonts.REGULAR.getWidth(value, 5.5F), this.y + 3.5F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));

      float trackWidth = this.width - 10.0F;
      float target = trackWidth * (this.setting.getCurrent() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin());
      this.anim += (target - this.anim) * 0.2F;
      if (this.dragging) {
         float percent = MathHelper.clamp((mouseX - this.x - 5.0F) / trackWidth, 0.0F, 1.0F);
         float valueNew = this.setting.getMin() + percent * (this.setting.getMax() - this.setting.getMin());
         float rounded = (float) MathUtil.round(valueNew, this.setting.getIncrement());
         this.setting.setCurrent(MathHelper.clamp(rounded, this.setting.getMin(), this.setting.getMax()));
      }

      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 5.0F, this.y + 12.0F, trackWidth, 2.0F, BorderRadius.all(1.0F), (new ColorRGBA(55, 55, 55)).withAlpha(100.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 5.0F, this.y + 12.0F, this.anim, 2.0F, BorderRadius.all(1.0F), (new ColorRGBA(123, 123, 123)).withAlpha(200.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 5.0F + this.anim - 4.0F, this.y + 9.5F, 8.0F, 8.0F, BorderRadius.all(4.0F), (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
   }

   private String formatValue() {
      float increment = this.setting.getIncrement();
      int decimals = Math.max(0, String.valueOf(increment).contains(".") ? String.valueOf(increment).split("\\.")[1].length() : 0);
      String format = "%." + decimals + "f";
      return String.format(Locale.US, format, this.setting.getCurrent()).replaceAll("\\.?0+$", "");
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x + 5.0F, this.y + 10.0F, this.width - 10.0F, 3.0F)) {
         this.dragging = true;
         return true;
      }
      return false;
   }

   @Override
   public void mouseRelease(float mouseX, float mouseY, int button) {
      this.dragging = false;
   }
}
