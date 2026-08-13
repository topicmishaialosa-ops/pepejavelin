package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class ColorComponent extends Component {
   private final ColorSetting setting;
   private final float[] hsb = new float[3];
   private boolean panelOpened;
   private boolean draggingHue;
   private boolean draggingPicker;
   private static final float PICKER_HEIGHT = 60.0F;

   public ColorComponent(ColorSetting setting) {
      this.setting = setting;
      this.initHsb();
      this.setHeight(18.0F);
   }

   private void initHsb() {
      ColorRGBA color = this.setting.getColor();
      this.hsb[0] = color.getHue();
      this.hsb[1] = color.getSaturation();
      this.hsb[2] = color.getBrightness();
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float pickerX() {
      return this.x + 5.0F;
   }

   private float pickerY() {
      return this.y + 17.0F;
   }

   private float pickerWidth() {
      return this.width - 20.0F;
   }

   private float sliderX() {
      return this.pickerX() + this.pickerWidth() + 5.0F;
   }

   private float sliderY() {
      return this.pickerY();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      draw.drawText(Fonts.REGULAR.getFont(6.0F), this.setting.getName(), this.x + 7.0F, this.y + 4.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + this.width - 24.0F, this.y + 4.0F, 16.0F, 10.0F, BorderRadius.all(3.5F), this.setting.getColor().withAlpha(255.0F * alpha));
      if (this.panelOpened) {
         this.renderPicker(draw, theme, mouseX, mouseY, alpha);
         this.setHeight(18.0F - 4.0F + PICKER_HEIGHT + 5.0F);
      } else {
         this.setHeight(18.0F);
      }
   }

   private void renderPicker(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      if (this.draggingPicker) {
         float saturation = MathHelper.clamp((mouseX - this.pickerX() - 4.0F) / (this.pickerWidth() - 8.0F), 0.0F, 1.0F);
         float brightness = 1.0F - MathHelper.clamp((mouseY - this.pickerY() - 4.0F) / (PICKER_HEIGHT - 8.0F), 0.0F, 1.0F);
         this.hsb[1] = saturation;
         this.hsb[2] = brightness;
         this.applyColor();
      }
      if (this.draggingHue) {
         this.hsb[0] = MathHelper.clamp((mouseY - this.sliderY()) / PICKER_HEIGHT, 0.0F, 1.0F);
         this.applyColor();
      }

      ColorRGBA hue = ColorRGBA.fromHSB(this.hsb[0], 1.0F, 1.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.pickerX(), this.pickerY(), this.pickerWidth(), PICKER_HEIGHT, BorderRadius.all(6.0F), ColorRGBA.WHITE, ColorRGBA.BLACK, ColorRGBA.BLACK, hue);

      float knobX = this.pickerX() + 4.0F + this.hsb[1] * (this.pickerWidth() - 8.0F);
      float knobY = this.pickerY() + 4.0F + (1.0F - this.hsb[2]) * (PICKER_HEIGHT - 8.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));

      DrawUtil.drawHueBar(draw.getMatrices(), this.sliderX(), this.sliderY(), 3.0F, PICKER_HEIGHT, 255.0F * alpha);

      float knobY2 = this.sliderY() + this.hsb[0] * PICKER_HEIGHT;
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.sliderX() - 2.5F, knobY2 - 4.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.sliderX() - 1.5F, knobY2 - 3.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
   }

   private void applyColor() {
      this.setting.setColor(ColorRGBA.fromHSB(this.hsb[0], this.hsb[1], this.hsb[2]));
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button == 1 && MathUtil.isHovered(mouseX, mouseY, this.x + this.width - 24.0F, this.y + 4.0F, 16.0F, 10.0F)) {
         this.panelOpened = !this.panelOpened;
         if (this.panelOpened) {
            this.initHsb();
         }
         return true;
      }
      if (this.panelOpened && button == 0) {
         if (MathUtil.isHovered(mouseX, mouseY, this.sliderX() - 2.0F, this.sliderY(), 7.0F, PICKER_HEIGHT)) {
            this.draggingHue = true;
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, this.pickerX(), this.pickerY(), this.pickerWidth(), PICKER_HEIGHT)) {
            this.draggingPicker = true;
            return true;
         }
      }
      return false;
   }

   @Override
   public void mouseRelease(float mouseX, float mouseY, int button) {
      this.draggingHue = false;
      this.draggingPicker = false;
   }
}
