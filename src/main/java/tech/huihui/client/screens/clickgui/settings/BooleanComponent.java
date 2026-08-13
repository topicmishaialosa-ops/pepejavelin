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
import tech.huihui.utility.render.display.base.ToggleSwitch;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

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
      float anim = this.animation.getValue();
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 5.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      float trackX = this.x + this.width - 24.0F;
      float trackY = this.y + 3.5F;
      ToggleSwitch.render(draw, trackX, trackY, 18.0F, 10.0F, anim, theme.getColor(), new ColorRGBA(25, 25, 25).withAlpha(170), alpha);
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 24.0F, this.y + 3.5F, 18.0F, 10.0F)) {
         this.setting.setEnabled(!this.setting.isEnabled());
         return true;
      }
      return false;
   }
}
