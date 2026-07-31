package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class BooleanComponent extends Component {
   private final BooleanSetting setting;
   private final Animation animation = new Animation(200L, Easing.CIRC_OUT);

   public BooleanComponent(BooleanSetting setting) {
      this.setting = setting;
      this.setHeight(16.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      this.animation.update(this.setting.isEnabled());
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 5.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + this.width - 15.0F, this.y + 3.0F, 10.0F, 10.0F, BorderRadius.all(3.0F), (new ColorRGBA(25, 25, 25)).withAlpha(170.0F * alpha));
      draw.drawText(Fonts.ICONS.getFont(6.0F), "S", this.x + this.width - 12.5F, this.y + 4.7F, (new ColorRGBA(153, 153, 153)).withAlpha(125.0F * this.animation.getValue() * alpha));
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 15.0F, this.y + 3.0F, 10.0F, 10.0F)) {
         this.setting.setEnabled(!this.setting.isEnabled());
         return true;
      }
      return false;
   }
}
