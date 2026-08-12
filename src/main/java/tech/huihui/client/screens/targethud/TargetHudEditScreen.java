package tech.huihui.client.screens.targethud;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.render.TargetHud;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class TargetHudEditScreen extends Screen implements IMinecraft {
   private static final String[] PRESETS = {"Крупный", "Маленький", "Проценты", "Большая полоса", "Вертикальный", "Метал", "Мини", "Градиент", "Боссбар", "Минимал", "Кружок"};
   private static final String[] MODES = {"Проценты и HP", "Проценты", "HP"};
   private static final String[] COLOR_NAMES = {"Цвет полоски", "Второй цвет полоски", "Цвет фона", "Цвет рамки", "Цвет текста"};
   private static final String[] TOGGLE_NAMES = {"Автоповорот головы", "Свои цвета", "Показывать броню", "Показывать пинг", "Глаза на голове", "Золотая полоска", "Полоски одного размера"};
   private static final String[] SECTION_NAMES = {"Полоска и голова", "Стиль", "Переключатели", "Отображение", "Цвета", "Изображения"};

   private static final ColorRGBA TEXT = new ColorRGBA(228, 228, 230);
   private static final ColorRGBA SUBTEXT = new ColorRGBA(148, 148, 156);
   private static final ColorRGBA DIM = new ColorRGBA(105, 105, 114);
   private static final ColorRGBA PANEL_BG = new ColorRGBA(13, 13, 16).withAlpha(225);
   private static final ColorRGBA ROW_BG = new ColorRGBA(30, 30, 35).withAlpha(180);
   private static final ColorRGBA ROW_HOVER = new ColorRGBA(52, 52, 60).withAlpha(210);
   private static final ColorRGBA INPUT_BG = new ColorRGBA(22, 22, 26).withAlpha(220);
   private static final ColorRGBA DANGER = new ColorRGBA(255, 84, 84);

   private static final float MARGIN = 10.0F;
   private static final float PAD = 12.0F;
   private static final float LIST_WIDTH = 250.0F;
   private static final float PANEL_WIDTH = 264.0F;
   private static final float EXIT_WIDTH = 58.0F;
   private static final float EXIT_HEIGHT = 18.0F;
   private static final float BOTTOM_BAR_HEIGHT = 24.0F;
   private static final float TOGGLE_WIDTH = 34.0F;
   private static final float TOGGLE_HEIGHT = 14.0F;
   private static final float MODE_HEIGHT = 18.0F;
   private static final float PICKER_HEIGHT = 62.0F;
   private static final float TYPE_PILL_Y = 48.0F;
   private static final float IMPORT_WIDTH = 380.0F;
   private static final float IMPORT_HEIGHT = 280.0F;

   private final TargetHud module;
   private final TargetHudPresetManager presetManager;

   private float listScroll;
   private float panelScroll;
   private float importScroll;
   private SettingsLayout layout;
   private String appliedName = "";
   private TextInput nameInput = new TextInput(24);
   private ColorSetting activeColor;
   private final float[] hsb = new float[3];
   private final float[] toggleAnim = new float[7];
   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private int dragSlider = -1;
   private boolean draggingPicker;
   private boolean draggingHue;
   private boolean importOpen;
   private StringSetting importSetting;
   private Path importDir;

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

   private Theme theme() {
      return HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
   }

   private ColorRGBA accent(Theme theme) {
      return theme.getColor();
   }

   private float panelX() {
      return this.width - PANEL_WIDTH - MARGIN;
   }

   private float panelY() {
      return 20.0F;
   }

   private float panelHeight() {
      return this.bottomBarY() - MARGIN - this.panelY();
   }

   private float listX() {
      return MARGIN;
   }

   private float listY() {
      return 78.0F;
   }

   private float listHeight() {
      return this.bottomBarY() - MARGIN - this.listY();
   }

   private float bottomBarY() {
      return this.height - BOTTOM_BAR_HEIGHT - MARGIN;
   }

   private float contentY(float offset) {
      return this.panelY() + 34.0F + offset - this.panelScroll;
   }

   private float trackX() {
      return this.panelX() + PAD;
   }

   private float trackWidth() {
      return PANEL_WIDTH - PAD * 2.0F;
   }

   private boolean inPanel(double mouseX, double mouseY) {
      return mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_WIDTH && mouseY >= this.panelY() + 34.0F && mouseY <= this.panelY() + this.panelHeight();
   }

   private boolean inList(double mouseX, double mouseY) {
      return mouseX >= this.listX() && mouseX <= this.listX() + LIST_WIDTH && mouseY >= this.listY() && mouseY <= this.listY() + this.listHeight();
   }

   private List<SliderEntry> sliders() {
      List<SliderEntry> list = new ArrayList();
      list.add(new SliderEntry("Толщина полоски", this.module.barThickness));
      if (this.module.showSecondBar.isEnabled() && !this.module.matchBarThickness.isEnabled()) {
         list.add(new SliderEntry("Толщина золотой полоски", this.module.secondBarThickness));
      }
      list.add(new SliderEntry("Размер иконки головы", this.module.headSize));
      list.add(new SliderEntry("Поворот головы", this.module.headYaw));
      list.add(new SliderEntry("Наклон головы", this.module.headPitch));
      list.add(new SliderEntry("Скругление", this.module.radius));
      list.add(new SliderEntry("Толщина рамки", this.module.borderThickness));
      list.add(new SliderEntry("Прозрачность фона", this.module.backgroundAlpha));
      list.add(new SliderEntry("Скорость анимации", this.module.animationSpeed));
      return list;
   }

   private BooleanSetting[] toggles() {
      return new BooleanSetting[]{this.module.headAutoRotate, this.module.customColors, this.module.showArmor, this.module.showPing, this.module.showEyes, this.module.showSecondBar, this.module.matchBarThickness};
   }

   private ColorSetting[] colors() {
      return new ColorSetting[]{this.module.barColor, this.module.barColorSecond, this.module.bgColor, this.module.borderColor, this.module.textColor};
   }

   private SettingsLayout buildLayout() {
      SettingsLayout l = new SettingsLayout();
      int extra = this.module.showSecondBar.isEnabled() && !this.module.matchBarThickness.isEnabled() ? 1 : 0;
      l.sliderY = new float[8 + extra];
      float y = 0.0F;
      for (int s = 0; s < 6; s++) {
         l.sectionY[s] = y + 8.0F;
         y = l.sectionY[s] + 18.0F;
         switch (s) {
            case 0, 1 -> {
               int start = s == 0 ? 0 : 4 + extra;
               int count = s == 0 ? 4 + extra : 4;
               for (int i = 0; i < count; i++) {
                  l.sliderY[start + i] = y;
                  y += 26.0F;
               }
            }
            case 2 -> {
               for (int i = 0; i < 7; i++) {
                  l.toggleY[i] = y;
                  y += 21.0F;
               }
            }
            case 3 -> {
               l.modeY = y + 10.0F;
               y = l.modeY + MODE_HEIGHT + 8.0F;
            }
            case 4 -> {
               for (int i = 0; i < 5; i++) {
                  l.colorY[i] = y;
                  y += 20.0F;
               }
               y += 8.0F;
               l.pickerY = y;
               y += PICKER_HEIGHT + 10.0F;
            }
            case 5 -> {
               for (int i = 0; i < 2; i++) {
                  l.imageY[i] = y;
                  l.imageStatusY[i] = y + 20.0F;
                  y += 38.0F;
               }
            }
         }
      }
      l.resetY = y + 6.0F;
      l.contentHeight = l.resetY + 26.0F;
      return l;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      Theme theme = this.theme();
      ColorRGBA accent = this.accent(theme);
      this.layout = this.buildLayout();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 125));

      this.renderHeader(draw, accent, mouseX, mouseY);
      this.renderTypeSelector(draw, accent, mouseX, mouseY);
      this.renderPresetList(draw, theme, accent, mouseX, mouseY);
      this.renderSettingsPanel(draw, theme, accent, mouseX, mouseY);
      this.renderBottomBar(draw, theme, accent, mouseX, mouseY);

      try {
         this.module.renderPreview(draw);
      } catch (Exception e) {
         e.printStackTrace();
      }
      float[] size = this.module.currentSize();
      float hx = this.module.x.getCurrent();
      float hy = this.module.y.getCurrent();
      if (this.dragging || MathUtil.isHovered(mouseX, mouseY, hx, hy, size[0], size[1])) {
         DrawUtil.drawRoundedBorder(draw.getMatrices(), hx - 1.0F, hy - 1.0F, size[0] + 2.0F, size[1] + 2.0F, 1.0F, BorderRadius.all(6.0F), accent.withAlpha(255));
      }

      if (this.importOpen) {
         this.renderImportDialog(draw, theme, mouseX, mouseY);
      }

      String pos = "X: " + String.format(Locale.US, "%.0f", this.module.x.getCurrent()) + " · Y: " + String.format(Locale.US, "%.0f", this.module.y.getCurrent());
      draw.drawText(Fonts.REGULAR.getFont(5.5F), pos, MARGIN, this.height - 14.0F, DIM);
   }

   private void renderHeader(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY) {
      draw.drawText(Fonts.SEMIBOLD.getFont(7.5F), "Редактор таргетхуда", MARGIN, 10.0F, TEXT);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Тащи таргетхуд мышью · колесо — прокрутка · Enter — сохранить", MARGIN, 29.0F, DIM);

      float x = this.width - MARGIN - EXIT_WIDTH;
      float y = MARGIN;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(4.0F), hovered ? accent.withAlpha(150) : new ColorRGBA(22, 22, 26).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, EXIT_WIDTH, EXIT_HEIGHT, 1.0F, BorderRadius.all(4.0F), hovered ? accent.withAlpha(170) : new ColorRGBA(70, 70, 78).withAlpha(90));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Выход", x + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 6.0F) / 2.0F, y + 6.0F, TEXT);
   }

   private void renderTypeSelector(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY) {
      float pillWidth = 240.0F;
      float arrowWidth = 22.0F;
      float x = this.width / 2.0F - pillWidth / 2.0F;
      float y = TYPE_PILL_Y;
      float leftX = x - arrowWidth - 4.0F;
      float rightX = x + pillWidth + 4.0F;
      boolean leftHovered = MathUtil.isHovered(mouseX, mouseY, leftX, y, arrowWidth, 18.0F);
      boolean pillHovered = MathUtil.isHovered(mouseX, mouseY, x, y, pillWidth, 18.0F);
      boolean rightHovered = MathUtil.isHovered(mouseX, mouseY, rightX, y, arrowWidth, 18.0F);

      DrawUtil.drawRoundedRect(draw.getMatrices(), leftX, y, arrowWidth, 18.0F, BorderRadius.all(4.0F), leftHovered ? accent.withAlpha(150) : new ColorRGBA(22, 22, 26).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), leftX, y, arrowWidth, 18.0F, 1.0F, BorderRadius.all(4.0F), leftHovered ? accent.withAlpha(170) : new ColorRGBA(70, 70, 78).withAlpha(90));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, pillWidth, 18.0F, BorderRadius.all(4.0F), pillHovered ? accent.withAlpha(70) : new ColorRGBA(22, 22, 26).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, pillWidth, 18.0F, 1.0F, BorderRadius.all(4.0F), accent.withAlpha(110));
      DrawUtil.drawRoundedRect(draw.getMatrices(), rightX, y, arrowWidth, 18.0F, BorderRadius.all(4.0F), rightHovered ? accent.withAlpha(150) : new ColorRGBA(22, 22, 26).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), rightX, y, arrowWidth, 18.0F, 1.0F, BorderRadius.all(4.0F), rightHovered ? accent.withAlpha(170) : new ColorRGBA(70, 70, 78).withAlpha(90));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "«", leftX + arrowWidth / 2.0F - Fonts.REGULAR.getWidth("«", 6.0F) / 2.0F, y + 5.0F, TEXT);
      String name = this.module.type.get();
      draw.drawText(Fonts.SEMIBOLD.getFont(6.5F), name, this.width / 2.0F - Fonts.SEMIBOLD.getWidth(name, 6.5F) / 2.0F, y + 5.0F, TEXT);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "»", rightX + arrowWidth / 2.0F - Fonts.REGULAR.getWidth("»", 6.0F) / 2.0F, y + 5.0F, TEXT);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Тип пресета", this.width / 2.0F - Fonts.REGULAR.getWidth("Тип пресета", 5.5F) / 2.0F, y - 13.0F, DIM);
   }

   private void renderPresetList(CustomDrawContext draw, Theme theme, ColorRGBA accent, float mouseX, float mouseY) {
      float x = this.listX();
      float y = this.listY();
      float w = LIST_WIDTH;
      float h = this.listHeight();
      float contentTop = y + 34.0F;

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(6.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(6.0F), theme.getSecondColor().darker(0.5F).withAlpha(160));
      draw.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Мои таргетхуды", x + PAD, y + 10.0F, TEXT);

      List<TargetHudPreset> presets = this.presetManager.getPresets();
      String count = String.valueOf(presets.size());
      float badgeW = Fonts.REGULAR.getWidth(count, 5.5F) + 12.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - PAD - badgeW, y + 8.0F, badgeW, 15.0F, BorderRadius.all(7.5F), accent.withAlpha(60));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), count, x + w - PAD - badgeW + 6.0F, y + 10.0F, SUBTEXT);

      float visibleH = h - 34.0F - 38.0F;
      float contentH = presets.size() * 22.0F;
      this.listScroll = MathHelper.clamp(this.listScroll, 0.0F, Math.max(contentH - visibleH, 0.0F));

      if (presets.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(5.5F), "Пусто — создай и сохрани свой таргетхуд", x + PAD, contentTop + 8.0F, DIM);
      } else {
         draw.enableScissor((int) x, (int) (contentTop - 2), (int) (x + w), (int) (contentTop + visibleH + 2));
         for (int i = 0; i < presets.size(); i++) {
            TargetHudPreset preset = presets.get(i);
            float rowY = contentTop - this.listScroll + i * 22.0F;
            if (rowY + 20.0F < contentTop - 2 || rowY > contentTop + visibleH + 2) {
               continue;
            }
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 5.0F, rowY, w - 10.0F, 20.0F);
            boolean applied = preset.getName().equals(this.appliedName);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, rowY, w - 10.0F, 20.0F, BorderRadius.all(4.0F), applied ? accent.withAlpha(55) : hovered ? ROW_HOVER : ROW_BG);
            if (applied) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), x + 8.0F, rowY + 4.0F, 3.0F, 12.0F, BorderRadius.all(1.5F), accent.withAlpha(230));
            }
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 16.0F, rowY + 6.0F, 8.0F, 8.0F, BorderRadius.all(2.5F), new ColorRGBA(preset.getBarColor()));
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x + 16.0F, rowY + 6.0F, 8.0F, 8.0F, 1.0F, BorderRadius.all(2.5F), new ColorRGBA(0, 0, 0).withAlpha(120));
            draw.drawText(Fonts.REGULAR.getFont(5.5F), preset.getName(), x + 30.0F, rowY + 6.0F, TEXT);
            boolean delHovered = MathUtil.isHovered(mouseX, mouseY, x + w - 25.0F, rowY, 16.0F, 20.0F);
            draw.drawText(Fonts.REGULAR.getFont(5.5F), "✕", x + w - 17.0F - Fonts.REGULAR.getWidth("✕", 5.5F) / 2.0F, rowY + 6.0F, delHovered ? new ColorRGBA(255, 180, 180) : new ColorRGBA(130, 130, 140));
         }
         draw.disableScissor();
         this.renderScrollbar(draw, x + w - 7.0F, contentTop, visibleH, this.listScroll, contentH, visibleH, accent);
      }

      float buttonY = y + h - 28.0F;
      boolean resetHovered = MathUtil.isHovered(mouseX, mouseY, x + PAD, buttonY, w - PAD * 2.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, buttonY, w - PAD * 2.0F, 18.0F, BorderRadius.all(4.0F), resetHovered ? accent.withAlpha(130) : new ColorRGBA(42, 42, 48).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Сбросить позицию", x + w / 2.0F - Fonts.REGULAR.getWidth("Сбросить позицию", 6.0F) / 2.0F, buttonY + 6.0F, TEXT);
   }

   private void renderSettingsPanel(CustomDrawContext draw, Theme theme, ColorRGBA accent, float mouseX, float mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      float boxH = this.panelHeight();
      SettingsLayout l = this.layout;
      this.panelScroll = MathHelper.clamp(this.panelScroll, 0.0F, Math.max(l.contentHeight - (boxH - 40.0F), 0.0F));

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, BorderRadius.all(6.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, 1.0F, BorderRadius.all(6.0F), theme.getSecondColor().darker(0.5F).withAlpha(160));
      draw.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Настройки", x + PAD, y + 10.0F, TEXT);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Колесо над панелью — прокрутка", x + PAD, y + 23.0F, DIM);

      draw.enableScissor((int) x, (int) (y + 34.0F), (int) (x + PANEL_WIDTH), (int) (y + boxH));
      this.renderPanelContent(draw, accent, mouseX, mouseY, l);
      draw.disableScissor();

      this.renderScrollbar(draw, x + PANEL_WIDTH - 6.0F, y + 40.0F, boxH - 40.0F, this.panelScroll, l.contentHeight, boxH - 40.0F, accent);
   }

   private void renderPanelContent(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY, SettingsLayout l) {
      float x = this.panelX();

      for (int s = 0; s < 6; s++) {
         float headerY = this.contentY(l.sectionY[s]);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, headerY + 1.0F, 3.0F, 12.0F, BorderRadius.all(1.5F), accent.withAlpha(230));
         draw.drawText(Fonts.REGULAR.getFont(6.0F), SECTION_NAMES[s].toUpperCase(Locale.ROOT), x + PAD + 8.0F, headerY + 1.0F, new ColorRGBA(172, 172, 180));
      }

      List<SliderEntry> sliders = this.sliders();
      for (int i = 0; i < sliders.size(); i++) {
         this.renderSlider(draw, accent, this.contentY(l.sliderY[i]), sliders.get(i), this.dragSlider == i);
      }

      BooleanSetting[] toggles = this.toggles();
      for (int i = 0; i < toggles.length; i++) {
         float rowY = this.contentY(l.toggleY[i]);
         boolean active = i != 6 || this.module.showSecondBar.isEnabled();
         boolean enabled = toggles[i].isEnabled() && active;
         this.toggleAnim[i] += ((enabled ? 1.0F : 0.0F) - this.toggleAnim[i]) * Math.min(1.0F, mc.getRenderTickCounter().getTickDelta(false) * 25.0F);
         draw.drawText(Fonts.REGULAR.getFont(5.5F), TOGGLE_NAMES[i], x + PAD, rowY + 4.0F, SUBTEXT.withAlpha(active ? 255 : 120));
         float tx = x + PANEL_WIDTH - PAD - TOGGLE_WIDTH;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, tx, rowY + 2.0F, TOGGLE_WIDTH, TOGGLE_HEIGHT) && active;
         DrawUtil.drawRoundedRect(draw.getMatrices(), tx, rowY + 2.0F, TOGGLE_WIDTH, TOGGLE_HEIGHT, BorderRadius.all(TOGGLE_HEIGHT / 2.0F), enabled ? accent.withAlpha(hovered ? 255 : 225) : hovered ? new ColorRGBA(62, 62, 70).withAlpha(255) : new ColorRGBA(48, 48, 54).withAlpha(active ? 255 : 140));
         float knobR = 5.0F;
         float knobX = tx + 2.0F + this.toggleAnim[i] * (TOGGLE_WIDTH - 4.0F - knobR * 2.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), knobX, rowY + 2.0F + 2.0F, knobR * 2.0F, knobR * 2.0F, BorderRadius.all(knobR), ColorRGBA.WHITE.withAlpha(active ? 255 : 130));
      }

      float modeY = this.contentY(l.modeY);
      float modeW = (PANEL_WIDTH - PAD * 2.0F - 8.0F) / 3.0F;
      for (int i = 0; i < MODES.length; i++) {
         float bx = x + PAD + i * (modeW + 4.0F);
         boolean selected = this.module.displayMode.is(MODES[i]);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, bx, modeY, modeW, MODE_HEIGHT);
         DrawUtil.drawRoundedRect(draw.getMatrices(), bx, modeY, modeW, MODE_HEIGHT, BorderRadius.all(4.0F), selected ? accent.withAlpha(190) : hovered ? accent.withAlpha(80) : new ColorRGBA(42, 42, 48).withAlpha(255));
         if (selected) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), bx, modeY, modeW, MODE_HEIGHT, 1.0F, BorderRadius.all(4.0F), accent.brighter(0.15F).withAlpha(140));
         }
         float labelW = Fonts.REGULAR.getWidth(MODES[i], 5.5F);
         draw.drawText(Fonts.REGULAR.getFont(5.5F), MODES[i], bx + modeW / 2.0F - labelW / 2.0F, modeY + 6.0F, TEXT);
      }

      ColorSetting[] colors = this.colors();
      float alpha = this.module.customColors.isEnabled() ? 1.0F : 0.35F;
      for (int i = 0; i < colors.length; i++) {
         float rowY = this.contentY(l.colorY[i]);
         boolean active = this.activeColor == colors[i];
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + PAD, rowY, this.trackWidth(), 18.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, rowY, this.trackWidth(), 18.0F, BorderRadius.all(4.0F), active ? accent.withAlpha(70) : hovered ? ROW_HOVER : ROW_BG);
         if (active) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, rowY + 3.0F, 3.0F, 12.0F, BorderRadius.all(1.5F), accent.withAlpha(230));
         }
         draw.drawText(Fonts.REGULAR.getFont(5.5F), COLOR_NAMES[i], x + PAD + 9.0F, rowY + 5.0F, TEXT.withAlpha(255.0F * alpha));
         float swatchX = x + PANEL_WIDTH - PAD - 16.0F;
         DrawUtil.drawRoundedRect(draw.getMatrices(), swatchX, rowY + 2.0F, 14.0F, 14.0F, BorderRadius.all(7.0F), colors[i].getColor().withAlpha(255.0F * alpha));
         DrawUtil.drawRoundedBorder(draw.getMatrices(), swatchX, rowY + 2.0F, 14.0F, 14.0F, 1.0F, BorderRadius.all(7.0F), new ColorRGBA(0, 0, 0).withAlpha(140));
      }

      if (this.activeColor != null) {
         this.renderPicker(draw, accent, mouseX, mouseY, l, alpha);
      }

      this.renderImageRow(draw, accent, mouseX, mouseY, l, 0, "Импорт фона", this.module.bgImage);
      this.renderImageRow(draw, accent, mouseX, mouseY, l, 1, "Импорт головы", this.module.headImage);

      float resetY = this.contentY(l.resetY);
      boolean resetHovered = MathUtil.isHovered(mouseX, mouseY, x + PAD, resetY, this.trackWidth(), 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, resetY, this.trackWidth(), 18.0F, BorderRadius.all(4.0F), resetHovered ? DANGER.withAlpha(180) : new ColorRGBA(42, 42, 48).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Сбросить настройки", x + PANEL_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Сбросить настройки", 6.0F) / 2.0F, resetY + 6.0F, TEXT);
   }

   private void renderSlider(CustomDrawContext draw, ColorRGBA accent, float rowY, SliderEntry entry, boolean dragging) {
      float x = this.panelX();
      float trackW = this.trackWidth();
      float value = entry.setting().getCurrent();
      String label = entry.name();
      String valueText = String.format(Locale.US, "%.2f", value);
      float valueW = Fonts.REGULAR.getWidth(valueText, 5.5F);

      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + PAD, rowY, SUBTEXT);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), valueText, x + PANEL_WIDTH - PAD - valueW, rowY, TEXT);

      float percent = (value - entry.setting().getMin()) / (entry.setting().getMax() - entry.setting().getMin());
      float trackY = rowY + 14.0F;
      float trackH = 2.5F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, trackY, trackW, trackH, BorderRadius.all(1.25F), new ColorRGBA(58, 58, 64).withAlpha(200));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, trackY, trackW * percent, trackH, BorderRadius.all(1.25F), accent.withAlpha(dragging ? 255 : 220));

      float knobX = x + PAD + trackW * percent - 4.5F;
      float knobY = trackY - 3.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX, knobY, 9.0F, 9.0F, BorderRadius.all(4.5F), ColorRGBA.WHITE);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), knobX, knobY, 9.0F, 9.0F, 1.0F, BorderRadius.all(4.5F), accent.withAlpha(dragging ? 255 : 180));
   }

   private void renderPicker(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY, SettingsLayout l, float alpha) {
      float py = this.contentY(l.pickerY);
      float x = this.panelX() + PAD;
      float pickerW = this.trackWidth() - 26.0F;
      if (this.draggingPicker) {
         float saturation = MathHelper.clamp((mouseX - x - 4.0F) / (pickerW - 8.0F), 0.0F, 1.0F);
         float brightness = 1.0F - MathHelper.clamp((mouseY - py - 4.0F) / (PICKER_HEIGHT - 8.0F), 0.0F, 1.0F);
         this.hsb[1] = saturation;
         this.hsb[2] = brightness;
         this.applyColor();
      }
      if (this.draggingHue) {
         this.hsb[0] = MathHelper.clamp((mouseY - py) / PICKER_HEIGHT, 0.0F, 1.0F);
         this.applyColor();
      }

      ColorRGBA hue = ColorRGBA.fromHSB(this.hsb[0], 1.0F, 1.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, py, pickerW, PICKER_HEIGHT, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), hue.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, py, pickerW, PICKER_HEIGHT, 1.0F, BorderRadius.all(6.0F), new ColorRGBA(0, 0, 0).withAlpha(160));

      float knobX = x + 4.0F + this.hsb[1] * (pickerW - 8.0F);
      float knobY = py + 4.0F + (1.0F - this.hsb[2]) * (PICKER_HEIGHT - 8.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(180.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));

      float hueX = x + pickerW + 8.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), hueX - 4.0F, py, 14.0F, PICKER_HEIGHT, BorderRadius.all(7.0F), new ColorRGBA(20, 20, 24).withAlpha(255.0F * alpha));
      for (int i = 0; i < (int) PICKER_HEIGHT; i++) {
         DrawUtil.drawRect(draw.getMatrices(), hueX - 2.0F, py + (float) i, 10.0F, 1.0F, ColorRGBA.fromHSB((float) i / PICKER_HEIGHT, 1.0F, 1.0F).withAlpha(255.0F * alpha));
      }
      float knobY2 = py + this.hsb[0] * PICKER_HEIGHT;
      DrawUtil.drawRoundedBorder(draw.getMatrices(), hueX - 4.0F, py, 14.0F, PICKER_HEIGHT, 1.0F, BorderRadius.all(7.0F), new ColorRGBA(0, 0, 0).withAlpha(140));
      DrawUtil.drawRoundedRect(draw.getMatrices(), hueX - 3.0F, knobY2 - 3.0F, 12.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
   }

   private void renderImageRow(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY, SettingsLayout l, int index, String label, StringSetting setting) {
      float y = this.contentY(l.imageY[index]);
      float x = this.panelX();
      String value = setting.getValue();
      boolean hasImage = !value.isEmpty();
      boolean error = this.module.hasImageError(value);
      float importW = 120.0F;
      float clearW = 28.0F;
      boolean importHovered = MathUtil.isHovered(mouseX, mouseY, x + PAD, y, importW, 18.0F);
      boolean clearHovered = MathUtil.isHovered(mouseX, mouseY, x + PAD + importW + 4.0F, y, clearW, 18.0F);
      ColorRGBA importBg;
      if (error) {
         importBg = DANGER.withAlpha(importHovered ? 255 : 210);
      } else if (hasImage) {
         importBg = accent.withAlpha(importHovered ? 255 : 200);
      } else {
         importBg = importHovered ? accent.withAlpha(110) : new ColorRGBA(42, 42, 48).withAlpha(255);
      }
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, y, importW, 18.0F, BorderRadius.all(4.0F), importBg);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + PAD + importW / 2.0F - Fonts.REGULAR.getWidth(label, 5.5F) / 2.0F, y + 5.0F, TEXT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD + importW + 4.0F, y, clearW, 18.0F, BorderRadius.all(4.0F), clearHovered ? DANGER.withAlpha(210) : new ColorRGBA(42, 42, 48).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "✕", x + PAD + importW + 4.0F + clearW / 2.0F - Fonts.REGULAR.getWidth("✕", 5.5F) / 2.0F, y + 5.0F, TEXT);

      String status;
      ColorRGBA statusColor;
      if (value.isEmpty()) {
         status = "не выбрано";
         statusColor = DIM;
      } else if (error) {
         status = "ошибка загрузки: " + this.trimPath(value);
         statusColor = DANGER;
      } else {
         status = this.trimPath(value);
         statusColor = new ColorRGBA(150, 220, 150);
      }
      draw.drawText(Fonts.REGULAR.getFont(5.0F), status, x + PAD, this.contentY(l.imageStatusY[index]), statusColor);
   }

   private void renderBottomBar(CustomDrawContext draw, Theme theme, ColorRGBA accent, float mouseX, float mouseY) {
      float y = this.bottomBarY();
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Название:", MARGIN, y + 6.0F, SUBTEXT);
      float fieldX = MARGIN + Fonts.REGULAR.getWidth("Название:", 6.0F) + 8.0F;
      float fieldW = 160.0F;
      float fieldH = 18.0F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, fieldX, y, fieldW, fieldH);
      DrawUtil.drawRoundedRect(draw.getMatrices(), fieldX, y, fieldW, fieldH, BorderRadius.all(4.0F), INPUT_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), fieldX, y, fieldW, fieldH, 1.0F, BorderRadius.all(4.0F), this.nameInput.focused ? accent.withAlpha(220) : hovered ? new ColorRGBA(100, 100, 110).withAlpha(110) : new ColorRGBA(70, 70, 78).withAlpha(90));
      TextInput input = this.nameInput;
      if (input.text.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(6.0F), input.focused ? "" : "Название таргетхуда", fieldX + 6.0F, y + 6.0F, DIM);
      } else {
         draw.drawText(Fonts.REGULAR.getFont(6.0F), input.text, fieldX + 6.0F, y + 6.0F, TEXT);
      }
      if (input.focused && System.currentTimeMillis() % 1000L < 500L) {
         float caretX = fieldX + 6.0F + Fonts.REGULAR.getWidth(input.text, 6.0F) + 2.0F;
         DrawUtil.drawRect(draw.getMatrices(), caretX, y + 5.0F, 1.0F, 9.0F, TEXT);
      }

      float buttonX = fieldX + fieldW + 6.0F;
      boolean saveHovered = MathUtil.isHovered(mouseX, mouseY, buttonX, y, 170.0F, fieldH);
      DrawUtil.drawRoundedRect(draw.getMatrices(), buttonX, y, 170.0F, fieldH, BorderRadius.all(4.0F), accent.withAlpha(saveHovered ? 255 : 150));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), buttonX, y, 170.0F, fieldH, 1.0F, BorderRadius.all(4.0F), accent.brighter(0.2F).withAlpha(saveHovered ? 190 : 120));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Сохранить таргетхуд", buttonX + 85.0F - Fonts.REGULAR.getWidth("Сохранить таргетхуд", 6.0F) / 2.0F, y + 6.0F, TEXT);
   }

   private void renderScrollbar(CustomDrawContext draw, float x, float y, float h, float scroll, float contentH, float visibleH, ColorRGBA accent) {
      if (contentH <= visibleH + 0.5F) {
         return;
      }
      float thumbH = Math.max(24.0F, visibleH * (visibleH / contentH));
      float maxScroll = contentH - visibleH;
      float thumbY = y + (h - thumbH) * (scroll / maxScroll);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, 4.0F, h, BorderRadius.all(2.0F), new ColorRGBA(255, 255, 255).withAlpha(24));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, thumbY, 4.0F, thumbH, BorderRadius.all(2.0F), accent.withAlpha(170));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.importOpen) {
         if (button == 0) {
            this.handleImportClick(mouseX, mouseY);
         }
         return true;
      }
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }
      this.nameInput.focused = false;

      float x = this.width - MARGIN - EXIT_WIDTH;
      if (MathUtil.isHovered(mouseX, mouseY, x, MARGIN, EXIT_WIDTH, EXIT_HEIGHT)) {
         this.close();
         return true;
      }

      if (this.handleTypeSelectorClick(mouseX, mouseY)) {
         return true;
      }
      if (this.handlePresetListClick(mouseX, mouseY)) {
         return true;
      }
      if (this.handleBottomBarClick(mouseX, mouseY)) {
         return true;
      }
      if (this.handlePanelClick(mouseX, mouseY)) {
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

   private boolean handleTypeSelectorClick(double mouseX, double mouseY) {
      float pillWidth = 240.0F;
      float arrowWidth = 22.0F;
      float x = this.width / 2.0F - pillWidth / 2.0F;
      float y = TYPE_PILL_Y;
      if (MathUtil.isHovered(mouseX, mouseY, x - arrowWidth - 4.0F, y, arrowWidth, 18.0F)) {
         this.cycleType(false);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + pillWidth + 4.0F, y, arrowWidth, 18.0F) || MathUtil.isHovered(mouseX, mouseY, x, y, pillWidth, 18.0F)) {
         this.cycleType(true);
         return true;
      }
      return false;
   }

   private boolean handlePresetListClick(double mouseX, double mouseY) {
      if (!this.inList(mouseX, mouseY)) {
         return false;
      }
      List<TargetHudPreset> presets = this.presetManager.getPresets();
      float contentTop = this.listY() + 34.0F;
      for (int i = 0; i < presets.size(); i++) {
         float rowY = contentTop - this.listScroll + i * 22.0F;
         if (mouseY < rowY || mouseY > rowY + 20.0F) {
            continue;
         }
         if (mouseX >= this.listX() + LIST_WIDTH - 25.0F && mouseX <= this.listX() + LIST_WIDTH - 9.0F) {
            this.presetManager.deletePreset(presets.get(i).getName());
            return true;
         }
         this.module.applyPreset(presets.get(i));
         this.appliedName = presets.get(i).getName();
         this.nameInput.text = presets.get(i).getName();
         return true;
      }
      float buttonY = this.listY() + this.listHeight() - 28.0F;
      if (MathUtil.isHovered(mouseX, mouseY, this.listX() + PAD, buttonY, LIST_WIDTH - PAD * 2.0F, 18.0F)) {
         this.module.x.setCurrent(4.0F);
         this.module.y.setCurrent(4.0F);
         return true;
      }
      return false;
   }

   private boolean handleBottomBarClick(double mouseX, double mouseY) {
      float y = this.bottomBarY();
      float fieldX = MARGIN + Fonts.REGULAR.getWidth("Название:", 6.0F) + 8.0F;
      float fieldW = 160.0F;
      if (MathUtil.isHovered(mouseX, mouseY, fieldX, y, fieldW, 18.0F)) {
         this.nameInput.focused = true;
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, fieldX + fieldW + 6.0F, y, 170.0F, 18.0F)) {
         this.savePreset();
         return true;
      }
      return false;
   }

   private boolean handlePanelClick(double mouseX, double mouseY) {
      if (!this.inPanel(mouseX, mouseY)) {
         return false;
      }
      SettingsLayout l = this.layout;
      float x = this.panelX();

      float modeY = this.contentY(l.modeY);
      float modeW = (PANEL_WIDTH - PAD * 2.0F - 8.0F) / 3.0F;
      for (int i = 0; i < MODES.length; i++) {
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD + i * (modeW + 4.0F), modeY, modeW, MODE_HEIGHT)) {
            this.module.displayMode.set(MODES[i]);
            return true;
         }
      }

      BooleanSetting[] toggles = this.toggles();
      for (int i = 0; i < toggles.length; i++) {
         if (i == 6 && !this.module.showSecondBar.isEnabled()) {
            continue;
         }
         float ty = this.contentY(l.toggleY[i]);
         if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_WIDTH - PAD - TOGGLE_WIDTH, ty + 2.0F, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            toggles[i].setEnabled(!toggles[i].isEnabled());
            return true;
         }
      }

      ColorSetting[] colors = this.colors();
      for (int i = 0; i < colors.length; i++) {
         float rowY = this.contentY(l.colorY[i]);
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD, rowY, this.trackWidth(), 18.0F)) {
            if (this.activeColor == colors[i]) {
               this.activeColor = null;
            } else {
               this.activeColor = colors[i];
               this.initHsb(colors[i]);
               float pickerBottom = this.contentY(l.pickerY) + PICKER_HEIGHT;
               float maxScroll = Math.max(this.layout.contentHeight - (this.panelHeight() - 40.0F), 0.0F);
               if (pickerBottom > this.panelY() + this.panelHeight()) {
                  this.panelScroll = Math.min(this.panelScroll + pickerBottom - (this.panelY() + this.panelHeight()) + 10.0F, maxScroll);
               }
            }
            return true;
         }
      }

      if (this.activeColor != null) {
         float py = this.contentY(l.pickerY);
         float pickerW = this.trackWidth() - 26.0F;
         float hueX = x + PAD + pickerW + 8.0F;
         if (MathUtil.isHovered(mouseX, mouseY, hueX - 4.0F, py, 14.0F, PICKER_HEIGHT)) {
            this.draggingHue = true;
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD, py, pickerW, PICKER_HEIGHT)) {
            this.draggingPicker = true;
            return true;
         }
      }

      for (int i = 0; i < 2; i++) {
         float rowY = this.contentY(l.imageY[i]);
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD, rowY, 120.0F, 18.0F)) {
            this.openImport(i == 0 ? this.module.bgImage : this.module.headImage);
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD + 124.0F, rowY, 28.0F, 18.0F)) {
            if (i == 0) {
               this.module.bgImage.setValue("");
            } else {
               this.module.headImage.setValue("");
            }
            this.module.clearImageCache();
            return true;
         }
      }

      List<SliderEntry> sliders = this.sliders();
      for (int i = 0; i < sliders.size(); i++) {
         float rowY = this.contentY(l.sliderY[i]);
         if (MathUtil.isHovered(mouseX, mouseY, x + PAD, rowY, this.trackWidth(), 18.0F)) {
            this.dragSlider = i;
            this.updateSliderValue(mouseX);
            return true;
         }
      }

      float resetY = this.contentY(l.resetY);
      if (MathUtil.isHovered(mouseX, mouseY, x + PAD, resetY, this.trackWidth(), 18.0F)) {
         this.resetAll();
         return true;
      }
      return false;
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.importOpen) {
         return true;
      }
      if (this.dragSlider >= 0) {
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
      this.dragSlider = -1;
      this.dragging = false;
      this.draggingPicker = false;
      this.draggingHue = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.importOpen) {
         this.importScroll -= (float) verticalAmount * 12.0F;
         return true;
      }
      if (this.inList(mouseX, mouseY)) {
         this.listScroll -= (float) verticalAmount * 12.0F;
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, this.width / 2.0F - 130.0F, TYPE_PILL_Y, 260.0F, 18.0F)) {
         this.cycleType(verticalAmount > 0.0D);
         return true;
      }
      if (this.inPanel(mouseX, mouseY)) {
         this.panelScroll -= (float) verticalAmount * 12.0F;
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.importOpen) {
         if (keyCode == 256) {
            this.importOpen = false;
         }
         return true;
      }
      if (this.nameInput.focused) {
         if (keyCode == 257) {
            this.savePreset();
            return true;
         }
         this.nameInput.handleKey(keyCode);
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      if (this.nameInput.focused) {
         this.nameInput.handleChar(codePoint);
         return true;
      }
      return super.charTyped(codePoint, modifiers);
   }

   private void savePreset() {
      String name = this.nameInput.text.trim();
      if (name.isEmpty()) {
         name = "Таргетхуд";
      }
      this.nameInput.text = name;
      this.appliedName = name;
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
      List<SliderEntry> sliders = this.sliders();
      if (this.dragSlider < 0 || this.dragSlider >= sliders.size()) {
         return;
      }
      NumberSetting setting = sliders.get(this.dragSlider).setting();
      float percent = MathHelper.clamp((float) (mouseX - this.trackX()) / this.trackWidth(), 0.0F, 1.0F);
      setting.setCurrent(MathHelper.clamp((float) MathUtil.round(setting.getMin() + percent * (setting.getMax() - setting.getMin()), setting.getIncrement()), setting.getMin(), setting.getMax()));
   }

   private void updatePosition(double mouseX, double mouseY) {
      float[] size = this.module.currentSize();
      this.module.x.setCurrent(MathHelper.clamp((float) mouseX - this.dragOffsetX, 0.0F, Math.max(this.width - size[0], 0.0F)));
      this.module.y.setCurrent(MathHelper.clamp((float) mouseY - this.dragOffsetY, 0.0F, Math.max(this.height - size[1], 0.0F)));
   }

   private void resetAll() {
      this.module.x.setCurrent(4.0F);
      this.module.y.setCurrent(4.0F);
      this.module.barThickness.setCurrent(1.0F);
      this.module.showSecondBar.setEnabled(false);
      this.module.matchBarThickness.setEnabled(true);
      this.module.secondBarThickness.setCurrent(1.0F);
      this.module.headSize.setCurrent(1.0F);
      this.module.headYaw.setCurrent(0.0F);
      this.module.headPitch.setCurrent(0.0F);
      this.module.headAutoRotate.setEnabled(false);
      this.module.displayMode.set("Проценты и HP");
      this.module.customColors.setEnabled(false);
      this.module.radius.setCurrent(5.0F);
      this.module.borderThickness.setCurrent(1.0F);
      this.module.backgroundAlpha.setCurrent(120.0F);
      this.module.animationSpeed.setCurrent(1.0F);
      this.module.showArmor.setEnabled(true);
      this.module.showPing.setEnabled(true);
      this.module.showEyes.setEnabled(true);
      this.module.eyeSize.setCurrent(1.0F);
      this.module.eyeColor.setColor(new ColorRGBA(255, 255, 255, 255));
      this.module.pupilColor.setColor(new ColorRGBA(15, 15, 15, 255));
      this.module.bgImage.setValue("");
      this.module.headImage.setValue("");
      this.module.clearImageCache();
      this.module.barColorSecond.setColor(new ColorRGBA(100, 100, 115, 255));
   }

   private void openImport(StringSetting setting) {
      this.importSetting = setting;
      this.importDir = mc.runDirectory.toPath();
      this.importScroll = 0.0F;
      this.importOpen = true;
   }

   private List<Path> importEntries() {
      if (this.importDir == null) {
         return List.of();
      }
      List<Path> dirs = new ArrayList();
      List<Path> files = new ArrayList();
      try (var stream = Files.list(this.importDir)) {
         for (Path path : stream.toList()) {
            if (Files.isDirectory(path)) {
               if (!path.getFileName().toString().startsWith(".")) {
                  dirs.add(path);
               }
            } else if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
               files.add(path);
            }
         }
      } catch (Exception e) {
      }
      dirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()));
      files.sort(Comparator.comparing((Path p) -> p.getFileName().toString()));
      List<Path> result = new ArrayList();
      result.addAll(dirs);
      result.addAll(files);
      return result;
   }

   private void renderImportDialog(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));
      float x = (this.width - IMPORT_WIDTH) / 2.0F;
      float y = (this.height - IMPORT_HEIGHT) / 2.0F;
      ColorRGBA accent = this.accent(theme);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x - 6.0F, y - 6.0F, IMPORT_WIDTH + 12.0F, IMPORT_HEIGHT + 12.0F, BorderRadius.all(10.0F), new ColorRGBA(0, 0, 0, 170));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, IMPORT_WIDTH, IMPORT_HEIGHT, BorderRadius.all(8.0F), new ColorRGBA(19, 19, 22).withAlpha(255));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, IMPORT_WIDTH, IMPORT_HEIGHT, 1.0F, BorderRadius.all(8.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));
      draw.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Импорт изображения (PNG)", x + 12.0F, y + 10.0F, TEXT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 12.0F, y + 24.0F, IMPORT_WIDTH - 24.0F, 1.0F, BorderRadius.all(0.5F), accent.withAlpha(90));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.importDir == null ? "" : this.importDir.toString(), x + 12.0F, y + 29.0F, DIM);

      float listY = y + 42.0F;
      float listH = IMPORT_HEIGHT - 42.0F - 34.0F;
      List<Path> entries = this.importEntries();
      float contentH = entries.size() * 16.0F + 16.0F;
      this.importScroll = MathHelper.clamp(this.importScroll, 0.0F, Math.max(contentH - listH, 0.0F));
      draw.enableScissor((int) x, (int) listY, (int) (x + IMPORT_WIDTH), (int) (listY + listH));
      float rowY = listY - this.importScroll;
      if (this.importDir.getParent() != null) {
         this.renderImportRow(draw, mouseX, x, rowY, "..  (назад)", true);
         rowY += 16.0F;
      }
      for (Path entry : entries) {
         if (rowY + 14.0F < listY || rowY > listY + listH) {
            rowY += 16.0F;
         } else {
            boolean isDir = Files.isDirectory(entry);
            this.renderImportRow(draw, mouseX, x, rowY, entry.getFileName().toString() + (isDir ? "/" : ""), isDir);
            rowY += 16.0F;
         }
      }
      draw.disableScissor();
      this.renderScrollbar(draw, x + IMPORT_WIDTH - 7.0F, listY, listH, this.importScroll, contentH, listH, accent);

      float buttonY = y + IMPORT_HEIGHT - 28.0F;
      boolean cancelHovered = MathUtil.isHovered(mouseX, mouseY, x + IMPORT_WIDTH - 88.0F, buttonY, 80.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + IMPORT_WIDTH - 88.0F, buttonY, 80.0F, 18.0F, BorderRadius.all(4.0F), cancelHovered ? new ColorRGBA(64, 64, 72).withAlpha(255) : new ColorRGBA(42, 42, 48).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Отмена", x + IMPORT_WIDTH - 48.0F - Fonts.REGULAR.getWidth("Отмена", 6.0F) / 2.0F, buttonY + 6.0F, TEXT);
   }

   private void renderImportRow(CustomDrawContext draw, float mouseX, float x, float rowY, String label, boolean dir) {
      boolean hovered = MathUtil.isHovered(mouseX, rowY, x + 4.0F, rowY, IMPORT_WIDTH - 8.0F, 15.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 4.0F, rowY, IMPORT_WIDTH - 8.0F, 15.0F, BorderRadius.all(3.0F), hovered ? this.theme().getColor().withAlpha(90) : new ColorRGBA(30, 30, 34).withAlpha(150));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + 12.0F, rowY + 4.0F, dir ? new ColorRGBA(160, 200, 255) : TEXT);
   }

   private void handleImportClick(double mouseX, double mouseY) {
      float x = (this.width - IMPORT_WIDTH) / 2.0F;
      float y = (this.height - IMPORT_HEIGHT) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + IMPORT_WIDTH - 88.0F, y + IMPORT_HEIGHT - 28.0F, 80.0F, 18.0F)) {
         this.importOpen = false;
         return;
      }
      float listY = y + 42.0F;
      float listH = IMPORT_HEIGHT - 42.0F - 34.0F;
      if (mouseX >= x && mouseX <= x + IMPORT_WIDTH && mouseY >= listY && mouseY <= listY + listH) {
         int index = (int) ((mouseY - listY + this.importScroll) / 16.0F);
         if (this.importDir.getParent() != null && index == 0) {
            this.importDir = this.importDir.getParent();
            this.importScroll = 0.0F;
            return;
         }
         if (this.importDir.getParent() != null) {
            index--;
         }
         List<Path> entries = this.importEntries();
         if (index >= 0 && index < entries.size()) {
            Path entry = entries.get(index);
            if (Files.isDirectory(entry)) {
               this.importDir = entry;
               this.importScroll = 0.0F;
            } else {
               this.importSetting.setValue(entry.toAbsolutePath().toString());
               this.module.clearImageCache();
               this.importOpen = false;
            }
         }
      }
   }

   private String trimPath(String path) {
      String name = path;
      int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
      if (slash >= 0 && slash < path.length() - 1) {
         name = path.substring(slash + 1);
      }
      return name.length() > 26 ? name.substring(0, 25) + "…" : name;
   }

   private static final class TextInput {
      String text = "";
      boolean focused;
      private final int maxLength;

      TextInput(int maxLength) {
         this.maxLength = maxLength;
      }

      void handleKey(int keyCode) {
         if (keyCode == 259 && !this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
         }
      }

      void handleChar(char codePoint) {
         if (this.text.length() < this.maxLength) {
            this.text = this.text + codePoint;
         }
      }
   }

   private static final class SettingsLayout {
      final float[] sectionY = new float[6];
      float[] sliderY;
      final float[] toggleY = new float[7];
      float modeY;
      final float[] colorY = new float[5];
      float pickerY;
      final float[] imageY = new float[2];
      final float[] imageStatusY = new float[2];
      float resetY;
      float contentHeight;
   }

   private record SliderEntry(String name, NumberSetting setting) {
   }
}
