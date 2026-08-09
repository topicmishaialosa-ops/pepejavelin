package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class BindComponent extends Component {
   private final KeySetting setting;
   private boolean activated;

   public BindComponent(KeySetting setting) {
      this.setting = setting;
      this.setHeight(16.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 4.5F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      String bind = this.setting.getKeyCode() == -1 ? "Нету" : this.setting.getNameKey();
      if (this.activated) {
         bind = "...";
      }
      float bindWidth = Fonts.REGULAR.getWidth(bind, 5.5F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + this.width - 4.0F - bindWidth - 4.0F, this.y + 3.5F, bindWidth + 4.0F, 9.0F, BorderRadius.all(2.0F), theme.getColor().withAlpha(this.activated ? 150.0F * alpha : 100.0F * alpha));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), bind, this.x + this.width - 4.0F - bindWidth - 2.0F, this.y + 5.5F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      if (this.activated) {
         if (key == 256 || key == 261) {
            this.setting.setKeyCode(-1);
         } else {
            this.setting.setKeyCode(key);
         }
         this.activated = false;
         return true;
      }
      return false;
   }

    @Override
    public boolean mouseClick(float mouseX, float mouseY, int button) {
       if (this.activated) {
          this.setting.setKeyCode(button);
          this.activated = false;
          return true;
       }
       if (this.isHovered(mouseX, mouseY) && button == 0) {
          this.activated = !this.activated;
          return true;
       }
       return false;
    }
}
