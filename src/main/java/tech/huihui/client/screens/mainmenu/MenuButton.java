package tech.huihui.client.screens.mainmenu;

import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class MenuButton {
   private final String text;
   private final Runnable action;
   private final Animation hoverAnimation = new Animation(200L, Easing.EXPO_OUT);
   private float x;
   private float y;
   private float width;
   private float height;
   private boolean hovered;

   public MenuButton(String text, Runnable action) {
      this.text = text;
      this.action = action;
   }

   public void set(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public void update(float mouseX, float mouseY) {
      this.hovered = MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
      this.hoverAnimation.animateTo(this.hovered ? 1.0F : 0.0F);
      this.hoverAnimation.update();
   }

   public boolean isHovered(float mouseX, float mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public void click() {
      this.action.run();
   }

   public void render(CustomDrawContext draw, ColorRGBA themeColor, float alpha) {
      float anim = this.hoverAnimation.getValue();
      if (anim > 0.01F) {
         DrawUtil.drawGlow(draw.getMatrices(), this.x - 6.0F, this.y - 3.0F, this.width + 12.0F, this.height + 8.0F, (int) (10.0F + 16.0F * anim));
      }
      DrawUtil.drawShadow(draw.getMatrices(), this.x, this.y, this.width, this.height, 4.0F, BorderRadius.all(9.0F), new ColorRGBA(0, 0, 0).withAlpha(110.0F * alpha));
      ColorRGBA bg = new ColorRGBA(255, 255, 255).withAlpha((6.0F + 22.0F * anim) * alpha);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x, this.y, this.width, this.height, BorderRadius.all(9.0F), bg);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), this.x, this.y, this.width, this.height, 1.0F, BorderRadius.all(9.0F), themeColor.withAlpha((55.0F + 80.0F * anim) * alpha));
      float textWidth = Fonts.REGULAR.getFont(5.0F).width(this.text);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.text, this.x + (this.width - textWidth) / 2.0F, this.y + (this.height - Fonts.REGULAR.getFont(5.0F).height()) / 2.0F, (new ColorRGBA(235, 235, 235)).withAlpha(255.0F * alpha));
   }
}
