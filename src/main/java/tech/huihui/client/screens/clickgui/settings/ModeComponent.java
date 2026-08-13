package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class ModeComponent extends Component {
   private final ModeSetting setting;
   private static final float SPACING = 4.0F;
   private float[] chipWidths;
   private float layoutWidth = -1.0F;

   public ModeComponent(ModeSetting setting) {
      this.setting = setting;
      this.setHeight(22.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private void ensureLayout() {
      if (this.layoutWidth == this.width && this.chipWidths != null) {
         return;
      }
      var values = this.setting.getValues();
      this.chipWidths = new float[values.size()];
      for (int i = 0; i < values.size(); ++i) {
         this.chipWidths[i] = Fonts.REGULAR.getWidth(values.get(i).getName(), 5.5F) + 2.0F;
      }
      this.layoutWidth = this.width;
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 2.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      this.ensureLayout();
      float offset = 0.0F;
      float row = 0.0F;
      var values = this.setting.getValues();
      for (int i = 0; i < values.size(); ++i) {
         float chipWidth = this.chipWidths[i];
         if (offset + chipWidth + SPACING >= this.width - 10.0F) {
            offset = 0.0F;
            row += 10.0F;
         }
         ModeSetting.Value value = values.get(i);
         boolean selected = this.setting.is(value);
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 6.5F + offset, this.y + 9.5F + row, chipWidth + 2.0F, 9.0F, BorderRadius.all(1.0F), selected ? theme.getColor().withAlpha(100.0F * alpha) : (new ColorRGBA(21, 21, 21)).withAlpha(100.0F * alpha));
         draw.drawText(Fonts.REGULAR.getFont(5.5F), value.getName(), this.x + 8.0F + offset, this.y + 11.5F + row, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
         offset += chipWidth + SPACING / 2.0F;
      }
      this.setHeight(22.0F + row);
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (button != 0) {
         return false;
      }
      this.ensureLayout();
      float offset = 0.0F;
      float row = 0.0F;
      var values = this.setting.getValues();
      for (int i = 0; i < values.size(); ++i) {
         float chipWidth = this.chipWidths[i];
         if (offset + chipWidth + SPACING >= this.width - 10.0F) {
            offset = 0.0F;
            row += 10.0F;
         }
         if (MathUtil.isHovered(mouseX, mouseY, this.x + 6.5F + offset, this.y + 9.5F + row, chipWidth + 2.0F, 9.0F)) {
            this.setting.setValue(values.get(i));
            return true;
         }
         offset += chipWidth + SPACING / 2.0F;
      }
      return false;
   }
}
