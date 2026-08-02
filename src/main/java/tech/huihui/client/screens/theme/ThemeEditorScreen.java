package tech.huihui.client.screens.theme;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.config.ConfigManager;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.base.theme.ThemeManager;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class ThemeEditorScreen extends Screen implements IMinecraft {
   private static final float LIST_WIDTH = 200.0F;
   private static final float LIST_X = 10.0F;
   private static final float LIST_Y = 52.0F;
   private static final float ROW_HEIGHT = 18.0F;
   private static final float EDIT_X = LIST_X + LIST_WIDTH + 12.0F;
   private static final float EDIT_WIDTH = 320.0F;
   private static final float PANEL_PADDING = 12.0F;
   private static final float SLIDER_HEIGHT = 14.0F;
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;

   private final ThemeManager themeManager;
   private Theme selected;
   private boolean editingColor2;
   private boolean nameFocused;
   private String nameBuffer = "";
   private int draggingSlider = -1;
   private float listScroll;

   public ThemeEditorScreen() {
      super(Text.literal("Редактор тем"));
      this.themeManager = HuihuiClient.getInstance().getThemeManager();
      this.selected = this.themeManager.getCurrentTheme();
      this.nameBuffer = this.selected.getName();
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof ThemeEditorScreen) {
         return;
      }
      mc.setScreen(new ThemeEditorScreen());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private float listBoxHeight() {
      return this.height - LIST_Y - 12.0F;
   }

   private int visibleRows() {
      return Math.max(1, (int) (this.listBoxHeight() / ROW_HEIGHT));
   }

   private void clampScroll() {
      float max = Math.max(0.0F, (float) this.themeManager.getThemes().size() * ROW_HEIGHT - this.listBoxHeight());
      this.listScroll = MathHelper.clamp(this.listScroll, 0.0F, max);
   }

   private int editedColor() {
      int c = this.editingColor2 ? this.selected.getColor2() : this.selected.getColor1();
      return ColorRGBA.fromInt(c).getRGB();
   }

   private int editedRed() {
      return new ColorRGBA(this.editedColor()).getRed();
   }

   private int editedGreen() {
      return new ColorRGBA(this.editedColor()).getGreen();
   }

   private int editedBlue() {
      return new ColorRGBA(this.editedColor()).getBlue();
   }

   private void setEditedRgb(int red, int green, int blue) {
      int argb = (new ColorRGBA(red, green, blue, 255)).getRGB();
      if (this.editingColor2) {
         this.selected.setColorRaw2(argb);
      } else {
         this.selected.setColorRaw1(argb);
      }
   }

   private void selectTheme(Theme theme) {
      this.selected = theme;
      this.nameBuffer = theme.getName();
      Theme current = this.themeManager.getCurrentTheme();
      if (current != theme) {
         theme.getAnimation().setValue(0.0F);
         theme.startAnimation(current.getColor1(), current.getColor2());
         this.themeManager.setCurrentTheme(theme);
      }
   }

   private void createTheme() {
      String name = this.nameBuffer.trim();
      if (name.isEmpty()) {
         name = "Тема " + (this.themeManager.getThemes().size() + 1);
      }
      Theme theme = new Theme(name, this.selected.getColor1(), this.selected.getColor2(), false);
      theme.setPreset(false);
      this.themeManager.addTheme(theme);
      this.selectTheme(theme);
   }

   private void deleteTheme() {
      if (this.selected == null || this.selected.isPreset()) {
         return;
      }
      List<Theme> themes = this.themeManager.getThemes();
      int index = themes.indexOf(this.selected);
      this.themeManager.removeTheme(this.selected);
      if (themes.isEmpty()) {
         this.selected = this.themeManager.getDefaultTheme();
      } else {
         this.selected = themes.get(Math.max(0, Math.min(index, themes.size() - 1)));
      }
      this.nameBuffer = this.selected.getName();
      this.themeManager.setCurrentTheme(this.selected);
   }

   private void resetTheme() {
      if (this.selected != null && this.selected.isPreset()) {
         this.selected.resetToDefault();
      }
   }

   private void saveNow() {
      HuihuiClient.getInstance().getConfigManager().saveConfig("current_config");
   }

   private void commitName() {
      String name = this.nameBuffer.trim();
      if (!name.isEmpty() && this.selected != null) {
         this.selected.setName(name);
      }
      this.nameBuffer = this.selected == null ? "" : this.selected.getName();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      if (this.draggingSlider >= 0) {
         this.updateSlider(mouseX, this.draggingSlider);
      }

      CustomDrawContext draw = CustomDrawContext.of(context);
      ColorRGBA themeColor = this.themeManager.getCurrentTheme().getColor();
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Редактор тем", EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Выбери тему слева и крути слайдеры — цвета меняются на лету", EXIT_OFFSET, EXIT_OFFSET + 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      this.renderList(draw, themeColor, mouseX, mouseY);
      this.renderEditor(draw, themeColor, mouseX, mouseY);
   }

   private void renderList(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float x = LIST_X;
      float y = LIST_Y;
      float h = this.listBoxHeight();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, LIST_WIDTH, h, BorderRadius.all(6.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, LIST_WIDTH, h, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(60));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Темы (" + this.themeManager.getThemes().size() + ")", x + PANEL_PADDING, y + 8.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      this.clampScroll();
      float contentY = y + 26.0F - this.listScroll;
      draw.enableScissor((int) x + 2, (int) y + 24, (int) (x + LIST_WIDTH - 2), (int) (y + h - 2));
      int index = 0;
      for (Theme theme : this.themeManager.getThemes()) {
         float ry = contentY + (float) index * ROW_HEIGHT;
         if (ry + ROW_HEIGHT >= y + 26.0F && ry <= y + h) {
            boolean isCurrent = theme == this.themeManager.getCurrentTheme();
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 2.0F, ry, LIST_WIDTH - 4.0F, ROW_HEIGHT);
            if (isCurrent) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, ry + 1.0F, LIST_WIDTH - 6.0F, ROW_HEIGHT - 2.0F, BorderRadius.all(4.0F), themeColor.withAlpha(70));
            } else if (hovered) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, ry + 1.0F, LIST_WIDTH - 6.0F, ROW_HEIGHT - 2.0F, BorderRadius.all(4.0F), new ColorRGBA(60, 60, 60).withAlpha(110));
            }
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 6.0F, ry + 5.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), theme.getColor(), theme.getColor(), theme.getSecondColor(), theme.getSecondColor());
            draw.drawText(Fonts.REGULAR.getFont(5.0F), theme.getName(), x + 19.0F, ry + 6.0F, new ColorRGBA(isCurrent ? 255 : 200, isCurrent ? 255 : 200, isCurrent ? 255 : 200).withAlpha(255));
         }
         index++;
      }
      draw.disableScissor();
   }

   private void renderEditor(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      if (this.selected == null) {
         return;
      }
      float x = EDIT_X;
      float y = LIST_Y;
      float w = EDIT_WIDTH;
      float h = this.listBoxHeight();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(6.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(60));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Настройка", x + PANEL_PADDING, y + 8.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      float swatchW = (w - PANEL_PADDING * 2.0F - 10.0F) / 2.0F;
      float btnW = (w - PANEL_PADDING * 2.0F - 10.0F) / 2.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Название", x + PANEL_PADDING, y + 34.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
      float nameY = y + 47.0F;
      boolean nameHovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, nameY, w - PANEL_PADDING * 2.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, nameY, w - PANEL_PADDING * 2.0F, 16.0F, BorderRadius.all(4.0F), new ColorRGBA(30, 30, 30).withAlpha(255));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x + PANEL_PADDING, nameY, w - PANEL_PADDING * 2.0F, 16.0F, 1.0F, BorderRadius.all(4.0F), this.nameFocused ? themeColor.withAlpha(160) : (nameHovered ? themeColor.withAlpha(80) : new ColorRGBA(50, 50, 50).withAlpha(255)));
      String nameText = this.nameBuffer + (this.nameFocused && System.currentTimeMillis() % 1000L > 500L ? "|" : "");
      draw.drawText(Fonts.REGULAR.getFont(5.0F), nameText, x + PANEL_PADDING + 4.0F, nameY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      this.renderColorSwatch(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING, y + 72.0F, swatchW, "Цвет 1", this.selected.getColor1(), !this.editingColor2);
      this.renderColorSwatch(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING + swatchW + 10.0F, y + 72.0F, swatchW, "Цвет 2", this.selected.getColor2(), this.editingColor2);

      for (int channel = 0; channel < 3; channel++) {
         this.renderSlider(draw, themeColor, mouseX, mouseY, y + 112.0F + (float) channel * (SLIDER_HEIGHT + 12.0F), channel == 0 ? "R" : channel == 1 ? "G" : "B", channel);
      }

      ColorRGBA preview = new ColorRGBA(this.editedRed(), this.editedGreen(), this.editedBlue());
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Предпросмотр", x + PANEL_PADDING, y + 188.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, y + 201.0F, w - PANEL_PADDING * 2.0F, 20.0F, BorderRadius.all(4.0F), preview, preview, preview.mix(ColorRGBA.BLACK, 0.35F), preview.mix(ColorRGBA.BLACK, 0.35F));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "#" + String.format("%02X%02X%02X", this.editedRed(), this.editedGreen(), this.editedBlue()), x + PANEL_PADDING + 4.0F, y + 205.0F, new ColorRGBA(30, 30, 30).withAlpha(200));

      this.renderButton(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING, y + 232.0F, btnW, "Создать");
      this.renderButton(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING + btnW + 10.0F, y + 232.0F, btnW, "Удалить");
      this.renderButton(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING, y + 256.0F, btnW, "Сбросить");
      this.renderButton(draw, themeColor, mouseX, mouseY, x + PANEL_PADDING + btnW + 10.0F, y + 256.0F, btnW, "Сохранить");
   }

   private void renderColorSwatch(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY, float x, float y, float w, String label, int color, boolean active) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, 28.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, 28.0F, BorderRadius.all(5.0F), new ColorRGBA(30, 30, 30).withAlpha(255));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, 28.0F, 1.5F, BorderRadius.all(5.0F), active ? themeColor.withAlpha(200) : (hovered ? themeColor.withAlpha(90) : new ColorRGBA(50, 50, 50).withAlpha(255)));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 6.0F, y + 6.0F, 16.0F, 16.0F, BorderRadius.all(4.0F), ColorRGBA.fromInt(color));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + 28.0F, y + 9.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void renderSlider(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY, float y, String label, int channel) {
      float x = EDIT_X + PANEL_PADDING;
      float w = EDIT_WIDTH - PANEL_PADDING * 2.0F;
      int value;
      switch (channel) {
         case 0: value = this.editedRed(); break;
         case 1: value = this.editedGreen(); break;
         default: value = this.editedBlue(); break;
      }
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x, y, new ColorRGBA(200, 200, 200).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), String.valueOf(value), x + w - Fonts.REGULAR.getWidth(String.valueOf(value), 5.0F), y, new ColorRGBA(153, 153, 153).withAlpha(255));

      float trackY = y + 10.0F;
      ColorRGBA c0 = new ColorRGBA(channel == 0 ? 0 : this.editedRed(), channel == 1 ? 0 : this.editedGreen(), channel == 2 ? 0 : this.editedBlue());
      ColorRGBA c1 = new ColorRGBA(channel == 0 ? 255 : this.editedRed(), channel == 1 ? 255 : this.editedGreen(), channel == 2 ? 255 : this.editedBlue());
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, w, 4.0F, BorderRadius.all(2.0F), c0, c0, c1, c1);
      float knob = w * (float) value / 255.0F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, trackY - 4.0F, w, 12.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + knob - 3.0F, trackY - 3.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), (this.draggingSlider == channel || hovered) ? themeColor.withAlpha(255) : themeColor.withAlpha(180));
   }

   private void renderButton(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY, float x, float y, float w, String label) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, 18.0F);
      ColorRGBA fill = hovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, 18.0F, BorderRadius.all(4.0F), fill);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + w / 2.0F - Fonts.REGULAR.getWidth(label, 5.0F) / 2.0F, y + 6.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void updateSlider(int mouseX, int channel) {
      float x = EDIT_X + PANEL_PADDING;
      float w = EDIT_WIDTH - PANEL_PADDING * 2.0F;
      float percent = MathHelper.clamp((float) (mouseX - x) / w, 0.0F, 1.0F);
      int value = Math.round(percent * 255.0F);
      int r = this.editedRed();
      int g = this.editedGreen();
      int b = this.editedBlue();
      switch (channel) {
         case 0: r = value; break;
         case 1: g = value; break;
         default: b = value; break;
      }
      this.setEditedRgb(r, g, b);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }
      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      if (MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT)) {
         this.close();
         return true;
      }

      float x = LIST_X;
      float y = LIST_Y;
      float h = this.listBoxHeight();
      if (MathUtil.isHovered(mouseX, mouseY, x, y, LIST_WIDTH, h)) {
         int index = (int) ((mouseY - (y + 26.0F - this.listScroll)) / ROW_HEIGHT);
         List<Theme> themes = this.themeManager.getThemes();
         if (index >= 0 && index < themes.size()) {
            this.selectTheme(themes.get(index));
         }
         return true;
      }

      if (this.selected == null) {
         return true;
      }
      float ex = EDIT_X;
      float ey = LIST_Y;
      float ew = EDIT_WIDTH;
      float nameY = ey + 47.0F;
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING, nameY, ew - PANEL_PADDING * 2.0F, 16.0F)) {
         this.nameFocused = true;
         return true;
      }
      this.nameFocused = false;
      this.commitName();

      float swatchY = ey + 72.0F;
      float swatchW = (ew - PANEL_PADDING * 2.0F - 10.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING, swatchY, swatchW, 28.0F)) {
         this.editingColor2 = false;
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING + swatchW + 10.0F, swatchY, swatchW, 28.0F)) {
         this.editingColor2 = true;
         return true;
      }

      float sliderW = ew - PANEL_PADDING * 2.0F;
      for (int channel = 0; channel < 3; channel++) {
         float sliderY = ey + 112.0F + (float) channel * (SLIDER_HEIGHT + 12.0F);
         if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING, sliderY + 6.0F, sliderW, 12.0F)) {
            this.draggingSlider = channel;
            this.updateSlider((int) mouseX, channel);
            return true;
         }
      }

      float btnW = (ew - PANEL_PADDING * 2.0F - 10.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING, ey + 232.0F, btnW, 18.0F)) {
         this.createTheme();
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING + btnW + 10.0F, ey + 232.0F, btnW, 18.0F)) {
         this.deleteTheme();
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING, ey + 256.0F, btnW, 18.0F)) {
         this.resetTheme();
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, ex + PANEL_PADDING + btnW + 10.0F, ey + 256.0F, btnW, 18.0F)) {
         this.saveNow();
         return true;
      }
      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingSlider = -1;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float x = LIST_X;
      float y = LIST_Y;
      float h = this.listBoxHeight();
      if (MathUtil.isHovered(mouseX, mouseY, x, y, LIST_WIDTH, h)) {
         this.listScroll -= (float) verticalAmount * ROW_HEIGHT;
         this.clampScroll();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.nameFocused) {
         if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.nameBuffer.isEmpty()) {
               this.nameBuffer = this.nameBuffer.substring(0, this.nameBuffer.length() - 1);
            }
         } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            this.nameFocused = false;
            this.commitName();
         }
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.close();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.nameFocused) {
         if (this.nameBuffer.length() < 32) {
            this.nameBuffer = this.nameBuffer + chr;
         }
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   @Override
   public void close() {
      this.commitName();
      super.close();
   }
}
