package tech.huihui.client.screens.clickgui.settings;

import lombok.Getter;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.screens.clickgui.Component;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class StringComponent extends Component implements IMinecraft {
   private final StringSetting setting;
   private TextFieldWidget field;
   private boolean initialized;

   public StringComponent(StringSetting setting) {
      this.setting = setting;
      this.setHeight(18.0F);
   }

   private TextFieldWidget field() {
      if (!this.initialized && mc.textRenderer != null) {
         this.field = new TextFieldWidget(mc.textRenderer, 0, 0, 90, 16, Text.empty());
         this.field.setMaxLength(40);
         this.field.setText(this.setting.getValue());
         this.initialized = true;
      }
      return this.field;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.setting.getName(), this.x + 7.0F, this.y + 6.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      TextFieldWidget field = this.field();
      if (field == null) {
         return;
      }
      float fieldX = this.x + this.width / 2.0F - 45.0F;
      float fieldY = this.y + 2.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), fieldX - 1.0F, fieldY - 1.0F, field.getWidth() + 2.0F, field.getHeight() + 2.0F, BorderRadius.all(3.0F), (new ColorRGBA(30, 30, 34)).withAlpha(200.0F * alpha));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), fieldX - 1.0F, fieldY - 1.0F, field.getWidth() + 2.0F, field.getHeight() + 2.0F, 1.0F, BorderRadius.all(3.0F), field.isFocused() ? theme.getColor().withAlpha(255.0F * alpha) : (new ColorRGBA(120, 120, 126)).withAlpha(90.0F * alpha));
      field.setX((int)fieldX);
      field.setY((int)fieldY);
      field.render(draw, (int)mouseX, (int)mouseY, 0.0F);
   }

   @Override
   public boolean mouseClick(float mouseX, float mouseY, int button) {
      TextFieldWidget field = this.field();
      if (field == null) {
         return false;
      }
      float fieldX = this.x + this.width / 2.0F - 45.0F;
      float fieldY = this.y + 2.0F;
      boolean inside = mouseX >= fieldX && mouseX <= fieldX + field.getWidth() && mouseY >= fieldY && mouseY <= fieldY + field.getHeight();
      field.setFocused(inside);
      if (inside && button == 0) {
         field.mouseClicked(mouseX, mouseY, button);
         return true;
      }
      return false;
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      TextFieldWidget field = this.field();
      if (field == null || !field.isFocused()) {
         return false;
      }
      field.keyPressed(key, scanCode, modifiers);
      this.setting.setValue(field.getText());
      return true;
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      TextFieldWidget field = this.field();
      if (field == null || !field.isFocused()) {
         return false;
      }
      field.charTyped(codePoint, modifiers);
      this.setting.setValue(field.getText());
      return true;
   }
}
