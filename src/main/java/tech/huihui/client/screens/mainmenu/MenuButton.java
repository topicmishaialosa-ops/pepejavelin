package tech.huihui.client.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.impl.render.Menu;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class MenuButton {
   private final float width;
   private final float height;
   private final String label;
   private final Runnable action;
   private final Animation hoverAnimation = new Animation(200L, Easing.EXPO_OUT);
   private float x;
   private float y;

   public MenuButton(float width, float height, String label, Runnable action) {
      this.width = width;
      this.height = height;
      this.label = label;
      this.action = action;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setPosition(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public boolean isHovered(float mouseX, float mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.width, this.height);
   }

   public void click() {
      if (this.action != null) {
         this.action.run();
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta, float open) {
      this.hoverAnimation.animateTo(this.action != null && this.isHovered((float) mouseX, (float) mouseY) ? 1.0F : 0.0F);
      this.hoverAnimation.update();
      float hover = Math.min(1.0F, this.hoverAnimation.getValue() / 0.9F);
      float scale = (0.85F + (0.15F * Easing.BACK_OUT.ease(open, 0.0F, 1.0F, 1.0F))) * (1.0F + (0.03F * hover));
      MatrixStack matrices = context.getMatrices();
      float cx = this.x + (this.width / 2.0F);
      float cy = this.y + (this.height / 2.0F);
      matrices.push();
      matrices.translate(cx, cy, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-cx, -cy, 0.0F);

      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      ColorRGBA secondColor = theme.getSecondColor();
      int style = Menu.INSTANCE.getStyleIndex();
      ColorRGBA textColor;
      switch (style) {
         case Menu.STYLE_OUTLINE: {
            DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(55.0F * open));
            DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 1.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha((18.0F + (22.0F * hover)) * open));
            textColor = new ColorRGBA(235, 235, 235);
            break;
         }
         case Menu.STYLE_GRADIENT: {
            DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(8.0F), Gradient.of(
               themeColor.withAlpha(225.0F * open), secondColor.withAlpha(225.0F * open), secondColor.withAlpha(225.0F * open), themeColor.withAlpha(225.0F * open)));
            DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 1.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha((12.0F + (18.0F * hover)) * open));
            textColor = new ColorRGBA(248, 248, 248);
            break;
         }
         case Menu.STYLE_LIGHT: {
            DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(8.0F), new ColorRGBA(250, 250, 252).withAlpha(230.0F * open));
            DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 0.5F, BorderRadius.all(8.0F), new ColorRGBA(0, 0, 0).withAlpha((15.0F + (25.0F * hover)) * open));
            textColor = new ColorRGBA(35, 35, 42);
            break;
         }
         case Menu.STYLE_NEON: {
            DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(205.0F * open));
            DrawUtil.drawRoundedRect(matrices, this.x + 4.0F, this.y + 4.0F, this.width - 8.0F, this.height - 8.0F, BorderRadius.all(6.0F), themeColor.withAlpha((10.0F + (14.0F * hover)) * open));
            DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 1.5F, BorderRadius.all(8.0F), themeColor.withAlpha((60.0F + (60.0F * hover)) * open));
            textColor = new ColorRGBA(245, 245, 245);
            break;
         }
         default: {
            DrawUtil.drawRoundedRect(matrices, this.x, this.y, this.width, this.height, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(198.0F * open));
            DrawUtil.drawRoundedBorder(matrices, this.x, this.y, this.width, this.height, 0.5F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha(hover * 20.0F * open));
            textColor = new ColorRGBA(255, 255, 255);
            break;
         }
      }
      if (this.label != null) {
         float time = (System.currentTimeMillis() % 3000L) / 3000.0F;
         float labelW = Fonts.REGULAR.getFont(8.0F).width(this.label);
         float startX = this.x + ((this.width - labelW) / 2.0F);
         float textY = this.y + ((this.height - Fonts.REGULAR.getFont(8.0F).height()) / 2.0F);
         CustomDrawContext draw = CustomDrawContext.of(context);
         for (int i = 0; i < this.label.length(); i++) {
            float wave = (float) ((Math.sin((time + (i * 0.5F / this.label.length())) * 3.141592654293742D * 2.0D) * 0.5D) + 0.5D);
            float bright = style == Menu.STYLE_LIGHT ? 25.0F + (40.0F * wave * hover) : 180.0F + (65.0F * wave * hover);
            ColorRGBA color = textColor.withAlpha(255.0F * open);
            String ch = String.valueOf(this.label.charAt(i));
            draw.drawText(Fonts.REGULAR.getFont(8.0F), ch, startX, textY, new ColorRGBA((int) (textColor.getRed() * (bright / 255.0F)), (int) (textColor.getGreen() * (bright / 255.0F)), (int) (textColor.getBlue() * (bright / 255.0F)), (int) (color.getAlpha())));
            startX += Fonts.REGULAR.getFont(8.0F).width(ch);
         }
      }
      matrices.pop();
   }
}