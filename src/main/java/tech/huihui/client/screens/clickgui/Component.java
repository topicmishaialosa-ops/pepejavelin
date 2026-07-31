package tech.huihui.client.screens.clickgui;

import lombok.Getter;
import lombok.Setter;
import tech.huihui.base.theme.Theme;
import tech.huihui.utility.render.display.base.CustomDrawContext;

@Getter
@Setter
public class Component {
   protected float x;
   protected float y;
   protected float width;
   protected float height;

   public boolean isHovered(float mouseX, float mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public boolean isHovered(float mouseX, float mouseY, float height) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + height;
   }

   public boolean isVisible() {
      return true;
   }

   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
   }

   public boolean mouseClick(float mouseX, float mouseY, int button) {
      return false;
   }

   public void mouseRelease(float mouseX, float mouseY, int button) {
   }

   public boolean keyPressed(int key, int scanCode, int modifiers) {
      return false;
   }

   public boolean charTyped(char codePoint, int modifiers) {
      return false;
   }
}
