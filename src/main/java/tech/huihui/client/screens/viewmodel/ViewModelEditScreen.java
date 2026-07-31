package tech.huihui.client.screens.viewmodel;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.render.ViewModel;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class ViewModelEditScreen extends Screen implements IMinecraft {
   private static final float Z_STEP = 0.05F;
   private static final float SCALE_STEP = 0.03F;
   private static final float GRAB_RADIUS = 30.0F;
   private static final float FALLBACK_PX_PER_WORLD = 500.0F;
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;

   private static final ColorRGBA LEFT_COLOR = new ColorRGBA(90, 200, 255, 255);
   private static final ColorRGBA RIGHT_COLOR = new ColorRGBA(255, 120, 200, 255);

   private final ViewModel module;
   private final boolean wasEnabled;
   private Drag drag;

   public ViewModelEditScreen(ViewModel module) {
      super(Text.literal("Редактор рук"));
      this.module = module;
      this.wasEnabled = module.isEnabled();
      if (!this.wasEnabled) {
         this.module.setToggled(true);
      }
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof ViewModelEditScreen) {
         return;
      }
      mc.setScreen(new ViewModelEditScreen(ViewModel.INSTANCE));
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      if (!this.wasEnabled) {
         this.module.setToggled(false);
      }
      super.close();
   }

   private NumberSetting xSetting(boolean left) {
      return left ? this.module.leftX : this.module.rightX;
   }

   private NumberSetting ySetting(boolean left) {
      return left ? this.module.leftY : this.module.rightY;
   }

   private NumberSetting zSetting(boolean left) {
      return left ? this.module.leftZ : this.module.rightZ;
   }

   private NumberSetting scaleSetting(boolean left) {
      return left ? this.module.leftScale : this.module.rightScale;
   }

   private float handScreenX(boolean left) {
      return left ? ViewModel.leftHandScreenX : ViewModel.rightHandScreenX;
   }

   private float handScreenY(boolean left) {
      return left ? ViewModel.leftHandScreenY : ViewModel.rightHandScreenY;
   }

   private float pxPerWorldX(boolean left) {
      return left ? ViewModel.leftHandPxPerWorldX : ViewModel.rightHandPxPerWorldX;
   }

   private float pxPerWorldY(boolean left) {
      return left ? ViewModel.leftHandPxPerWorldY : ViewModel.rightHandPxPerWorldY;
   }

   private boolean validPosition(float x, float y) {
      return x > 0.0F && y > 0.0F && x < (float)this.width && y < (float)this.height;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      if (this.drag != null) {
         this.drag.update(mouseX, mouseY);
      }

      float leftX = this.handScreenX(true);
      float leftY = this.handScreenY(true);
      float rightX = this.handScreenX(false);
      float rightY = this.handScreenY(false);

      CustomDrawContext draw = CustomDrawContext.of(context);
      ColorRGBA themeColor = HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor();

      this.renderTopBar(draw, themeColor, mouseX, mouseY);
      this.renderMarker(draw, leftX, leftY, LEFT_COLOR, this.isGrabbed(true), "Левая");
      this.renderMarker(draw, rightX, rightY, RIGHT_COLOR, this.isGrabbed(false), "Правая");
      this.renderCrosshair(draw, mouseX, mouseY);
      this.renderValues(draw);
      this.renderReset(draw, themeColor, mouseX, mouseY);
   }

   private boolean isGrabbed(boolean left) {
      return this.drag != null && this.drag.left == left;
   }

   private void renderTopBar(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Редактор рук", EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Тащи предмет мышью — позиция. Колесо над рукой — глубина · Ctrl+Колесо — размер", EXIT_OFFSET, EXIT_OFFSET + 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
   }

   private void renderMarker(CustomDrawContext draw, float x, float y, ColorRGBA color, boolean grabbed, String label) {
      if (!this.validPosition(x, y)) {
         return;
      }
      float size = grabbed ? 22.0F : 14.0F;
      float cx = x - size / 2.0F;
      float cy = y - size / 2.0F;
      ColorRGBA fill = color.withAlpha(grabbed ? 90 : 45);
      ColorRGBA border = color.withAlpha(grabbed ? 255 : 200);
      DrawUtil.drawRoundedRect(draw.getMatrices(), cx, cy, size, size, BorderRadius.all(size / 2.0F), fill);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), cx, cy, size, size, grabbed ? 2.0F : 1.5F, BorderRadius.all(size / 2.0F), border);
      draw.drawText(Fonts.REGULAR.getFont(4.5F), label, x - Fonts.REGULAR.getWidth(label, 4.5F) / 2.0F, y - size / 2.0F - 10.0F, color.withAlpha(255));
   }

   private void renderCrosshair(CustomDrawContext draw, int mouseX, int mouseY) {
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f((float)mouseX - 6.0F, (float)mouseY), new Vec2f((float)mouseX + 6.0F, (float)mouseY), new ColorRGBA(222, 222, 222).withAlpha(160));
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f((float)mouseX, (float)mouseY - 6.0F), new Vec2f((float)mouseX, (float)mouseY + 6.0F), new ColorRGBA(222, 222, 222).withAlpha(160));
   }

   private void renderValues(CustomDrawContext draw) {
      String left = "Л: X " + format(this.module.leftX.getCurrent()) + " · Y " + format(this.module.leftY.getCurrent()) + " · Z " + format(this.module.leftZ.getCurrent()) + " · S " + format(this.module.leftScale.getCurrent());
      String right = "П: X " + format(this.module.rightX.getCurrent()) + " · Y " + format(this.module.rightY.getCurrent()) + " · Z " + format(this.module.rightZ.getCurrent()) + " · S " + format(this.module.rightScale.getCurrent());
      draw.drawText(Fonts.REGULAR.getFont(5.5F), left, 8.0F, this.height - 24.0F, LEFT_COLOR.withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), right, this.width - 8.0F - Fonts.REGULAR.getWidth(right, 5.5F), this.height - 24.0F, RIGHT_COLOR.withAlpha(255));
   }

   private void renderReset(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float rx = this.width / 2.0F - 60.0F;
      float ry = this.height - 32.0F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, rx, ry, 120.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), rx, ry, 120.0F, 16.0F, BorderRadius.all(3.0F), hovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Сбросить руки", this.width / 2.0F - Fonts.REGULAR.getWidth("Сбросить руки", 5.0F) / 2.0F, this.height - 27.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private final class Drag {
      private final boolean left;
      private final float grabMouseX;
      private final float grabMouseY;
      private final float grabWorldX;
      private final float grabWorldY;
      private final float pxX;
      private final float pxY;

      private Drag(boolean left, float mouseX, float mouseY) {
         this.left = left;
         this.grabMouseX = mouseX;
         this.grabMouseY = mouseY;
         this.grabWorldX = ViewModelEditScreen.this.xSetting(left).getCurrent();
         this.grabWorldY = ViewModelEditScreen.this.ySetting(left).getCurrent();
         float mx = ViewModelEditScreen.this.pxPerWorldX(left);
         float my = ViewModelEditScreen.this.pxPerWorldY(left);
         this.pxX = mx > 1.0F ? mx : FALLBACK_PX_PER_WORLD;
         this.pxY = my > 1.0F ? my : FALLBACK_PX_PER_WORLD;
      }

      private void update(float mouseX, float mouseY) {
         float newX = this.grabWorldX + (mouseX - this.grabMouseX) / this.pxX;
         float newY = this.grabWorldY - (mouseY - this.grabMouseY) / this.pxY;
         setCurrent(ViewModelEditScreen.this.xSetting(this.left), newX);
         setCurrent(ViewModelEditScreen.this.ySetting(this.left), newY);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT)) {
         this.close();
         return true;
      }
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, this.width / 2.0F - 60.0F, this.height - 32.0F, 120.0F, 16.0F)) {
         this.resetHands();
         return true;
      }
      if (button == 0) {
         boolean left = this.closestHand(mouseX, mouseY);
         if (this.withinGrabRadius(mouseX, mouseY)) {
            this.drag = new Drag(left, (float)mouseX, (float)mouseY);
            return true;
         }
      }
      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.drag = null;
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      boolean left = this.closestHand(mouseX, mouseY);
      boolean scale = this.hasControlDown();
      NumberSetting setting = scale ? this.scaleSetting(left) : this.zSetting(left);
      float step = scale ? SCALE_STEP : Z_STEP;
      setCurrent(setting, setting.getCurrent() + (float)verticalAmount * step);
      return true;
   }

   private boolean closestHand(double mouseX, double mouseY) {
      float leftX = this.handScreenX(true);
      float leftY = this.handScreenY(true);
      float rightX = this.handScreenX(false);
      float rightY = this.handScreenY(false);
      float distL = this.validPosition(leftX, leftY) ? this.sqDist(mouseX, mouseY, leftX, leftY) : Float.MAX_VALUE;
      float distR = this.validPosition(rightX, rightY) ? this.sqDist(mouseX, mouseY, rightX, rightY) : Float.MAX_VALUE;
      return distL <= distR;
   }

   private boolean withinGrabRadius(double mouseX, double mouseY) {
      boolean left = this.closestHand(mouseX, mouseY);
      float hx = this.handScreenX(left);
      float hy = this.handScreenY(left);
      return this.validPosition(hx, hy) && this.sqDist(mouseX, mouseY, hx, hy) <= GRAB_RADIUS * GRAB_RADIUS;
   }

   private void resetHands() {
      this.module.leftX.setCurrent(0.0F);
      this.module.leftY.setCurrent(0.0F);
      this.module.leftZ.setCurrent(0.0F);
      this.module.leftScale.setCurrent(1.0F);
      this.module.rightX.setCurrent(0.0F);
      this.module.rightY.setCurrent(0.0F);
      this.module.rightZ.setCurrent(0.0F);
      this.module.rightScale.setCurrent(1.0F);
   }

   private float sqDist(double mx, double my, float x, float y) {
      float dx = (float)mx - x;
      float dy = (float)my - y;
      return dx * dx + dy * dy;
   }

   private static void setCurrent(NumberSetting setting, float value) {
      setting.setCurrent(MathHelper.clamp(value, setting.getMin(), setting.getMax()));
   }

   private static String format(float value) {
      return String.format(Locale.US, "%.2f", value);
   }
}
