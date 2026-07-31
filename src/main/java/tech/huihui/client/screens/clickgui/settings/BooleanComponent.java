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
import tech.huihui.utility.render.display.base.color.ColorUtil;
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
      float trackX = this.x + this.width - 21.0F;
      float trackY = this.y + 3.5F;
      ColorRGBA trackColor = ColorUtil.interpolate((new ColorRGBA(25, 25, 25)).withAlpha(170.0F * alpha), theme.getColor().withAlpha(210.0F * alpha), this.animation.getValue());
      DrawUtil.drawRoundedRect(draw.getMatrices(), trackX, trackY, 16.0F, 10.0F, BorderRadius.all(5.0F), trackColor);
      float knobX = trackX + 1.5F + this.animation.getValue() * 7.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX, trackY + 1.5F, 7.0F, 7.0F, BorderRadius.all(3.5F), (new ColorRGBA(240, 240, 240)).withAlpha(255.0F * alpha));
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 21.0F, this.y + 3.5F, 16.0F, 10.0F)) {
         this.setting.setEnabled(!this.setting.isEnabled());
         return true;
      }
      return false;
   }
}
