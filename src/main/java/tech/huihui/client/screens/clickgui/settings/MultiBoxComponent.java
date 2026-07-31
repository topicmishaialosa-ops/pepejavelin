package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class MultiBoxComponent extends Component {
   private final MultiBooleanSetting setting;
   private static final float SPACING = 2.0F;

   public MultiBoxComponent(MultiBooleanSetting setting) {
      this.setting = setting;
      this.setHeight(22.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 2.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      float offset = 0.0F;
      float row = 0.0F;
      for (MultiBooleanSetting.Value value : this.setting.getBooleanSettings()) {
         float chipWidth = Fonts.REGULAR.getWidth(value.getName(), 5.5F) + 2.0F;
         if (offset + chipWidth + SPACING >= this.width - 10.0F) {
            offset = 0.0F;
            row += 10.0F;
         }
         boolean enabled = value.isEnabled();
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 6.5F + offset, this.y + 9.5F + row, chipWidth + 2.0F, 9.0F, BorderRadius.all(1.0F), enabled ? theme.getColor().withAlpha(100.0F * alpha) : (new ColorRGBA(21, 21, 21)).withAlpha(100.0F * alpha));
         draw.drawText(Fonts.REGULAR.getFont(5.5F), value.getName(), this.x + 8.0F + offset, this.y + 11.5F + row, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
         offset += chipWidth + SPACING;
      }
      this.setHeight(22.0F + row);
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button != 0) {
         return false;
      }
      float offset = 0.0F;
      float row = 0.0F;
      for (MultiBooleanSetting.Value value : this.setting.getBooleanSettings()) {
         float chipWidth = Fonts.REGULAR.getWidth(value.getName(), 5.5F) + 2.0F;
         if (offset + chipWidth + SPACING >= this.width - 10.0F) {
            offset = 0.0F;
            row += 10.0F;
         }
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 6.5F + offset, this.y + 9.5F + row, chipWidth + 2.0F, 9.0F)) {
            value.toggle();
            return true;
         }
         offset += chipWidth + SPACING;
      }
      return false;
   }
}
