package tech.huihui.client.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class ModernTextField implements IClient {
   private final String placeholder;
   private final Font font = Fonts.REGULAR.getFont(6.0F);
   private String text = "";
   private int cursor;
   private boolean focused;
   private float x;
   private float y;
   private float width = 90.0F;
   private float height = 18.0F;

   public ModernTextField(String placeholder) {
      this.placeholder = placeholder;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text == null ? "" : text;
      this.cursor = this.text.length();
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void setFocused(boolean focused) {
      this.focused = focused;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public boolean mouseClicked(double mx, double my, int button) {
      boolean inside = MathUtil.isHovered(mx, my, this.x, this.y, this.width, this.height);
      if (inside) {
         this.focused = true;
         this.cursor = this.countTo((float) mx - this.x - 4.0F);
      } else {
         this.focused = false;
      }
      return inside;
   }

   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused) {
         return false;
      }
      if (this.text.length() >= 16) {
         return true;
      }
      this.text = this.text.substring(0, this.cursor) + chr + this.text.substring(this.cursor);
      this.cursor++;
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.focused) {
         return false;
      }
      switch (keyCode) {
         case 259:
            if (this.cursor > 0) {
               this.text = this.text.substring(0, this.cursor - 1) + this.text.substring(this.cursor);
               this.cursor--;
            }
            return true;
         case 261:
            if (this.cursor < this.text.length()) {
               this.text = this.text.substring(0, this.cursor) + this.text.substring(this.cursor + 1);
            }
            return true;
         case 263:
            if (this.cursor > 0) {
               this.cursor--;
            }
            return true;
         case 262:
            if (this.cursor < this.text.length()) {
               this.cursor++;
            }
            return true;
         case 268:
            this.cursor = 0;
            return true;
         case 269:
            this.cursor = this.text.length();
            return true;
         default:
            return false;
      }
   }

   private int countTo(float localX) {
      int best = 0;
      float bestDist = Float.MAX_VALUE;
      for (int i = 0; i <= this.text.length(); i++) {
         float dist = Math.abs(this.font.width(this.text.substring(0, i)) - localX);
         if (dist < bestDist) {
            bestDist = dist;
            best = i;
         }
      }
      return best;
   }

   public void render(DrawContext context, int mx, int my, float open) {
      CustomDrawContext draw = CustomDrawContext.of(context);
      MatrixStack matrices = draw.getMatrices();
      boolean hovered = MathUtil.isHovered(mx, my, this.x, this.y, this.width, this.height);
      DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(5.0F), new ColorRGBA(255, 255, 255).withAlpha((hovered ? 10 : 6) * open));
      ColorRGBA border = this.focused ? HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor().withAlpha(60.0F * open) : new ColorRGBA(255, 255, 255).withAlpha(26.0F * open);
      DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 1.0F, BorderRadius.all(5.0F), border);

      float maxW = this.width - 8.0F;
      String shown = this.text;
      int start = 0;
      while (start < shown.length() && this.font.width(shown.substring(start)) > maxW) {
         start++;
      }
      String visible = shown.substring(start);
      float textY = this.y + ((this.height - this.font.height()) / 2.0F);
      if (visible.isEmpty()) {
         draw.drawText(this.font, this.placeholder, this.x + 4.0F, textY, new ColorRGBA(150, 150, 165).withAlpha(255.0F * open));
      } else {
         draw.drawText(this.font, visible, this.x + 4.0F, textY, new ColorRGBA(255, 255, 255, (int) (240.0F * open)));
      }
      if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         int caretIndex = Math.max(start, Math.min(this.cursor, shown.length()));
         float caretX = this.x + 4.0F + this.font.width(shown.substring(start, caretIndex));
         DrawUtil.drawLine(matrices, new Vec2f(caretX, this.y + 3.0F), new Vec2f(caretX, this.y + this.height - 3.0F), new ColorRGBA(255, 255, 255, (int) (220.0F * open)));
      }
   }
}