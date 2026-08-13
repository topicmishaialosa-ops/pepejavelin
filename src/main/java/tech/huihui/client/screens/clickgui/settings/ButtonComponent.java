package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class ButtonComponent extends Component {
   private final ButtonSetting setting;
   private float nameWidth = -1.0F;

   public ButtonComponent(ButtonSetting setting) {
      this.setting = setting;
      this.setHeight(16.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      if (this.nameWidth < 0.0F) {
         this.nameWidth = Fonts.REGULAR.getWidth(this.setting.getName(), 5.5F);
      }
      boolean hovered = this.isHovered(mouseX, mouseY);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 4.5F, this.y + 2.0F, this.width - 9.0F, 12.0F, BorderRadius.all(2.0F), hovered ? theme.getColor().withAlpha(110.0F * alpha) : (new ColorRGBA(40, 40, 40)).withAlpha(255.0F * alpha));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + this.width / 2.0F - this.nameWidth / 2.0F, this.y + 5.0F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 0 && this.isHovered(mouseX, mouseY)) {
         this.setting.toggle();
         return true;
      }
      return false;
   }
}
