package tech.huihui.client.screens.watermark;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.lang.Lang;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.impl.render.Watermark;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class WatermarkLogoEditorScreen extends Screen implements IMinecraft {
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;
   private static final float PANEL_W = 380.0F;
   private static final float PANEL_OFFSET = 24.0F;
   private static final float CANVAS_TOP = 70.0F;
   private static final float CANVAS_BOTTOM_PAD = 46.0F;
   private static final float CELL_GAP = 1.0F;

   private static final ColorRGBA CELL_ON = new ColorRGBA(74, 238, 151, 230);
   private static final ColorRGBA CELL_OFF = new ColorRGBA(255, 255, 255, 18);
   private static final ColorRGBA CELL_HOVER = new ColorRGBA(255, 255, 255, 46);
   private static final ColorRGBA TEXT_MAIN = new ColorRGBA(222, 226, 232);
   private static final ColorRGBA TEXT_DIM = new ColorRGBA(150, 158, 168);

   private static final String[] TARGETS = new String[]{"logo", "nick", "ping", "fps", "tps", "server", "time"};
   private static final String[] SEPARATORS = new String[]{"Нет", "Точка", "Слэш", "Черта"};

   private static final String[][] TARGET_LABELS_RU = new String[][]{{"logo", "Логотип"}, {"nick", "Ник"}, {"ping", "Пинг"}, {"fps", "ФПС"}, {"tps", "ТПС"}, {"server", "Сервер"}, {"time", "Время"}};
   private static final String[][] TARGET_LABELS_EN = new String[][]{{"logo", "Logo"}, {"nick", "Nick"}, {"ping", "Ping"}, {"fps", "FPS"}, {"tps", "TPS"}, {"server", "Server"}, {"time", "Time"}};
   private static final String[][] TARGET_LABELS_ZH = new String[][]{{"logo", "标志"}, {"nick", "昵称"}, {"ping", "延迟"}, {"fps", "帧数"}, {"tps", "TPS"}, {"server", "服务器"}, {"time", "时间"}};

   private final Watermark module;
   private WatermarkLogo logo;
   private int gridW;
   private int gridH;
   private String target = "logo";
   private String letter = "";
   private boolean inputFocused;
   private List<WatermarkLogo> variants;
   private boolean painting;
   private boolean paintValue;
   private int brushSize = 4;
   private int pixelBrushSize = 1;
   private boolean brushMode;
   private float lastBrushX = -1.0F;
   private float lastBrushY = -1.0F;

   public WatermarkLogoEditorScreen() {
      super(Text.literal("Редактор ватермарки"));
      this.module = Watermark.INSTANCE;
      this.loadTarget("logo");
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof WatermarkLogoEditorScreen) {
         return;
      }
      mc.setScreen(new WatermarkLogoEditorScreen());
   }

   private void loadTarget(String id) {
      this.target = id;
      String data = id.equals("logo") ? this.module.getLogoData() : this.module.getIconLogoData(id);
      WatermarkLogo loaded = WatermarkLogo.deserialize(data);
      if (loaded == null) {
         this.logo = new WatermarkLogo(8, 8);
      } else {
         this.logo = loaded;
      }
      this.gridW = this.logo.getWidth();
      this.gridH = this.logo.getHeight();
      this.letter = "";
      this.regenerateVariants();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      super.close();
   }

   private void regenerateVariants() {
      this.variants = WatermarkLogoGenerator.generate(this.letter, this.gridW, this.gridH);
   }

   private void resizeGrid(int newW, int newH) {
      this.gridW = Math.max(WatermarkLogo.MIN_SIZE, Math.min(WatermarkLogo.MAX_SIZE, newW));
      this.gridH = Math.max(WatermarkLogo.MIN_SIZE, Math.min(WatermarkLogo.MAX_SIZE, newH));
      this.logo = this.logo.resized(this.gridW, this.gridH);
      this.regenerateVariants();
   }

   private float panelX() {
      return this.width - PANEL_W - PANEL_OFFSET;
   }

   private float canvasCell() {
      float availW = this.panelX() - PANEL_OFFSET * 2.0F - 40.0F;
      float availH = this.height - CANVAS_TOP - CANVAS_BOTTOM_PAD;
      return Math.max(2.0F, Math.min(availW / (float) this.gridW, availH / (float) this.gridH) - CELL_GAP);
   }

   private float canvasX() {
      float cell = this.canvasCell();
      float gridWpx = (cell + CELL_GAP) * (float) this.gridW;
      return (this.panelX() - PANEL_OFFSET * 2.0F) / 2.0F - gridWpx / 2.0F;
   }

   private float canvasY() {
      float cell = this.canvasCell();
      float gridHpx = (cell + CELL_GAP) * (float) this.gridH;
      return CANVAS_TOP + (this.height - CANVAS_TOP - CANVAS_BOTTOM_PAD) / 2.0F - gridHpx / 2.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      if (this.painting) {
         this.paintCellAt(mouseX, mouseY, this.paintValue);
      }
      CustomDrawContext draw = CustomDrawContext.of(context);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      String exitLabel = this.t("Выход", "Exit", "退出");
      draw.drawText(Fonts.REGULAR.getFont(5.5F), exitLabel, exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth(exitLabel, 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), this.t("Редактор ватермарки", "Watermark editor", "水印编辑器"), EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Выбери элемент, нарисуй пиксели или впиши букву для генерации", "Pick an element, draw pixels or type a letter to generate", "选择元素，绘制像素或输入字母生成"), EXIT_OFFSET, EXIT_OFFSET + 12.0F, TEXT_DIM);

      this.renderCanvas(draw, theme, mouseX, mouseY);
      this.renderPanel(draw, theme, themeColor, mouseX, mouseY);
   }

   private void renderCanvas(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float cell = this.canvasCell();
      float x = this.canvasX();
      float y = this.canvasY();

      float gridWpx = (cell + CELL_GAP) * (float) this.gridW;
      float gridHpx = (cell + CELL_GAP) * (float) this.gridH;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x - 8.0F, y - 8.0F, gridWpx + 16.0F, gridHpx + 16.0F, BorderRadius.all(6.0F), new ColorRGBA(12, 12, 16, 210));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x - 8.0F, y - 8.0F, gridWpx + 16.0F, gridHpx + 16.0F, 1.0F, BorderRadius.all(6.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));

      int hoverCX = (int) Math.floor((mouseX - x) / (cell + CELL_GAP));
      int hoverCY = (int) Math.floor((mouseY - y) / (cell + CELL_GAP));

      for (int cy = 0; cy < this.gridH; cy++) {
         float py = y + (float) cy * (cell + CELL_GAP);
         int cx = 0;
         while (cx < this.gridW) {
            if (!this.logo.get(cx, cy)) {
               if (cx == hoverCX && cy == hoverCY) {
                  float px = x + (float) cx * (cell + CELL_GAP);
                  DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, cell, cell, BorderRadius.all(2.0F), CELL_HOVER);
               }
               cx++;
               continue;
            }
            int start = cx;
            while (cx + 1 < this.gridW && this.logo.get(cx + 1, cy)) {
               cx++;
            }
            float px = x + (float) start * (cell + CELL_GAP);
            float runW = ((float) (cx - start + 1)) * (cell + CELL_GAP) - CELL_GAP;
            DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, runW, cell, BorderRadius.all(2.0F), CELL_ON);
            cx++;
         }
      }

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("ЛКМ — рисовать, ПКМ — стирать", "LMB — draw, RMB — erase", "左键绘制，右键擦除"), x - 8.0F, y + gridHpx + 12.0F, TEXT_DIM);
   }

   private void renderPanel(CustomDrawContext draw, Theme theme, ColorRGBA themeColor, float mouseX, float mouseY) {
      float x = this.panelX();
      float y = CANVAS_TOP;
      float boxH = this.height - CANVAS_TOP - CANVAS_BOTTOM_PAD + 8.0F;

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, PANEL_W, boxH, BorderRadius.all(6.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, PANEL_W, boxH, 1.0F, BorderRadius.all(6.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Настройки", x + 16.0F, y + 12.0F, TEXT_MAIN);

      float cursor = y + 34.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Элемент", "Element", "元素"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      float tBtnW = (PANEL_W - 32.0F - (TARGETS.length - 1) * 4.0F) / TARGETS.length;
      for (int ti = 0; ti < TARGETS.length; ti++) {
         float tx = x + 16.0F + (float) ti * (tBtnW + 4.0F);
         this.renderToggleButton(draw, themeColor, mouseX, mouseY, tx, cursor, tBtnW, this.targetLabel(TARGETS[ti]), this.target.equals(TARGETS[ti]));
      }
      cursor += 24.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Масштаб логотипа и иконок", "Logo & icon scale", "标志和图标缩放"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      float stepperW = (PANEL_W - 32.0F - 20.0F) / 2.0F;
      this.renderFloatStepper(draw, theme, mouseX, mouseY, x + 16.0F, cursor, stepperW, this.t("Масштаб", "Scale", "缩放"), this.module.logoScale.getCurrent());
      this.renderStepper(draw, theme, mouseX, mouseY, x + 16.0F + stepperW + 20.0F, cursor, stepperW, this.t("Отступ", "Gap", "间距"), (int) this.module.cellGap.getCurrent());
      cursor += 26.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Разделитель", "Separator", "分隔符"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      float sepW = (PANEL_W - 32.0F - (SEPARATORS.length - 1) * 4.0F) / SEPARATORS.length;
      for (int si = 0; si < SEPARATORS.length; si++) {
         float sx = x + 16.0F + (float) si * (sepW + 4.0F);
         this.renderToggleButton(draw, themeColor, mouseX, mouseY, sx, cursor, sepW, this.separatorLabel(SEPARATORS[si]), this.module.separator.is(SEPARATORS[si]));
      }
      cursor += 24.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Размер сетки", "Grid size", "网格大小"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 14.0F;
      this.renderStepper(draw, theme, mouseX, mouseY, x + 16.0F, cursor, stepperW, this.t("Ширина", "Width", "宽"), this.gridW);
      this.renderStepper(draw, theme, mouseX, mouseY, x + 16.0F + stepperW + 20.0F, cursor, stepperW, this.t("Высота", "Height", "高"), this.gridH);
      cursor += 26.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Режим кисти", "Brush mode", "画笔模式"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      float modeW = (PANEL_W - 32.0F - 8.0F) / 2.0F;
      this.renderToggleButton(draw, themeColor, mouseX, mouseY, x + 16.0F, cursor, modeW, this.t("Кисть (Крита)", "Brush (Krita)", "画笔 (Krita)"), this.brushMode);
      this.renderToggleButton(draw, themeColor, mouseX, mouseY, x + 16.0F + modeW + 8.0F, cursor, modeW, this.t("Попиксельно", "Pixel", "像素"), !this.brushMode);
      cursor += 24.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.brushMode ? this.t("Диаметр кисти", "Brush diameter", "画笔直径") : this.t("Толщина кисти", "Brush thickness", "画笔粗细"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      this.renderStepper(draw, theme, mouseX, mouseY, x + 16.0F, cursor, PANEL_W - 32.0F, this.brushMode ? this.t("Диаметр", "Diameter", "直径") : this.t("Толщина", "Thickness", "粗细"), this.brushMode ? this.brushSize : this.pixelBrushSize);
      cursor += 26.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Буква (кириллица / латиница)", "Letter (Cyrillic / Latin)", "字母（西里尔/拉丁）"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;
      float inputX = x + 16.0F;
      float inputY = cursor;
      boolean inputHovered = MathUtil.isHovered(mouseX, mouseY, inputX, inputY, PANEL_W - 32.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), inputX, inputY, PANEL_W - 32.0F, 18.0F, BorderRadius.all(3.0F), this.inputFocused ? themeColor.withAlpha(90) : inputHovered ? new ColorRGBA(255, 255, 255, 26) : new ColorRGBA(255, 255, 255, 14));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), inputX, inputY, PANEL_W - 32.0F, 18.0F, 1.0F, BorderRadius.all(3.0F), this.inputFocused ? themeColor.withAlpha(180) : new ColorRGBA(255, 255, 255, 22));
      String shown = this.letter + (this.inputFocused && System.currentTimeMillis() % 900L < 450L ? "|" : "");
      draw.drawText(Fonts.REGULAR.getFont(6.0F), shown.isEmpty() ? this.t("Введи букву...", "Enter a letter...", "输入字母...") : shown, inputX + 8.0F, inputY + 5.0F, this.letter.isEmpty() ? TEXT_DIM : TEXT_MAIN);
      cursor += 24.0F;

      draw.drawText(Fonts.REGULAR.getFont(5.0F), this.t("Варианты", "Variants", "变体"), x + 16.0F, cursor, TEXT_DIM);
      cursor += 13.0F;

      if (this.variants.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(5.0F), this.letter.isEmpty() ? this.t("Введи букву, чтобы сгенерировать варианты", "Enter a letter to generate variants", "输入字母以生成变体") : this.t("Не удалось сгенерировать варианты", "Failed to generate variants", "无法生成变体"), x + 16.0F, cursor + 4.0F, TEXT_DIM);
      } else {
         float vCell = 44.0F;
         float gap = 8.0F;
         int cols = Math.max(1, (int) ((PANEL_W - 32.0F + gap) / (vCell + gap)));
         int index = 0;
         for (WatermarkLogo variant : this.variants) {
            int col = index % cols;
            int row = index / cols;
            float vx = x + 16.0F + (float) col * (vCell + gap);
            float vy = cursor + (float) row * (vCell + gap);
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, vx, vy, vCell, vCell);
            boolean active = variant.serialize().equals(this.logo.serialize());
            ColorRGBA fill = active ? themeColor.withAlpha(120) : hovered ? new ColorRGBA(255, 255, 255, 30) : new ColorRGBA(255, 255, 255, 14);
            DrawUtil.drawRoundedRect(draw.getMatrices(), vx, vy, vCell, vCell, BorderRadius.all(4.0F), fill);
            DrawUtil.drawRoundedBorder(draw.getMatrices(), vx, vy, vCell, vCell, 1.0F, BorderRadius.all(4.0F), active ? themeColor.withAlpha(200) : new ColorRGBA(255, 255, 255, 18));
            variant.renderFit(draw.getMatrices(), vx + 6.0F, vy + 6.0F, vCell - 12.0F, vCell - 12.0F, TEXT_MAIN);
            index++;
         }
      }

      float bottomY = y + boxH - 52.0F;
      float btnW = (PANEL_W - 32.0F - 8.0F) / 2.0F;
      this.renderActionButton(draw, themeColor, mouseX, mouseY, x + 16.0F, bottomY, PANEL_W - 32.0F, this.t("Импорт PNG", "Import PNG", "导入 PNG"));
      this.renderActionButton(draw, themeColor, mouseX, mouseY, x + 16.0F, bottomY + 24.0F, btnW, this.t("Очистить", "Clear", "清除"));
      this.renderActionButton(draw, themeColor, mouseX, mouseY, x + 16.0F + btnW + 8.0F, bottomY + 24.0F, btnW, this.t("Сохранить", "Save", "保存"));
   }

   private String lang() {
      return this.module.lang.get();
   }

   private String t(String ru, String en, String zh) {
      return Lang.t(this.lang(), ru, en, zh);
   }

   private String targetLabel(String id) {
      for (int i = 0; i < TARGETS.length; i++) {
         if (TARGETS[i].equals(id)) {
            if (this.lang().equals("English")) {
               return TARGET_LABELS_EN[i][1];
            }
            if (this.lang().equals("中文")) {
               return TARGET_LABELS_ZH[i][1];
            }
            return TARGET_LABELS_RU[i][1];
         }
      }
      return id;
   }

   private String separatorLabel(String sep) {
      if (this.lang().equals("English")) {
         return sep.equals("Нет") ? "None" : sep.equals("Точка") ? "Dot" : sep.equals("Слэш") ? "Slash" : "Pipe";
      }
      if (this.lang().equals("中文")) {
         return sep.equals("Нет") ? "无" : sep.equals("Точка") ? "点" : sep.equals("Слэш") ? "斜线" : "竖线";
      }
      return sep;
   }

   private void renderStepper(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float x, float y, float width, String label, int value) {
      boolean minusHovered = MathUtil.isHovered(mouseX, mouseY, x, y, 16.0F, 18.0F);
      boolean plusHovered = MathUtil.isHovered(mouseX, mouseY, x + width - 16.0F, y, 16.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 18.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 14));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, 16.0F, 18.0F, BorderRadius.all(3.0F), minusHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(255, 255, 255, 8));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 16.0F, y, 16.0F, 18.0F, BorderRadius.all(3.0F), plusHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(255, 255, 255, 8));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "-", x + 6.0F, y + 4.5F, TEXT_MAIN);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "+", x + width - 9.5F, y + 4.5F, TEXT_MAIN);
      String center = label + ": " + value;
      draw.drawText(Fonts.REGULAR.getFont(5.5F), center, x + width / 2.0F - Fonts.REGULAR.getWidth(center, 5.5F) / 2.0F, y + 5.5F, TEXT_MAIN);
   }

   private void renderActionButton(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY, float x, float y, float width, String label) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, 18.0F);
      ColorRGBA fill = hovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 18.0F, BorderRadius.all(3.0F), fill);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + width / 2.0F - Fonts.REGULAR.getWidth(label, 5.5F) / 2.0F, y + 5.5F, TEXT_MAIN);
   }

   private void renderFloatStepper(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float x, float y, float width, String label, float value) {
      boolean minusHovered = MathUtil.isHovered(mouseX, mouseY, x, y, 16.0F, 18.0F);
      boolean plusHovered = MathUtil.isHovered(mouseX, mouseY, x + width - 16.0F, y, 16.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 18.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 14));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, 16.0F, 18.0F, BorderRadius.all(3.0F), minusHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(255, 255, 255, 8));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 16.0F, y, 16.0F, 18.0F, BorderRadius.all(3.0F), plusHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(255, 255, 255, 8));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "-", x + 6.0F, y + 4.5F, TEXT_MAIN);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "+", x + width - 9.5F, y + 4.5F, TEXT_MAIN);
      String center = label + ": " + Math.round(value * 10.0F) / 10.0F;
      draw.drawText(Fonts.REGULAR.getFont(5.5F), center, x + width / 2.0F - Fonts.REGULAR.getWidth(center, 5.5F) / 2.0F, y + 5.5F, TEXT_MAIN);
   }

   private void renderToggleButton(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY, float x, float y, float width, String label, boolean active) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, 18.0F);
      ColorRGBA fill = active ? themeColor.withAlpha(130) : hovered ? new ColorRGBA(255, 255, 255, 26) : new ColorRGBA(40, 40, 40).withAlpha(255);
      ColorRGBA border = active ? themeColor.withAlpha(200) : new ColorRGBA(255, 255, 255, 16);
      ColorRGBA text = active ? new ColorRGBA(255, 255, 255, 255) : TEXT_MAIN;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 18.0F, BorderRadius.all(3.0F), fill);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, width, 18.0F, 1.0F, BorderRadius.all(3.0F), border);
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + width / 2.0F - Fonts.REGULAR.getWidth(label, 5.5F) / 2.0F, y + 5.5F, text);
   }

   private void paintCellAt(double mouseX, double mouseY, boolean value) {
      float cell = this.canvasCell();
      float x = this.canvasX();
      float y = this.canvasY();
      float cx = (float) ((mouseX - x) / (cell + CELL_GAP));
      float cy = (float) ((mouseY - y) / (cell + CELL_GAP));
      if (this.brushMode) {
         this.paintBrushStroke(cx, cy, value);
      } else {
         int ix = Math.round(cx);
         int iy = Math.round(cy);
         int half = Math.max(0, (this.pixelBrushSize - 1) / 2);
         for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
               this.logo.set(ix + dx, iy + dy, value);
            }
         }
         this.lastBrushX = -1.0F;
      }
   }

   private void paintBrushStroke(float cx, float cy, boolean value) {
      float radius = Math.max((this.brushSize - 1) / 2.0F, 0.5F);
      if (this.lastBrushX >= 0.0F) {
         float fromX = this.lastBrushX;
         float fromY = this.lastBrushY;
         float dist = (float) Math.sqrt((cx - fromX) * (cx - fromX) + (cy - fromY) * (cy - fromY));
         int steps = Math.max(1, (int) Math.ceil(dist / 0.5F));
         for (int i = 1; i <= steps; i++) {
            float t = (float) i / (float) steps;
            this.stampCircle(fromX + (cx - fromX) * t, fromY + (cy - fromY) * t, radius, value);
         }
      } else {
         this.stampCircle(cx, cy, radius, value);
      }
      this.lastBrushX = cx;
      this.lastBrushY = cy;
   }

   private void stampCircle(float cx, float cy, float radius, boolean value) {
      int minX = Math.max(0, (int) Math.floor(cx - radius));
      int maxX = Math.min(this.gridW - 1, (int) Math.ceil(cx + radius));
      int minY = Math.max(0, (int) Math.floor(cy - radius));
      int maxY = Math.min(this.gridH - 1, (int) Math.ceil(cy + radius));
      for (int px = minX; px <= maxX; px++) {
         for (int py = minY; py <= maxY; py++) {
            float dx = (px + 0.5F) - cx;
            float dy = (py + 0.5F) - cy;
            if (dx * dx + dy * dy <= radius * radius) {
               this.logo.set(px, py, value);
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      if (MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT)) {
         this.close();
         return true;
      }

      if (button == 0 || button == 1) {
         float cell = this.canvasCell();
         float x = this.canvasX();
         float y = this.canvasY();
         float gridWpx = (cell + CELL_GAP) * (float) this.gridW;
         float gridHpx = (cell + CELL_GAP) * (float) this.gridH;
         if (mouseX >= x - 8.0F && mouseX <= x - 8.0F + gridWpx + 16.0F && mouseY >= y - 8.0F && mouseY <= y - 8.0F + gridHpx + 16.0F) {
            this.painting = true;
            this.paintValue = button == 0;
            this.lastBrushX = -1.0F;
            this.paintCellAt(mouseX, mouseY, this.paintValue);
            return true;
         }
      }

      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      float panelY = CANVAS_TOP;
      if (mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_W && mouseY >= panelY && mouseY <= panelY + this.height - CANVAS_TOP - CANVAS_BOTTOM_PAD + 8.0F) {
         this.handlePanelClick(mouseX, mouseY, panelY);
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void handlePanelClick(double mouseX, double mouseY, float panelY) {
      float x = this.panelX();
      float cursor = panelY + 34.0F;

      cursor += 13.0F;
      float tBtnW = (PANEL_W - 32.0F - (TARGETS.length - 1) * 4.0F) / TARGETS.length;
      for (int ti = 0; ti < TARGETS.length; ti++) {
         float tx = x + 16.0F + (float) ti * (tBtnW + 4.0F);
         if (MathUtil.isHovered(mouseX, mouseY, tx, cursor, tBtnW, 18.0F)) {
            if (!this.target.equals(TARGETS[ti])) {
               this.loadTarget(TARGETS[ti]);
            }
            return;
         }
      }
      cursor += 24.0F;

      cursor += 13.0F;
      float stepperW = (PANEL_W - 32.0F - 20.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, cursor, 16.0F, 18.0F)) {
         this.module.logoScale.setCurrent(Math.max(0.5F, Math.round((this.module.logoScale.getCurrent() - 0.1F) * 10.0F) / 10.0F));
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + stepperW - 16.0F, cursor, 16.0F, 18.0F)) {
         this.module.logoScale.setCurrent(Math.min(3.0F, Math.round((this.module.logoScale.getCurrent() + 0.1F) * 10.0F) / 10.0F));
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + stepperW + 20.0F, cursor, 16.0F, 18.0F)) {
         this.module.cellGap.setCurrent(Math.max(4.0F, this.module.cellGap.getCurrent() - 1.0F));
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + stepperW + 20.0F + stepperW - 16.0F, cursor, 16.0F, 18.0F)) {
         this.module.cellGap.setCurrent(Math.min(32.0F, this.module.cellGap.getCurrent() + 1.0F));
         return;
      }
      cursor += 26.0F;

      cursor += 13.0F;
      float sepW = (PANEL_W - 32.0F - (SEPARATORS.length - 1) * 4.0F) / SEPARATORS.length;
      for (int si = 0; si < SEPARATORS.length; si++) {
         float sx = x + 16.0F + (float) si * (sepW + 4.0F);
         if (MathUtil.isHovered(mouseX, mouseY, sx, cursor, sepW, 18.0F)) {
            this.module.separator.set(SEPARATORS[si]);
            return;
         }
      }
      cursor += 24.0F;

      cursor += 14.0F;
      float minusW = x + 16.0F;
      float minusH = cursor;
      float plusW = x + 16.0F + stepperW - 16.0F;
      float plusH = cursor;
      if (MathUtil.isHovered(mouseX, mouseY, minusW, minusH, 16.0F, 18.0F)) {
         this.resizeGrid(this.gridW - 1, this.gridH);
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, plusW, plusH, 16.0F, 18.0F)) {
         this.resizeGrid(this.gridW + 1, this.gridH);
         return;
      }
      float minusW2 = x + 16.0F + stepperW + 20.0F;
      float minusH2 = cursor;
      float plusW2 = x + 16.0F + stepperW + 20.0F + stepperW - 16.0F;
      float plusH2 = cursor;
      if (MathUtil.isHovered(mouseX, mouseY, minusW2, minusH2, 16.0F, 18.0F)) {
         this.resizeGrid(this.gridW, this.gridH - 1);
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, plusW2, plusH2, 16.0F, 18.0F)) {
         this.resizeGrid(this.gridW, this.gridH + 1);
         return;
      }

      cursor += 26.0F;

      cursor += 13.0F;
      float modeW = (PANEL_W - 32.0F - 8.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, cursor, modeW, 18.0F)) {
         this.brushMode = true;
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + modeW + 8.0F, cursor, modeW, 18.0F)) {
         this.brushMode = false;
         return;
      }
      cursor += 24.0F;

      cursor += 13.0F;
      float thickW = PANEL_W - 32.0F;
      if (this.brushMode) {
         if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, cursor, 16.0F, 18.0F)) {
            this.brushSize = Math.max(1, this.brushSize - 1);
            return;
         }
         if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + thickW - 16.0F, cursor, 16.0F, 18.0F)) {
            this.brushSize = Math.min(128, this.brushSize + 1);
            return;
         }
      } else {
         if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, cursor, 16.0F, 18.0F)) {
            this.pixelBrushSize = Math.max(1, this.pixelBrushSize - 1);
            return;
         }
         if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + thickW - 16.0F, cursor, 16.0F, 18.0F)) {
            this.pixelBrushSize = Math.min(32, this.pixelBrushSize + 1);
            return;
         }
      }
      cursor += 26.0F;
      cursor += 13.0F;
      float inputY = cursor;
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, inputY, PANEL_W - 32.0F, 18.0F)) {
         this.inputFocused = true;
         return;
      }
      this.inputFocused = false;

      cursor += 24.0F;
      cursor += 13.0F;

      float vCell = 44.0F;
      float gap = 8.0F;
      int cols = Math.max(1, (int) ((PANEL_W - 32.0F + gap) / (vCell + gap)));
      int index = 0;
      for (WatermarkLogo variant : this.variants) {
         int col = index % cols;
         int row = index / cols;
         float vx = x + 16.0F + (float) col * (vCell + gap);
         float vy = cursor + (float) row * (vCell + gap);
         if (MathUtil.isHovered(mouseX, mouseY, vx, vy, vCell, vCell)) {
            this.logo = variant.resized(this.gridW, this.gridH);
            return;
         }
         index++;
      }

      float bottomY = panelY + this.height - CANVAS_TOP - CANVAS_BOTTOM_PAD + 8.0F - 52.0F;
      float btnW = (PANEL_W - 32.0F - 8.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, bottomY, PANEL_W - 32.0F, 18.0F)) {
         this.importPng();
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F, bottomY + 24.0F, btnW, 18.0F)) {
         this.logo.clear();
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + 16.0F + btnW + 8.0F, bottomY + 24.0F, btnW, 18.0F)) {
         this.saveAndClose();
      }
   }

   private void saveAndClose() {
      if (this.target.equals("logo")) {
         this.module.setLogoData(this.logo.serialize());
      } else {
         this.module.setIconLogoData(this.target, this.logo.serialize());
      }
      HuihuiClient.getInstance().getConfigManager().saveConfig("current_config");
      this.close();
   }

   private void importPng() {
      Thread thread = new Thread(() -> {
         try {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            chooser.setDialogTitle(this.t("Выбери PNG логотип", "Choose PNG logo", "选择 PNG 标志"));
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG изображения", "png"));
            int result = chooser.showOpenDialog(null);
            if (result != javax.swing.JFileChooser.APPROVE_OPTION) {
               return;
            }
            java.io.File file = chooser.getSelectedFile();
            if (file == null) {
               return;
            }
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
            if (image == null) {
               return;
            }
            WatermarkLogo imported = WatermarkLogo.fromImage(image, this.gridW, this.gridH);
            mc.execute(() -> {
               this.logo = imported.resized(this.gridW, this.gridH);
               this.letter = "";
               this.regenerateVariants();
            });
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
      thread.setDaemon(true);
      thread.start();
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 || button == 1) {
         this.painting = false;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.painting && (button == 0 || button == 1)) {
         this.paintCellAt(mouseX, mouseY, this.paintValue);
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.inputFocused) {
         if (keyCode == 259 && !this.letter.isEmpty()) {
            this.letter = this.letter.substring(0, this.letter.length() - 1);
            this.regenerateVariants();
            return true;
         }
         if (keyCode == 257 || keyCode == 256) {
            this.inputFocused = false;
            return true;
         }
      }
      if (keyCode == 256) {
         this.close();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.inputFocused && chr >= 32 && chr != 127) {
         this.letter = String.valueOf(chr);
         this.regenerateVariants();
         return true;
      }
      return super.charTyped(chr, modifiers);
   }
}