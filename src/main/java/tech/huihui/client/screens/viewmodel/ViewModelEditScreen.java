package tech.huihui.client.screens.viewmodel;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
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
   private static final float DRAG_SENSITIVITY = 0.004F;
   private static final float Z_STEP = 0.05F;
   private static final float SCALE_STEP = 0.05F;
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;

   private final ViewModel module;
   private final boolean wasEnabled;
   private boolean editing;
   private boolean dragLeft;

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

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      ColorRGBA themeColor = HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor();

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Тащи мышью — двигать ближайшую руку · Колесо — глубина · Ctrl + Колесо — размер", EXIT_OFFSET, EXIT_OFFSET + 5.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      String left = "Л: X " + format(this.module.leftX.getCurrent()) + " · Y " + format(this.module.leftY.getCurrent()) + " · Z " + format(this.module.leftZ.getCurrent()) + " · S " + format(this.module.leftScale.getCurrent());
      String right = "П: X " + format(this.module.rightX.getCurrent()) + " · Y " + format(this.module.rightY.getCurrent()) + " · Z " + format(this.module.rightZ.getCurrent()) + " · S " + format(this.module.rightScale.getCurrent());
      draw.drawText(Fonts.REGULAR.getFont(5.5F), left, 8.0F, this.height - 14.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), right, this.width - 8.0F - Fonts.REGULAR.getWidth(right, 5.5F), this.height - 14.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      if (button == 0 && MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT)) {
         this.close();
         return true;
      }
      if (button == 0) {
         this.editing = true;
         this.dragLeft = this.isLeftHandClosest(mouseX, mouseY);
      }
      return true;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (!this.editing) {
         this.editing = true;
         this.dragLeft = this.isLeftHandClosest(mouseX, mouseY);
      }
      NumberSetting xSetting = this.dragLeft ? this.module.leftX : this.module.rightX;
      NumberSetting ySetting = this.dragLeft ? this.module.leftY : this.module.rightY;
      setCurrent(xSetting, xSetting.getCurrent() + (float) deltaX * DRAG_SENSITIVITY);
      setCurrent(ySetting, ySetting.getCurrent() - (float) deltaY * DRAG_SENSITIVITY);
      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.editing = false;
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      boolean leftHand = this.isLeftHandClosest(mouseX, mouseY);
      if (this.hasControlDown()) {
         NumberSetting scaleSetting = leftHand ? this.module.leftScale : this.module.rightScale;
         setCurrent(scaleSetting, scaleSetting.getCurrent() + (float) verticalAmount * SCALE_STEP);
      } else {
         NumberSetting zSetting = leftHand ? this.module.leftZ : this.module.rightZ;
         setCurrent(zSetting, zSetting.getCurrent() + (float) verticalAmount * Z_STEP);
      }
      return true;
   }

   private boolean isLeftHandClosest(double mouseX, double mouseY) {
      float dxL = (float) mouseX - ViewModel.leftHandScreenX;
      float dyL = (float) mouseY - ViewModel.leftHandScreenY;
      float dxR = (float) mouseX - ViewModel.rightHandScreenX;
      float dyR = (float) mouseY - ViewModel.rightHandScreenY;
      return dxL * dxL + dyL * dyL <= dxR * dxR + dyR * dyR;
   }

   private static void setCurrent(NumberSetting setting, float value) {
      setting.setCurrent(MathHelper.clamp(value, setting.getMin(), setting.getMax()));
   }

   private static String format(float value) {
      return String.format(Locale.US, "%.2f", value);
   }
}
