package tech.huihui.client.screens.swing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.render.SwingAnimation;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class SwingEditorScreen extends Screen implements IMinecraft {
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;
   private static final float PANEL_WIDTH = 230.0F;
   private static final float PANEL_OFFSET = 10.0F;
   private static final float PANEL_PADDING = 10.0F;
   private static final float ROW_HEIGHT = 20.0F;
   private static final float SECTION_PADDING = 14.0F;
   private static final float BAR_HEIGHT = 28.0F;
   private static final float BAR_OFFSET = 20.0F;
   private static final float[] SPEEDS = {0.5F, 1.0F, 2.0F};
   private static final String[] SPEED_LABELS = {"0.5x", "1x", "2x"};

   private final SwingAnimation module;
   private final boolean wasEnabled;
   private final List<Row> rows = new ArrayList<>();
   private int draggingRow = -1;
   private boolean draggingTimeline;
   private boolean playing = true;
   private float speed = 1.0F;
   private float preview;
   private int direction = 1;
   private long lastFrame;

   public SwingEditorScreen() {
      super(Text.literal("Редактор анимации удара"));
      this.module = SwingAnimation.INSTANCE;
      this.wasEnabled = this.module.isEnabled();
      if (!this.wasEnabled) {
         this.module.setToggled(true);
      }
      this.module.animationMode.set("Custom");
      this.module.setPreviewProgress(0.0F);
      this.lastFrame = System.currentTimeMillis();
      this.buildRows();
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof SwingEditorScreen) {
         return;
      }
      mc.setScreen(new SwingEditorScreen());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      this.module.stopPreview();
      if (!this.wasEnabled) {
         this.module.setToggled(false);
      }
      super.close();
   }

   private void buildRows() {
      this.rows.clear();
      this.addSection("Угол старта");
      this.addSlider("Старт X", this.module.customStartX);
      this.addSlider("Старт Y", this.module.customStartY);
      this.addSlider("Старт Z", this.module.customStartZ);
      this.addSection("Угол конца");
      this.addSlider("Конец X", this.module.customEndX);
      this.addSlider("Конец Y", this.module.customEndY);
      this.addSlider("Конец Z", this.module.customEndZ);
      this.addSection("Позиция");
      this.addSlider("Позиция X", this.module.customPosX);
      this.addSlider("Позиция Y", this.module.customPosY);
      this.addSlider("Позиция Z", this.module.customPosZ);
      this.addSection("Масштаб");
      this.addSlider("Масштаб", this.module.customScale);
   }

   private void addSection(String title) {
      this.rows.add(new Row(title));
   }

   private void addSlider(String label, NumberSetting setting) {
      this.rows.add(new Row(label, setting));
   }

   private float panelX() {
      return this.width - PANEL_WIDTH - PANEL_OFFSET;
   }

   private float panelY() {
      return 30.0F;
   }

   private float panelBoxHeight() {
      return Math.max(300.0F, this.height - this.panelY() - BAR_HEIGHT - BAR_OFFSET * 2.0F);
   }

   private float barY() {
      return this.height - BAR_HEIGHT - BAR_OFFSET;
   }

   private float barTrackX() {
      return BAR_OFFSET + 46.0F;
   }

   private float barTrackW() {
      return this.width - BAR_OFFSET * 2.0F - 46.0F - 190.0F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      this.updatePreview();
      if (this.draggingRow >= 0 && this.draggingRow < this.rows.size()) {
         Row row = this.rows.get(this.draggingRow);
         if (row.setting != null) {
            this.updateSliderValue(row.setting, mouseX, row);
         }
      }
      if (this.draggingTimeline) {
         this.setPreviewFromX(mouseX);
      }

      CustomDrawContext draw = CustomDrawContext.of(context);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      float exitY = EXIT_OFFSET;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, exitY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Редактор анимации удара", EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Крути полосу внизу, лови позу и жми «В старт»/«В конец» — так анимация собирается за секунды", EXIT_OFFSET, EXIT_OFFSET + 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      this.renderPanel(draw, theme, themeColor, mouseX, mouseY);
      this.renderPlaybackBar(draw, theme, themeColor, mouseX, mouseY);
   }

   private void updatePreview() {
      if (!this.playing || this.draggingTimeline) {
         return;
      }
      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastFrame) / 1000.0F;
      this.lastFrame = now;
      if (delta <= 0.0F) {
         return;
      }
      this.preview += delta * this.speed * 0.4F * this.direction;
      if (this.preview >= 1.0F) {
         this.preview = 1.0F;
         this.direction = -1;
      }
      if (this.preview <= 0.0F) {
         this.preview = 0.0F;
         this.direction = 1;
      }
      this.module.setPreviewProgress(this.preview);
   }

   private float curveAt(float t) {
      return this.module.customCurve.is("Плавная") ? t * t * (3.0F - 2.0F * t) : t;
   }

   private float interp(float start, float end, float t) {
      return start + (end - start) * t;
   }

   private float poseX(float t) {
      return this.interp(this.module.customStartX.getCurrent(), this.module.customEndX.getCurrent(), t);
   }

   private float poseY(float t) {
      return this.interp(this.module.customStartY.getCurrent(), this.module.customEndY.getCurrent(), t);
   }

   private float poseZ(float t) {
      return this.interp(this.module.customStartZ.getCurrent(), this.module.customEndZ.getCurrent(), t);
   }

   private void captureToStart() {
      float t = this.curveAt(this.preview);
      this.setRounded(this.module.customStartX, this.poseX(t));
      this.setRounded(this.module.customStartY, this.poseY(t));
      this.setRounded(this.module.customStartZ, this.poseZ(t));
   }

   private void captureToEnd() {
      float t = this.curveAt(this.preview);
      this.setRounded(this.module.customEndX, this.poseX(t));
      this.setRounded(this.module.customEndY, this.poseY(t));
      this.setRounded(this.module.customEndZ, this.poseZ(t));
   }

   private void setRounded(NumberSetting setting, float value) {
      float rounded = (float) MathUtil.round(value, setting.getIncrement());
      setting.setCurrent(MathHelper.clamp(rounded, setting.getMin(), setting.getMax()));
   }

   private void renderPanel(CustomDrawContext draw, Theme theme, ColorRGBA themeColor, float mouseX, float mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      float boxH = this.panelBoxHeight();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, BorderRadius.all(5.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Параметры удара", x + PANEL_PADDING, y + 10.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      float rowY = y + 28.0F;
      for (int i = 0; i < this.rows.size(); i++) {
         Row row = this.rows.get(i);
         if (row.setting == null) {
            draw.drawText(Fonts.REGULAR.getFont(5.0F), row.label, x + PANEL_PADDING, rowY + 4.0F, themeColor.withAlpha(230));
            rowY += SECTION_PADDING;
            continue;
         }
         float trackWidth = PANEL_WIDTH - PANEL_PADDING * 2.0F;
         String value = this.formatValue(row.setting);
         draw.drawText(Fonts.REGULAR.getFont(5.0F), row.label, x + PANEL_PADDING, rowY + 3.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
         draw.drawText(Fonts.REGULAR.getFont(5.0F), value, x + PANEL_PADDING + trackWidth - Fonts.REGULAR.getWidth(value, 5.0F), rowY + 3.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

         float trackY = rowY + 11.0F;
         float anim = this.sliderAnim(row, trackWidth);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, trackY, trackWidth, 2.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, trackY, trackWidth, 2.0F, BorderRadius.all(1.0F), new ColorRGBA(55, 55, 55).withAlpha(120));
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, trackY, anim, 2.0F, BorderRadius.all(1.0F), themeColor.withAlpha(220));
         ColorRGBA knob = this.draggingRow == i ? themeColor.withAlpha(255) : hovered ? themeColor.withAlpha(200) : themeColor.withAlpha(160);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING + anim - 4.0F, trackY - 4.0F, 8.0F, 10.0F, BorderRadius.all(4.0F), knob);
         rowY += ROW_HEIGHT;
      }

      float curveY = rowY + 2.0F;
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Кривая", x + PANEL_PADDING, curveY, new ColorRGBA(200, 200, 200).withAlpha(255));
      float buttonWidth = (PANEL_WIDTH - PANEL_PADDING * 2.0F - 6.0F) / 2.0F;
      this.renderCurveButton(draw, theme, mouseX, mouseY, x + PANEL_PADDING, curveY + 12.0F, buttonWidth, "Линейная", this.module.customCurve.is("Линейная"));
      this.renderCurveButton(draw, theme, mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6.0F, curveY + 12.0F, buttonWidth, "Плавная", this.module.customCurve.is("Плавная"));

      float captureY = curveY + 36.0F;
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Захватить позу", x + PANEL_PADDING, captureY, new ColorRGBA(200, 200, 200).withAlpha(255));
      this.renderCaptureButton(draw, theme, mouseX, mouseY, x + PANEL_PADDING, captureY + 12.0F, buttonWidth, "В старт");
      this.renderCaptureButton(draw, theme, mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6.0F, captureY + 12.0F, buttonWidth, "В конец");

      float resetY = captureY + 36.0F;
      boolean resetHovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F, BorderRadius.all(3.0F), resetHovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Сбросить", x + PANEL_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Сбросить", 5.0F) / 2.0F, resetY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void renderCurveButton(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float x, float y, float width, String label, boolean selected) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, 14.0F);
      ColorRGBA fill = selected ? theme.getColor().withAlpha(hovered ? 210 : 180) : hovered ? new ColorRGBA(58, 58, 58).withAlpha(255) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 14.0F, BorderRadius.all(3.0F), fill);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + width / 2.0F - Fonts.REGULAR.getWidth(label, 5.0F) / 2.0F, y + 4.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void renderCaptureButton(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float x, float y, float width, String label) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, 14.0F);
      ColorRGBA fill = hovered ? theme.getColor().withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 14.0F, BorderRadius.all(3.0F), fill);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + width / 2.0F - Fonts.REGULAR.getWidth(label, 5.0F) / 2.0F, y + 4.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void renderPlaybackBar(CustomDrawContext draw, Theme theme, ColorRGBA themeColor, float mouseX, float mouseY) {
      float y = this.barY();
      float x = BAR_OFFSET;
      float w = this.width - BAR_OFFSET * 2.0F;

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, BAR_HEIGHT, BorderRadius.all(5.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, BAR_HEIGHT, 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));

      float btnX = x + 14.0F;
      float btnY = y + (BAR_HEIGHT - 16.0F) / 2.0F;
      boolean btnHovered = MathUtil.isHovered(mouseX, mouseY, btnX - 4.0F, btnY - 4.0F, 24.0F, 24.0F);
      ColorRGBA btnFill = btnHovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), btnX - 4.0F, btnY - 4.0F, 24.0F, 24.0F, BorderRadius.all(4.0F), btnFill);
      if (this.playing) {
         this.drawPauseIcon(draw, btnX + 3.0F, btnY + 3.0F, 10.0F, 10.0F, themeColor.withAlpha(255));
      } else {
         this.drawPlayIcon(draw, btnX + 4.0F, btnY + 1.0F, 12.0F, 14.0F, themeColor.withAlpha(255));
      }

      float trackX = this.barTrackX();
      float trackY = y + (BAR_HEIGHT - 4.0F) / 2.0F;
      float trackW = this.barTrackW();
      boolean trackHovered = MathUtil.isHovered(mouseX, mouseY, trackX, trackY - 5.0F, trackW, 14.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), trackX, trackY, trackW, 4.0F, BorderRadius.all(2.0F), new ColorRGBA(55, 55, 55).withAlpha(120));
      float fill = trackW * this.preview;
      DrawUtil.drawRoundedRect(draw.getMatrices(), trackX, trackY, fill, 4.0F, BorderRadius.all(2.0F), themeColor.withAlpha(220));
      float knobX = trackX + fill;
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, trackY - 3.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), (this.draggingTimeline || trackHovered) ? themeColor.withAlpha(255) : themeColor.withAlpha(180));

      float percentTextX = trackX + trackW + 12.0F;
      String percent = String.format(Locale.US, "%d%%", (int)Math.round(this.preview * 100.0D));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), percent, percentTextX, y + (BAR_HEIGHT - 6.0F) / 2.0F, new ColorRGBA(200, 200, 200).withAlpha(255));

      float speedX = this.width - BAR_OFFSET - 138.0F;
      for (int i = 0; i < SPEED_LABELS.length; i++) {
         float sX = speedX + i * 46.0F;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, sX, y + (BAR_HEIGHT - 20.0F) / 2.0F, 42.0F, 20.0F);
         boolean selected = this.speed == SPEEDS[i];
         ColorRGBA fillC = selected ? themeColor.withAlpha(hovered ? 210 : 180) : hovered ? new ColorRGBA(58, 58, 58).withAlpha(255) : new ColorRGBA(40, 40, 40).withAlpha(255);
         DrawUtil.drawRoundedRect(draw.getMatrices(), sX, y + (BAR_HEIGHT - 20.0F) / 2.0F, 42.0F, 20.0F, BorderRadius.all(3.0F), fillC);
         draw.drawText(Fonts.REGULAR.getFont(5.0F), SPEED_LABELS[i], sX + 21.0F - Fonts.REGULAR.getWidth(SPEED_LABELS[i], 5.0F) / 2.0F, y + (BAR_HEIGHT - 6.0F) / 2.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      }
   }

   private void drawPlayIcon(CustomDrawContext draw, float x, float y, float w, float h, ColorRGBA color) {
      draw.getMatrices().push();
      draw.getMatrices().translate(0.0F, 0.0F, 1.0F);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(x, y), new Vec2f(x + w, y + h / 2.0F), color);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(x + w, y + h / 2.0F), new Vec2f(x, y + h), color);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(x, y + h), new Vec2f(x, y), color);
      draw.getMatrices().pop();
   }

   private void drawPauseIcon(CustomDrawContext draw, float x, float y, float w, float h, ColorRGBA color) {
      float barW = w / 3.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, barW, h, BorderRadius.all(1.0F), color);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - barW, y, barW, h, BorderRadius.all(1.0F), color);
   }

   private float sliderAnim(Row row, float trackWidth) {
      NumberSetting setting = row.setting;
      float range = setting.getMax() - setting.getMin();
      if (range <= 0.0F) {
         return 0.0F;
      }
      return trackWidth * (setting.getCurrent() - setting.getMin()) / range;
   }

   private void setPreviewFromX(float mouseX) {
      float trackX = this.barTrackX();
      float trackW = this.barTrackW();
      float percent = MathHelper.clamp((mouseX - trackX) / trackW, 0.0F, 1.0F);
      this.preview = percent;
      this.module.setPreviewProgress(percent);
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
      if (mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_WIDTH && mouseY >= this.panelY() && mouseY <= this.panelY() + this.panelBoxHeight()) {
         this.handlePanelClick(mouseX, mouseY);
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, BAR_OFFSET, this.barY(), this.width - BAR_OFFSET * 2.0F, BAR_HEIGHT)) {
         this.handleBarClick(mouseX, mouseY);
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void handlePanelClick(double mouseX, double mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      float rowY = y + 28.0F;
      for (int i = 0; i < this.rows.size(); i++) {
         Row row = this.rows.get(i);
         if (row.setting == null) {
            rowY += SECTION_PADDING;
            continue;
         }
         float trackWidth = PANEL_WIDTH - PANEL_PADDING * 2.0F;
         float trackY = rowY + 11.0F;
         if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, trackY, trackWidth, 2.0F)) {
            this.draggingRow = i;
            this.updateSliderValue(row.setting, (float)mouseX, row);
            return;
         }
         rowY += ROW_HEIGHT;
      }

      float curveY = rowY + 2.0F;
      float buttonWidth = (PANEL_WIDTH - PANEL_PADDING * 2.0F - 6.0F) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, curveY + 12.0F, buttonWidth, 14.0F)) {
         this.module.customCurve.set("Линейная");
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6.0F, curveY + 12.0F, buttonWidth, 14.0F)) {
         this.module.customCurve.set("Плавная");
         return;
      }

      float captureY = curveY + 36.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, captureY + 12.0F, buttonWidth, 14.0F)) {
         this.captureToStart();
         return;
      }
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6.0F, captureY + 12.0F, buttonWidth, 14.0F)) {
         this.captureToEnd();
         return;
      }

      float resetY = captureY + 36.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F)) {
         this.resetAll();
      }
   }

   private void handleBarClick(double mouseX, double mouseY) {
      float x = BAR_OFFSET;
      float y = this.barY();

      float btnX = x + 14.0F - 4.0F;
      float btnY = y + (BAR_HEIGHT - 16.0F) / 2.0F - 4.0F;
      if (MathUtil.isHovered(mouseX, mouseY, btnX, btnY, 24.0F, 24.0F)) {
         this.togglePlay();
         return;
      }

      float trackX = this.barTrackX();
      float trackW = this.barTrackW();
      if (MathUtil.isHovered(mouseX, mouseY, trackX, trackYForHit(), trackW, 14.0F)) {
         this.draggingTimeline = true;
         this.setPreviewFromX((float)mouseX);
         return;
      }

      float speedX = this.width - BAR_OFFSET - 138.0F;
      for (int i = 0; i < SPEED_LABELS.length; i++) {
         float sX = speedX + i * 46.0F;
         if (MathUtil.isHovered(mouseX, mouseY, sX, y + (BAR_HEIGHT - 20.0F) / 2.0F, 42.0F, 20.0F)) {
            this.speed = SPEEDS[i];
            return;
         }
      }
   }

   private float trackYForHit() {
      return this.barY() + (BAR_HEIGHT - 4.0F) / 2.0F - 5.0F;
   }

   private void togglePlay() {
      this.playing = !this.playing;
      this.lastFrame = System.currentTimeMillis();
   }

   private void updateSliderValue(NumberSetting setting, float mouseX, Row row) {
      float x = this.panelX() + PANEL_PADDING;
      float trackWidth = PANEL_WIDTH - PANEL_PADDING * 2.0F;
      float percent = MathHelper.clamp((mouseX - x) / trackWidth, 0.0F, 1.0F);
      float range = setting.getMax() - setting.getMin();
      float raw = setting.getMin() + percent * range;
      float rounded = (float) MathUtil.round(raw, setting.getIncrement());
      setting.setCurrent(MathHelper.clamp(rounded, setting.getMin(), setting.getMax()));
   }

   private void resetAll() {
      this.module.customStartX.setCurrent(0.0F);
      this.module.customStartY.setCurrent(0.0F);
      this.module.customStartZ.setCurrent(0.0F);
      this.module.customEndX.setCurrent(-90.0F);
      this.module.customEndY.setCurrent(0.0F);
      this.module.customEndZ.setCurrent(0.0F);
      this.module.customPosX.setCurrent(0.0F);
      this.module.customPosY.setCurrent(0.0F);
      this.module.customPosZ.setCurrent(0.0F);
      this.module.customScale.setCurrent(1.0F);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingRow = -1;
         this.draggingTimeline = false;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_SPACE) {
         this.togglePlay();
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_LEFT) {
         this.setPreviewFromX(this.barTrackX() + this.barTrackW() * (this.preview - 0.05F));
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_RIGHT) {
         this.setPreviewFromX(this.barTrackX() + this.barTrackW() * (this.preview + 0.05F));
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_HOME) {
         this.setPreviewFromX(this.barTrackX());
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_END) {
         this.setPreviewFromX(this.barTrackX() + this.barTrackW());
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private String formatValue(NumberSetting setting) {
      float increment = setting.getIncrement();
      int decimals = Math.max(0, String.valueOf(increment).contains(".") ? String.valueOf(increment).split("\\.")[1].length() : 0);
      String format = "%." + decimals + "f";
      return String.format(Locale.US, format, setting.getCurrent()).replaceAll("\\.?0+$", "");
   }

   private static final class Row {
      private final String label;
      private final NumberSetting setting;

      private Row(String label, NumberSetting setting) {
         this.label = label;
         this.setting = setting;
      }

      private Row(String label) {
         this.label = label;
         this.setting = null;
      }
   }
}
