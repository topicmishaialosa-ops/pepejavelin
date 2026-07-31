package tech.huihui.client.screens.targethud;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.render.TargetHud;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class TargetHudEditScreen extends Screen implements IMinecraft {
   private static final String[] PRESETS = {"Крупный", "Маленький", "Проценты", "Большая полоса", "Вертикальный", "Метал", "Мини", "Градиент", "Боссбар", "Минимал"};
   private static final String[] MODES = {"Проценты и HP", "Проценты", "HP"};
   private static final String[] COLOR_NAMES = {"Цвет полоски", "Цвет фона", "Цвет рамки", "Цвет текста"};
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;
   private static final float PANEL_WIDTH = 190.0F;
   private static final float PANEL_OFFSET = 10.0F;
   private static final float PANEL_PADDING = 10.0F;
   private static final float PICKER_HEIGHT = 60.0F;
   private static final float LIST_WIDTH = 250.0F;

   private final TargetHud module;
   private final TargetHudPresetManager presetManager;
   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private DragTarget dragTarget = DragTarget.NONE;
   private ColorSetting activeColor;
   private final float[] hsb = new float[3];
   private boolean draggingPicker;
   private boolean draggingHue;
   private float scroll;
   private TextFieldWidget nameField;
   private boolean initialized;

   private enum DragTarget {
      NONE, THICKNESS, HEAD_SIZE, HEAD_YAW, HEAD_PITCH
   }

   public TargetHudEditScreen(TargetHud module) {
      super(Text.literal("Редактор таргетхуда"));
      this.module = module;
      this.presetManager = HuihuiClient.getInstance().getTargetHudPresetManager();
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof TargetHudEditScreen) {
         return;
      }
      mc.setScreen(new TargetHudEditScreen(TargetHud.INSTANCE));
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private TextFieldWidget nameField() {
      if (!this.initialized && mc.textRenderer != null) {
         this.nameField = new TextFieldWidget(mc.textRenderer, 0, 0, 140, 16, Text.empty());
         this.nameField.setMaxLength(24);
         this.initialized = true;
      }
      return this.nameField;
   }

   private float panelX() {
      return this.width - PANEL_WIDTH - PANEL_OFFSET;
   }

   private float panelY() {
      return 20.0F;
   }

   private float panelHeight() {
      float h = this.colorStartY() + 72.0F;
      if (this.activeColor != null) {
         h += 66.0F;
      }
      return h + 10.0F - this.panelY();
   }

   private float sliderTrackX() {
      return this.panelX() + PANEL_PADDING;
   }

   private float sliderTrackWidth() {
      return PANEL_WIDTH - PANEL_PADDING * 2.0F;
   }

   private float thicknessTrackY() {
      return this.panelY() + 44.0F;
   }

   private float headSizeTrackY() {
      return this.panelY() + 74.0F;
   }

   private float headYawTrackY() {
      return this.panelY() + 104.0F;
   }

   private float headPitchTrackY() {
      return this.panelY() + 134.0F;
   }

   private float autoRotateToggleY() {
      return this.panelY() + 160.0F;
   }

   private float modeRowY() {
      return this.panelY() + 190.0F;
   }

   private float modeButtonWidth() {
      return (PANEL_WIDTH - PANEL_PADDING * 2.0F - 8.0F) / 3.0F;
   }

   private float modeButtonX(int index) {
      return this.panelX() + PANEL_PADDING + index * (this.modeButtonWidth() + 4.0F);
   }

   private float customToggleY() {
      return this.panelY() + 222.0F;
   }

   private float colorStartY() {
      return this.panelY() + 244.0F;
   }

   private float colorRowY(int index) {
      return this.colorStartY() + index * 18.0F;
   }

   private float pickerX() {
      return this.panelX() + PANEL_PADDING + 8.0F;
   }

   private float pickerY() {
      return this.colorStartY() + 78.0F;
   }

   private float pickerWidth() {
      return this.sliderTrackWidth() - 18.0F;
   }

   private float hueSliderX() {
      return this.pickerX() + this.pickerWidth() + 4.0F;
   }

   private float bottomBarY() {
      return this.height - 34.0F;
   }

   private float listX() {
      return 10.0F;
   }

   private float listY() {
      return 70.0F;
   }

   private float listHeight() {
      return Math.max(this.bottomBarY() - this.listY() - 10.0F, 40.0F);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 110));

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Редактор таргетхуда", EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Тащи таргетхуд мышью — позиция. Колесо над списком — прокрутка", EXIT_OFFSET, EXIT_OFFSET + 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      this.renderPresetSelector(draw, theme, mouseX, mouseY);
      this.renderPresetList(draw, theme, mouseX, mouseY);
      this.renderPanel(draw, theme, mouseX, mouseY);
      this.renderBottomBar(draw, theme, mouseX, mouseY);

      try {
         this.module.renderPreview(draw);
      } catch (Exception var13) {
         var13.printStackTrace();
      }

      String pos = "X: " + String.format(Locale.US, "%.0f", this.module.x.getCurrent()) + " · Y: " + String.format(Locale.US, "%.0f", this.module.y.getCurrent());
      draw.drawText(Fonts.REGULAR.getFont(5.0F), pos, EXIT_OFFSET, this.height - 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
   }

   private void renderPresetSelector(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float centerX = this.width / 2.0F;
      float y = 46.0F;
      String name = this.module.type.get();
      float pillWidth = 220.0F;
      float pillX = centerX - pillWidth / 2.0F;
      float arrowWidth = 20.0F;
      boolean pillHovered = MathUtil.isHovered(mouseX, mouseY, pillX, y, pillWidth, 16.0F);
      boolean leftHovered = MathUtil.isHovered(mouseX, mouseY, pillX - arrowWidth - 4.0F, y, arrowWidth, 16.0F);
      boolean rightHovered = MathUtil.isHovered(mouseX, mouseY, pillX + pillWidth + 4.0F, y, arrowWidth, 16.0F);

      DrawUtil.drawRoundedRect(draw.getMatrices(), pillX - arrowWidth - 4.0F, y, arrowWidth, 16.0F, BorderRadius.all(3.0F), leftHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      DrawUtil.drawRoundedRect(draw.getMatrices(), pillX, y, pillWidth, 16.0F, BorderRadius.all(3.0F), pillHovered ? theme.getColor().withAlpha(90) : new ColorRGBA(15, 15, 15).withAlpha(180));
      DrawUtil.drawRoundedRect(draw.getMatrices(), pillX + pillWidth + 4.0F, y, arrowWidth, 16.0F, BorderRadius.all(3.0F), rightHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "«", pillX - arrowWidth - 4.0F + arrowWidth / 2.0F - Fonts.REGULAR.getWidth("«", 5.0F) / 2.0F, y + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), name, centerX - Fonts.REGULAR.getWidth(name, 5.5F) / 2.0F, y + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "»", pillX + pillWidth + 4.0F + arrowWidth / 2.0F - Fonts.REGULAR.getWidth("»", 5.0F) / 2.0F, y + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(4.5F), "Тип пресета", centerX - Fonts.REGULAR.getWidth("Тип пресета", 4.5F) / 2.0F, y - 9.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
   }

   private void renderPresetList(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float x = this.listX();
      float y = this.listY();
      float w = LIST_WIDTH;
      float h = this.listHeight();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(5.0F), new ColorRGBA(15, 15, 15).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Мои таргетхуды", x + PANEL_PADDING, y + 10.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      List<TargetHudPreset> presets = this.presetManager.getPresets();
      float contentY = y + 28.0F;
      float visibleH = h - 34.0F;
      float contentH = presets.size() * 20.0F;
      this.scroll = MathHelper.clamp(this.scroll, 0.0F, Math.max(contentH - visibleH, 0.0F));

      if (presets.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(4.5F), "Пусто — создай и сохрани свой таргетхуд", x + PANEL_PADDING, contentY, new ColorRGBA(120, 120, 120).withAlpha(255));
         return;
      }

      draw.enableScissor((int) x, (int) (contentY - 2), (int) (x + w), (int) (contentY + visibleH + 2));
      for (int i = 0; i < presets.size(); i++) {
         TargetHudPreset preset = presets.get(i);
         float rowY = contentY - this.scroll + i * 20.0F;
         if (rowY + 18.0F < contentY - 2 || rowY > contentY + visibleH + 2) {
            continue;
         }
         boolean hovered = mouseY >= rowY && mouseY <= rowY + 18.0F && mouseX >= x + 5.0F && mouseX <= x + w - 5.0F;
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, rowY, w - 10.0F, 18.0F, BorderRadius.all(3.0F), hovered ? theme.getColor().withAlpha(80) : new ColorRGBA(25, 25, 25).withAlpha(150));
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 9.0F, rowY + 5.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), new ColorRGBA(preset.getBarColor()));
         String name = preset.getName();
         draw.drawText(Fonts.REGULAR.getFont(4.5F), name, x + 22.0F, rowY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
         boolean delHovered = mouseX >= x + w - 24.0F && mouseX <= x + w - 8.0F && mouseY >= rowY + 1.0F && mouseY <= rowY + 17.0F;
         draw.drawText(Fonts.REGULAR.getFont(5.0F), "✕", x + w - 19.0F - Fonts.REGULAR.getWidth("✕", 5.0F) / 2.0F, rowY + 5.0F, (delHovered ? new ColorRGBA(255, 80, 80) : new ColorRGBA(140, 140, 140)).withAlpha(255));
      }
      draw.disableScissor();
   }

   private void renderPanel(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, PANEL_WIDTH, this.panelHeight(), BorderRadius.all(5.0F), new ColorRGBA(15, 15, 15).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, PANEL_WIDTH, this.panelHeight(), 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Настройки", x + PANEL_PADDING, y + 12.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      this.renderSlider(draw, theme, y + 30.0F, this.thicknessTrackY(), "Толщина полоски", this.module.barThickness);
      this.renderSlider(draw, theme, y + 60.0F, this.headSizeTrackY(), "Размер иконки головы", this.module.headSize);
      this.renderSlider(draw, theme, y + 90.0F, this.headYawTrackY(), "Поворот головы", this.module.headYaw);
      this.renderSlider(draw, theme, y + 120.0F, this.headPitchTrackY(), "Наклон головы", this.module.headPitch);

      boolean auto = this.module.headAutoRotate.isEnabled();
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Автоповорот головы", x + PANEL_PADDING, y + 150.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
      boolean autoHovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.autoRotateToggleY(), 50.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.autoRotateToggleY(), 50.0F, 16.0F, BorderRadius.all(8.0F), auto ? theme.getColor().withAlpha(180) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), auto ? "Вкл" : "Выкл", x + PANEL_WIDTH - PANEL_PADDING - 25.0F - Fonts.REGULAR.getWidth(auto ? "Вкл" : "Выкл", 5.0F) / 2.0F, this.autoRotateToggleY() + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Отображение", x + PANEL_PADDING, y + 180.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
      for (int i = 0; i < MODES.length; i++) {
         boolean selected = this.module.displayMode.is(MODES[i]);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, this.modeButtonX(i), this.modeRowY(), this.modeButtonWidth(), 16.0F);
         ColorRGBA bg = selected ? theme.getColor().withAlpha(160) : hovered ? theme.getColor().withAlpha(90) : new ColorRGBA(40, 40, 40).withAlpha(255);
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.modeButtonX(i), this.modeRowY(), this.modeButtonWidth(), 16.0F, BorderRadius.all(3.0F), bg);
         String label = MODES[i];
         draw.drawText(Fonts.REGULAR.getFont(4.5F), label, this.modeButtonX(i) + this.modeButtonWidth() / 2.0F - Fonts.REGULAR.getWidth(label, 4.5F) / 2.0F, this.modeRowY() + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      }

      boolean custom = this.module.customColors.isEnabled();
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Свои цвета", x + PANEL_PADDING, y + 211.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
      boolean customHovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.customToggleY(), 50.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.customToggleY(), 50.0F, 16.0F, BorderRadius.all(8.0F), custom ? theme.getColor().withAlpha(180) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), custom ? "Вкл" : "Выкл", x + PANEL_WIDTH - PANEL_PADDING - 25.0F - Fonts.REGULAR.getWidth(custom ? "Вкл" : "Выкл", 5.0F) / 2.0F, this.customToggleY() + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      ColorSetting[] colors = {this.module.barColor, this.module.bgColor, this.module.borderColor, this.module.textColor};
      float alpha = custom ? 1.0F : 0.35F;
      for (int i = 0; i < colors.length; i++) {
         float rowY = this.colorRowY(i);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, rowY, this.sliderTrackWidth(), 16.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, rowY, this.sliderTrackWidth(), 16.0F, BorderRadius.all(3.0F), this.activeColor == colors[i] ? theme.getColor().withAlpha(120) : hovered ? new ColorRGBA(50, 50, 50).withAlpha(180) : new ColorRGBA(30, 30, 30).withAlpha(150));
         draw.drawText(Fonts.REGULAR.getFont(4.5F), COLOR_NAMES[i], x + PANEL_PADDING + 6.0F, rowY + 5.0F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_WIDTH - PANEL_PADDING - 18.0F, rowY + 3.0F, 12.0F, 10.0F, BorderRadius.all(2.5F), colors[i].getColor().withAlpha(255.0F * alpha));
         DrawUtil.drawRoundedBorder(draw.getMatrices(), x + PANEL_WIDTH - PANEL_PADDING - 18.0F, rowY + 3.0F, 12.0F, 10.0F, 1.0F, BorderRadius.all(2.5F), (new ColorRGBA(0, 0, 0)).withAlpha(120));
      }

      if (this.activeColor != null) {
         this.renderPicker(draw, theme, mouseX, mouseY, alpha);
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
         this.hsb[0] = MathHelper.clamp((mouseY - this.pickerY()) / PICKER_HEIGHT, 0.0F, 1.0F);
         this.applyColor();
      }

      ColorRGBA hue = ColorRGBA.fromHSB(this.hsb[0], 1.0F, 1.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.pickerX(), this.pickerY(), this.pickerWidth(), PICKER_HEIGHT, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), hue.withAlpha(255.0F * alpha));

      float knobX = this.pickerX() + 4.0F + this.hsb[1] * (this.pickerWidth() - 8.0F);
      float knobY = this.pickerY() + 4.0F + (1.0F - this.hsb[2]) * (PICKER_HEIGHT - 8.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));

      for (int i = 0; i < (int) PICKER_HEIGHT; i++) {
         float rowHue = (float) i / PICKER_HEIGHT;
         DrawUtil.drawRect(draw.getMatrices(), this.hueSliderX(), this.pickerY() + (float) i, 4.0F, 1.0F, ColorRGBA.fromHSB(rowHue, 1.0F, 1.0F).withAlpha(255.0F * alpha));
      }
      float knobY2 = this.pickerY() + this.hsb[0] * PICKER_HEIGHT;
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.hueSliderX() - 2.5F, knobY2 - 4.0F, 9.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.hueSliderX() - 1.5F, knobY2 - 3.0F, 7.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
   }

   private void renderBottomBar(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float y = this.bottomBarY();
      String label = "Название:";
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, 10.0F, y + 5.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
      float fieldX = 10.0F + Fonts.REGULAR.getWidth(label, 5.5F) + 6.0F;
      TextFieldWidget field = this.nameField();
      if (field == null) {
         return;
      }
      field.setX((int) fieldX);
      field.setY((int) y);
      DrawUtil.drawRoundedRect(draw.getMatrices(), fieldX - 1.0F, y - 1.0F, field.getWidth() + 2.0F, field.getHeight() + 2.0F, BorderRadius.all(3.0F), new ColorRGBA(25, 25, 25).withAlpha(170));
      if (field.isFocused()) {
         DrawUtil.drawRoundedBorder(draw.getMatrices(), fieldX - 1.0F, y - 1.0F, field.getWidth() + 2.0F, field.getHeight() + 2.0F, 1.0F, BorderRadius.all(3.0F), theme.getColor().withAlpha(255));
      }
      field.render(draw, (int) mouseX, (int) mouseY, 0.0F);

      float buttonY = y;
      float nextX = fieldX + field.getWidth() + 6.0F;
      nextX = this.renderBottomButton(draw, theme, mouseX, mouseY, nextX, buttonY, 130.0F, "Сохранить таргетхуд", this::savePreset);
      nextX = this.renderBottomButton(draw, theme, mouseX, mouseY, nextX, buttonY, 110.0F, "Сбросить позицию", () -> {
         this.module.x.setCurrent(4.0F);
         this.module.y.setCurrent(4.0F);
      });
      this.renderBottomButton(draw, theme, mouseX, mouseY, nextX, buttonY, 120.0F, "Сбросить настройки", this::resetAll);
   }

   private float renderBottomButton(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float x, float y, float width, String label, Runnable action) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 16.0F, BorderRadius.all(3.0F), hovered ? theme.getColor().withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + width / 2.0F - Fonts.REGULAR.getWidth(label, 5.0F) / 2.0F, y + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      return x + width + 6.0F;
   }

   private void renderSlider(CustomDrawContext draw, Theme theme, float labelY, float trackY, String label, NumberSetting setting) {
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, this.sliderTrackX(), labelY, new ColorRGBA(153, 153, 153).withAlpha(255));
      String value = String.format(Locale.US, "%.2f", setting.getCurrent());
      draw.drawText(Fonts.REGULAR.getFont(5.5F), value, this.panelX() + PANEL_WIDTH - PANEL_PADDING - Fonts.REGULAR.getWidth(value, 5.5F), labelY, new ColorRGBA(153, 153, 153).withAlpha(255));

      float percent = (setting.getCurrent() - setting.getMin()) / (setting.getMax() - setting.getMin());
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.sliderTrackX(), trackY, this.sliderTrackWidth(), 2.0F, BorderRadius.all(1.0F), new ColorRGBA(55, 55, 55).withAlpha(160));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.sliderTrackX(), trackY, this.sliderTrackWidth() * percent, 2.0F, BorderRadius.all(1.0F), theme.getColor().withAlpha(255));
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.sliderTrackX() + this.sliderTrackWidth() * percent - 4.0F, trackY - 3.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), new ColorRGBA(222, 222, 222).withAlpha(255));
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

      float centerX = this.width / 2.0F;
      float y = 46.0F;
      float pillX = centerX - 110.0F;
      float arrowWidth = 20.0F;
      if (MathUtil.isHovered(mouseX, mouseY, pillX - arrowWidth - 4.0F, y, arrowWidth, 16.0F)) {
         this.cycleType(false);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, pillX + 220.0F + 4.0F, y, arrowWidth, 16.0F) || MathUtil.isHovered(mouseX, mouseY, pillX, y, 220.0F, 16.0F)) {
         this.cycleType(true);
         return true;
      }

      if (this.handlePresetListClick(mouseX, mouseY)) {
         return true;
      }

      TextFieldWidget field = this.nameField();
      if (field != null) {
         float fieldX = 10.0F + Fonts.REGULAR.getWidth("Название:", 5.5F) + 6.0F;
         boolean inside = mouseX >= fieldX && mouseX <= fieldX + field.getWidth() && mouseY >= this.bottomBarY() && mouseY <= this.bottomBarY() + field.getHeight();
         field.setFocused(inside);
         if (inside) {
            field.mouseClicked(mouseX, mouseY, button);
            return true;
         }
      }
      if (this.handleBottomBarClick(mouseX, mouseY)) {
         return true;
      }

      for (int i = 0; i < MODES.length; i++) {
         if (MathUtil.isHovered(mouseX, mouseY, this.modeButtonX(i), this.modeRowY(), this.modeButtonWidth(), 16.0F)) {
            this.module.displayMode.set(MODES[i]);
            return true;
         }
      }

      if (MathUtil.isHovered(mouseX, mouseY, this.panelX() + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.autoRotateToggleY(), 50.0F, 16.0F)) {
         this.module.headAutoRotate.setEnabled(!this.module.headAutoRotate.isEnabled());
         return true;
      }

      if (MathUtil.isHovered(mouseX, mouseY, this.panelX() + PANEL_WIDTH - PANEL_PADDING - 50.0F, this.customToggleY(), 50.0F, 16.0F)) {
         this.module.customColors.setEnabled(!this.module.customColors.isEnabled());
         return true;
      }

      ColorSetting[] colors = {this.module.barColor, this.module.bgColor, this.module.borderColor, this.module.textColor};
      for (int i = 0; i < colors.length; i++) {
         if (MathUtil.isHovered(mouseX, mouseY, this.panelX() + PANEL_PADDING, this.colorRowY(i), this.sliderTrackWidth(), 16.0F)) {
            if (this.activeColor == colors[i]) {
               this.activeColor = null;
            } else {
               this.activeColor = colors[i];
               this.initHsb(colors[i]);
            }
            return true;
         }
      }

      if (this.activeColor != null) {
         if (MathUtil.isHovered(mouseX, mouseY, this.hueSliderX() - 2.0F, this.pickerY(), 8.0F, PICKER_HEIGHT)) {
            this.draggingHue = true;
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, this.pickerX(), this.pickerY(), this.pickerWidth(), PICKER_HEIGHT)) {
            this.draggingPicker = true;
            return true;
         }
      }

      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.thicknessTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.dragTarget = DragTarget.THICKNESS;
         this.updateSliderValue(mouseX);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headSizeTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.dragTarget = DragTarget.HEAD_SIZE;
         this.updateSliderValue(mouseX);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headYawTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.dragTarget = DragTarget.HEAD_YAW;
         this.updateSliderValue(mouseX);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headPitchTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.dragTarget = DragTarget.HEAD_PITCH;
         this.updateSliderValue(mouseX);
         return true;
      }

      float[] size = this.module.currentSize();
      float hx = this.module.x.getCurrent();
      float hy = this.module.y.getCurrent();
      if (MathUtil.isHovered(mouseX, mouseY, hx, hy, size[0], size[1])) {
         this.dragging = true;
         this.dragOffsetX = (float) mouseX - hx;
         this.dragOffsetY = (float) mouseY - hy;
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean handlePresetListClick(double mouseX, double mouseY) {
      float x = this.listX();
      float y = this.listY();
      if (mouseX < x || mouseX > x + LIST_WIDTH || mouseY < y || mouseY > y + this.listHeight()) {
         return false;
      }
      List<TargetHudPreset> presets = this.presetManager.getPresets();
      float contentY = y + 28.0F;
      for (int i = 0; i < presets.size(); i++) {
         float rowY = contentY - this.scroll + i * 20.0F;
         if (mouseY < rowY || mouseY > rowY + 18.0F) {
            continue;
         }
         if (mouseX >= x + LIST_WIDTH - 24.0F && mouseX <= x + LIST_WIDTH - 8.0F) {
            this.presetManager.deletePreset(presets.get(i).getName());
            return true;
         }
         this.module.applyPreset(presets.get(i));
         TextFieldWidget field = this.nameField();
         if (field != null) {
            field.setText(presets.get(i).getName());
         }
         return true;
      }
      return false;
   }

   private boolean handleBottomBarClick(double mouseX, double mouseY) {
      float y = this.bottomBarY();
      String label = "Название:";
      float fieldX = 10.0F + Fonts.REGULAR.getWidth(label, 5.5F) + 6.0F;
      TextFieldWidget field = this.nameField();
      float nextX = field != null ? fieldX + field.getWidth() + 6.0F : fieldX + 140.0F + 6.0F;
      if (MathUtil.isHovered(mouseX, mouseY, nextX, y, 130.0F, 16.0F)) {
         this.savePreset();
         return true;
      }
      nextX += 130.0F + 6.0F;
      if (MathUtil.isHovered(mouseX, mouseY, nextX, y, 110.0F, 16.0F)) {
         this.module.x.setCurrent(4.0F);
         this.module.y.setCurrent(4.0F);
         return true;
      }
      nextX += 110.0F + 6.0F;
      if (MathUtil.isHovered(mouseX, mouseY, nextX, y, 120.0F, 16.0F)) {
         this.resetAll();
         return true;
      }
      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.dragTarget != DragTarget.NONE) {
         this.updateSliderValue(mouseX);
         return true;
      }
      if (this.draggingPicker || this.draggingHue) {
         return true;
      }
      if (this.dragging) {
         this.updatePosition(mouseX, mouseY);
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.dragTarget = DragTarget.NONE;
      this.dragging = false;
      this.draggingPicker = false;
      this.draggingHue = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (mouseX >= this.listX() && mouseX <= this.listX() + LIST_WIDTH && mouseY >= this.listY() && mouseY <= this.listY() + this.listHeight()) {
         this.scroll -= (float) verticalAmount * 12.0F;
         return true;
      }
      float centerX = this.width / 2.0F;
      float y = 46.0F;
      if (MathUtil.isHovered(mouseX, mouseY, centerX - 130.0F, y, 260.0F, 16.0F)) {
         this.cycleType(verticalAmount > 0.0D);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.thicknessTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.module.barThickness.setCurrent(MathHelper.clamp(this.module.barThickness.getCurrent() + (float) verticalAmount * this.module.barThickness.getIncrement(), this.module.barThickness.getMin(), this.module.barThickness.getMax()));
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headSizeTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.module.headSize.setCurrent(MathHelper.clamp(this.module.headSize.getCurrent() + (float) verticalAmount * this.module.headSize.getIncrement(), this.module.headSize.getMin(), this.module.headSize.getMax()));
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headYawTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.module.headYaw.setCurrent(MathHelper.clamp(this.module.headYaw.getCurrent() + (float) verticalAmount * this.module.headYaw.getIncrement(), this.module.headYaw.getMin(), this.module.headYaw.getMax()));
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.sliderTrackX(), this.headPitchTrackY() - 3.0F, this.sliderTrackWidth(), 8.0F)) {
         this.module.headPitch.setCurrent(MathHelper.clamp(this.module.headPitch.getCurrent() + (float) verticalAmount * this.module.headPitch.getIncrement(), this.module.headPitch.getMin(), this.module.headPitch.getMax()));
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      TextFieldWidget field = this.nameField();
      if (field != null && field.isFocused()) {
         field.keyPressed(keyCode, scanCode, modifiers);
         if (keyCode == 257) {
            this.savePreset();
         }
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      TextFieldWidget field = this.nameField();
      if (field != null && field.isFocused()) {
         field.charTyped(codePoint, modifiers);
         return true;
      }
      return super.charTyped(codePoint, modifiers);
   }

   private void savePreset() {
      TextFieldWidget field = this.nameField();
      String name = field != null ? field.getText().trim() : "";
      if (name.isEmpty()) {
         name = "Таргетхуд";
      }
      this.presetManager.savePreset(this.module.toPreset(name));
   }

   private void cycleType(boolean next) {
      int index = 0;
      for (int i = 0; i < PRESETS.length; i++) {
         if (this.module.type.is(PRESETS[i])) {
            index = i;
            break;
         }
      }
      int newIndex = next ? (index + 1) % PRESETS.length : (index - 1 + PRESETS.length) % PRESETS.length;
      this.module.type.set(PRESETS[newIndex]);
   }

   private void initHsb(ColorSetting color) {
      ColorRGBA c = color.getColor();
      this.hsb[0] = c.getHue();
      this.hsb[1] = c.getSaturation();
      this.hsb[2] = c.getBrightness();
   }

   private void applyColor() {
      if (this.activeColor != null) {
         this.activeColor.setColor(ColorRGBA.fromHSB(this.hsb[0], this.hsb[1], this.hsb[2]));
      }
   }

   private void updateSliderValue(double mouseX) {
      float percent = MathHelper.clamp((float) (mouseX - this.sliderTrackX()) / this.sliderTrackWidth(), 0.0F, 1.0F);
      if (this.dragTarget == DragTarget.THICKNESS) {
         this.setCurrent(this.module.barThickness, this.module.barThickness.getMin() + percent * (this.module.barThickness.getMax() - this.module.barThickness.getMin()));
      } else if (this.dragTarget == DragTarget.HEAD_SIZE) {
         this.setCurrent(this.module.headSize, this.module.headSize.getMin() + percent * (this.module.headSize.getMax() - this.module.headSize.getMin()));
      } else if (this.dragTarget == DragTarget.HEAD_YAW) {
         this.setCurrent(this.module.headYaw, this.module.headYaw.getMin() + percent * (this.module.headYaw.getMax() - this.module.headYaw.getMin()));
      } else if (this.dragTarget == DragTarget.HEAD_PITCH) {
         this.setCurrent(this.module.headPitch, this.module.headPitch.getMin() + percent * (this.module.headPitch.getMax() - this.module.headPitch.getMin()));
      }
   }

   private void updatePosition(double mouseX, double mouseY) {
      float[] size = this.module.currentSize();
      float scaledWidth = this.width;
      float scaledHeight = this.height;
      float newX = MathHelper.clamp((float) mouseX - this.dragOffsetX, 0.0F, Math.max(scaledWidth - size[0], 0.0F));
      float newY = MathHelper.clamp((float) mouseY - this.dragOffsetY, 0.0F, Math.max(scaledHeight - size[1], 0.0F));
      this.module.x.setCurrent(newX);
      this.module.y.setCurrent(newY);
   }

   private void resetAll() {
      this.module.x.setCurrent(4.0F);
      this.module.y.setCurrent(4.0F);
      this.module.barThickness.setCurrent(1.0F);
      this.module.headSize.setCurrent(1.0F);
      this.module.headYaw.setCurrent(0.0F);
      this.module.headPitch.setCurrent(0.0F);
      this.module.headAutoRotate.setEnabled(false);
      this.module.displayMode.set("Проценты и HP");
      this.module.customColors.setEnabled(false);
   }

   private static void setCurrent(NumberSetting setting, float value) {
      setting.setCurrent(MathHelper.clamp((float) MathUtil.round(value, setting.getIncrement()), setting.getMin(), setting.getMax()));
   }
}
