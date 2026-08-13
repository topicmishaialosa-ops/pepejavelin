package tech.huihui.client.screens.targethud;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.client.modules.impl.render.TargetHud;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.ToggleSwitch;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class TargetHudEditScreen extends Screen implements IMinecraft {
   private static final String[] TYPES = {"Крупный", "Маленький", "Проценты", "Большая полоса", "Вертикальный", "Метал", "Мини", "Градиент", "Боссбар", "Минимал", "Кружок"};
   private static final String[] MODES = {"Проценты и HP", "Проценты", "HP"};
   private static final String[] COLOR_NAMES = {"Цвет полоски", "Цвет золотой", "Цвет фона", "Цвет рамки", "Цвет текста"};

   private static final ColorRGBA TEXT = new ColorRGBA(233, 236, 242);
   private static final ColorRGBA SUBTEXT = new ColorRGBA(150, 155, 165);
   private static final ColorRGBA DIM = new ColorRGBA(104, 109, 120);
   private static final ColorRGBA PANEL_BG = new ColorRGBA(14, 15, 19, 242);
   private static final ColorRGBA CARD = new ColorRGBA(27, 28, 34, 235);
   private static final ColorRGBA ROW_BG = new ColorRGBA(31, 32, 39, 220);
   private static final ColorRGBA ROW_HOVER = new ColorRGBA(55, 56, 66, 220);
   private static final ColorRGBA INPUT_BG = new ColorRGBA(20, 21, 26, 235);
   private static final ColorRGBA DANGER = new ColorRGBA(255, 96, 96);

   private static final float MARGIN = 12.0F;
   private static final float HEADER_H = 44.0F;
   private static final float TOP_Y = HEADER_H + 6.0F;
   private static final float RAIL_W = 158.0F;
   private static final float GAP = 8.0F;
   private static final float BOTTOM_BAR_H = 46.0F;
   private static final float TAB_H = 34.0F;
   private static final float TAB_GAP = 4.0F;
   private static final float TAB_STRIP_H = TAB_H + 10.0F;
   private static final float CARDS_W = 240.0F;
   private static final float MIN_W = 520.0F;
   private static final float DOCK_W = 250.0F;
   private static final float FLOAT_W = 780.0F;
   private static final float FLOAT_H = 560.0F;
   private static final float FLOAT_TITLE_H = 40.0F;
   private static final float PILL_H = 26.0F;
   private static final float PILL_AREA_H = 68.0F;
   private static final float PAD = 14.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float SECTION_H = 22.0F;
   private static final float SLIDER_H = 30.0F;
   private static final float TOGGLE_H = 26.0F;
   private static final float MODE_H = 34.0F;
   private static final float COLOR_H = 28.0F;
   private static final float PICKER_H = 62.0F;
   private static final float IMAGE_H = 38.0F;
   private static final float LIST_H = 24.0F;
   private static final float BUTTON_H = 26.0F;
   private static final float IMPORT_W = 380.0F;
   private static final float IMPORT_H = 280.0F;

   private enum Tab {
      BAR("Полоска"),
      HEAD("Голова"),
      STYLE("Стиль"),
      COLORS("Цвета"),
      IMAGES("Изображения"),
      PRESETS("Пресеты"),
      LAYOUT("Вид");

      final String label;

      Tab(String label) {
         this.label = label;
      }
   }

   private enum ScreenView {
      CLASSIC("Классика", "Вкладки слева, панель справа"),
      TOP("Табы сверху", "Вкладки над панелью по центру"),
      CARDS("Плитки", "Крупные карточки-вкладки"),
      MINIMAL("Минимум", "Узкая панель по центру"),
      DOCKED("Док-превью", "Живое превью в правой панели"),
      FLOAT("Окно", "Плавающее окно по центру");

      final String label;
      final String desc;

      ScreenView(String label, String desc) {
         this.label = label;
         this.desc = desc;
      }
   }

   private static final ScreenView[] SCREEN_VIEWS = ScreenView.values();
   private static final String[] VIEW_DESC = new String[SCREEN_VIEWS.length];

   static {
      for (int i = 0; i < SCREEN_VIEWS.length; i++) {
         VIEW_DESC[i] = SCREEN_VIEWS[i].desc;
      }
   }
   private static final String[] VIEW_LABEL = new String[SCREEN_VIEWS.length];

   static {
      for (int i = 0; i < SCREEN_VIEWS.length; i++) {
         VIEW_LABEL[i] = SCREEN_VIEWS[i].label;
      }
   }

   private final TargetHud module;
   private final TargetHudPresetManager presetManager;

   private Tab selectedTab;
   private final List<Tab> tabOrder = new ArrayList<>();
   private final float[] contentScroll = new float[Tab.values().length];
   private String appliedName = "";
   private final TextInput nameInput = new TextInput(24);
   private ColorSetting activeColor;
   private final float[] hsb = new float[3];
   private final Map<BooleanSetting, Float> toggleAnim = new HashMap<>();
   private final Map<NumberSetting, Float> sliderAnim = new HashMap<>();

   private int screenViewIndex;

   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private int dragSlider = -1;
   private boolean draggingPicker;
   private boolean draggingHue;

   private boolean importOpen;
   private StringSetting importSetting;
   private Path importDir;
   private float importScroll;

   private List<Row> rows = new ArrayList<>();
   private float contentHeight;

   public TargetHudEditScreen(TargetHud module) {
      super(Text.literal("Редактор таргетхуда"));
      this.module = module;
      this.presetManager = HuihuiClient.getInstance().getTargetHudPresetManager();
      this.tabOrder.addAll(Arrays.asList(Tab.values()));
      this.selectedTab = Tab.BAR;
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

   private ColorRGBA accent() {
      return this.theme().getColor();
   }

   private ColorRGBA border() {
      return this.theme().getSecondColor().darker(0.5F).withAlpha(150);
   }

   private List<Tab> visibleTabs() {
      return this.tabOrder;
   }

   private ScreenView view() {
      return SCREEN_VIEWS[Math.abs(this.screenViewIndex) % SCREEN_VIEWS.length];
   }

   private float railW() {
      return switch (this.view()) {
         case TOP, MINIMAL -> 0.0F;
         case CARDS -> CARDS_W;
         default -> RAIL_W;
      };
   }

   private float frameX() {
      return switch (this.view()) {
         case FLOAT -> (this.width - FLOAT_W) / 2.0F;
         case MINIMAL -> (this.width - MIN_W) / 2.0F;
         default -> 0.0F;
      };
   }

   private float frameW() {
      return switch (this.view()) {
         case FLOAT -> FLOAT_W;
         case MINIMAL -> MIN_W;
         default -> this.width;
      };
   }

   private float uiBottom() {
      if (this.view() == ScreenView.FLOAT) {
         return (this.height - FLOAT_H) / 2.0F + FLOAT_H;
      }
      return this.height - MARGIN;
   }

   private float tableTop() {
      if (this.view() == ScreenView.FLOAT) {
         return this.frameY() + FLOAT_TITLE_H;
      }
      if (this.view() == ScreenView.TOP) {
         return TOP_Y + TAB_STRIP_H;
      }
      return (this.view() == ScreenView.MINIMAL) ? this.frameY() + PILL_AREA_H : TOP_Y;
   }

   private float frameY() {
      return switch (this.view()) {
         case FLOAT -> (this.height - FLOAT_H) / 2.0F;
         case MINIMAL -> TOP_Y;
         default -> 0.0F;
      };
   }

   private float contentX() {
      return this.frameX() + MARGIN + this.railW() + (this.railW() > 0.0F ? GAP : 0.0F);
   }

   private float contentW() {
      float dockW = this.view() == ScreenView.DOCKED ? DOCK_W + GAP : 0.0F;
      return this.frameX() + this.frameW() - MARGIN - dockW - this.contentX();
   }

   private float contentTop() {
      return this.tableTop();
   }

   private float panelH() {
      return this.bottomBarY() - this.contentTop();
   }

   private float bottomBarY() {
      return this.uiBottom() - BOTTOM_BAR_H;
   }

   private boolean hasRail() {
      return this.railW() > 0.0F;
   }

   private boolean inRail(double mouseX, double mouseY) {
      if (!this.hasRail()) {
         return false;
      }
      return mouseX >= this.frameX() + MARGIN && mouseX <= this.frameX() + MARGIN + this.railW() && mouseY >= this.contentTop() && mouseY <= this.bottomBarY();
   }

   private boolean inTabStrip(double mouseX, double mouseY) {
      if (this.view() == ScreenView.TOP) {
         return mouseY >= this.frameY() + TOP_Y && mouseY <= this.frameY() + TOP_Y + TAB_STRIP_H && mouseX >= this.frameX() + MARGIN && mouseX <= this.frameX() + this.frameW() - MARGIN;
      }
      if (this.view() == ScreenView.MINIMAL) {
         return mouseY >= this.frameY() && mouseY <= this.frameY() + PILL_AREA_H && mouseX >= this.frameX() + MARGIN && mouseX <= this.frameX() + this.frameW() - MARGIN;
      }
      return false;
   }

   private boolean inContent(double mouseX, double mouseY) {
      return mouseX >= this.contentX() && mouseX <= this.contentX() + this.contentW() && mouseY >= this.contentTop() && mouseY <= this.bottomBarY();
   }

   private float tabW() {
      List<Tab> tabs = this.visibleTabs();
      if (tabs.isEmpty()) {
         return 120.0F;
      }
      if (this.view() == ScreenView.TOP) {
         float avail = this.frameX() + this.frameW() - MARGIN * 2.0F - 12.0F;
         return Math.max(86.0F, Math.min(120.0F, (avail - (tabs.size() - 1) * TAB_GAP) / tabs.size()));
      }
      if (this.view() == ScreenView.MINIMAL) {
         float avail = MIN_W - MARGIN * 2.0F - 12.0F;
         return Math.max(64.0F, Math.min(120.0F, (avail - (tabs.size() - 1) * TAB_GAP) / tabs.size()));
      }
      if (this.view() == ScreenView.CARDS) {
         return (this.railW() - 16.0F - TAB_GAP) / 2.0F;
      }
      return RAIL_W - 16.0F;
   }

   private float[] tabRect(int index) {
      List<Tab> tabs = this.visibleTabs();
      if (index < 0 || index >= tabs.size()) {
         return new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      }
      float w = this.tabW();
      switch (this.view()) {
         case TOP: {
            float x = this.frameX() + MARGIN + 6.0F + index * (w + TAB_GAP);
            return new float[]{x, this.frameY() + TOP_Y + 5.0F, w, TAB_H};
         }
         case MINIMAL: {
            float x = this.frameX() + MARGIN + 6.0F + index * (w + TAB_GAP);
            return new float[]{x, this.frameY() + 36.0F, w, PILL_H};
         }
         case CARDS: {
            int col = index % 2;
            int row = index / 2;
            float x = this.frameX() + MARGIN + 8.0F + col * (w + TAB_GAP);
            float y = this.tableTop() + 30.0F + row * (TAB_H + TAB_GAP);
            return new float[]{x, y, w, TAB_H};
         }
         default: {
            float y = this.tableTop() + 30.0F + index * (TAB_H + TAB_GAP);
            return new float[]{this.frameX() + MARGIN + 8.0F, y, w, TAB_H};
         }
      }
   }

   private boolean hitTab(int index, double mouseX, double mouseY) {
      float[] r = this.tabRect(index);
      return MathUtil.isHovered(mouseX, mouseY, r[0], r[1], r[2], r[3]);
   }

   private List<NumberSetting> tabSliders() {
      List<NumberSetting> list = new ArrayList<>();
      if (this.selectedTab == Tab.BAR) {
         list.add(this.module.barThickness);
         list.add(this.module.barRadius);
         if (this.module.showSecondBar.isEnabled() && !this.module.matchBarThickness.isEnabled()) {
            list.add(this.module.secondBarThickness);
         }
      } else if (this.selectedTab == Tab.HEAD) {
         list.add(this.module.headSize);
         list.add(this.module.headYaw);
         list.add(this.module.headPitch);
         if (this.module.showEyes.isEnabled()) {
            list.add(this.module.eyeSize);
         }
      } else if (this.selectedTab == Tab.STYLE) {
         list.add(this.module.radius);
         list.add(this.module.borderThickness);
         list.add(this.module.backgroundAlpha);
         list.add(this.module.animationSpeed);
      }
      return list;
   }

   private List<BooleanSetting> tabToggles() {
      List<BooleanSetting> list = new ArrayList<>();
      if (this.selectedTab == Tab.BAR) {
         list.add(this.module.showSecondBar);
         if (this.module.showSecondBar.isEnabled()) {
            list.add(this.module.matchBarThickness);
         }
      } else if (this.selectedTab == Tab.HEAD) {
         list.add(this.module.headAutoRotate);
         list.add(this.module.showEyes);
      } else if (this.selectedTab == Tab.STYLE) {
         list.add(this.module.showArmor);
         list.add(this.module.showPing);
      } else if (this.selectedTab == Tab.COLORS) {
         list.add(this.module.customColors);
      }
      return list;
   }

   private ColorSetting[] colors() {
      return new ColorSetting[]{this.module.barColor, this.module.barColorSecond, this.module.bgColor, this.module.borderColor, this.module.textColor};
   }

   private void buildRows() {
      this.rows.clear();
      float y = 8.0F;
      switch (this.selectedTab) {
         case BAR -> {
            int s = 0;
            int t = 0;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            if (this.module.showSecondBar.isEnabled()) {
               this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
               y += TOGGLE_H + ROW_GAP;
               if (!this.module.matchBarThickness.isEnabled()) {
                  this.rows.add(new Row("slider", s++, y, SLIDER_H));
                  y += SLIDER_H + ROW_GAP;
               }
            }
         }
         case HEAD -> {
            int s = 0;
            int t = 0;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            if (this.module.showEyes.isEnabled()) {
               this.rows.add(new Row("slider", s, y, SLIDER_H));
               y += SLIDER_H + ROW_GAP;
            }
         }
         case STYLE -> {
            int s = 0;
            int t = 0;
            y = this.section(y, 0);
            this.rows.add(new Row("mode", 0, y, MODE_H));
            y += MODE_H + ROW_GAP;
            this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            this.rows.add(new Row("toggle", t++, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            y = this.section(y, 1);
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
            this.rows.add(new Row("slider", s++, y, SLIDER_H));
            y += SLIDER_H + ROW_GAP;
         }
         case COLORS -> {
            ColorSetting[] colors = this.colors();
            this.rows.add(new Row("toggle", 0, y, TOGGLE_H));
            y += TOGGLE_H + ROW_GAP;
            y = this.section(y, 0);
            for (int i = 0; i < colors.length; i++) {
               this.rows.add(new Row("color", i, y, COLOR_H));
               y += COLOR_H + ROW_GAP;
               if (this.activeColor == colors[i]) {
                  this.rows.add(new Row("picker", 0, y, PICKER_H + 6.0F));
                  y += PICKER_H + 6.0F + ROW_GAP;
               }
            }
         }
         case IMAGES -> {
            y = this.section(y, 0);
            this.rows.add(new Row("image", 0, y, IMAGE_H));
            y += IMAGE_H + ROW_GAP;
            this.rows.add(new Row("image", 1, y, IMAGE_H));
            y += IMAGE_H + ROW_GAP;
            this.rows.add(new Row("hint", 0, y, 20.0F));
            y += 20.0F + ROW_GAP;
         }
         case PRESETS -> {
            y = this.section(y, 0);
            for (int i = 0; i < TYPES.length; i++) {
               this.rows.add(new Row("type", i, y, LIST_H));
               y += LIST_H + 2.0F;
            }
            y = this.section(y, 1);
            List<TargetHudPreset> presets = this.presetManager.getPresets();
            if (presets.isEmpty()) {
               this.rows.add(new Row("hint", 0, y, 20.0F));
               y += 20.0F + ROW_GAP;
            } else {
               for (int i = 0; i < presets.size(); i++) {
                  this.rows.add(new Row("preset", i, y, LIST_H));
                  y += LIST_H + 2.0F;
               }
            }
            y += 6.0F;
            this.rows.add(new Row("button", 0, y, BUTTON_H));
            y += BUTTON_H + ROW_GAP;
            this.rows.add(new Row("button", 1, y, BUTTON_H));
            y += BUTTON_H + ROW_GAP;
         }
         case LAYOUT -> {
            y = this.section(y, 0);
            for (int i = 0; i < SCREEN_VIEWS.length; i++) {
               this.rows.add(new Row("view", i, y, LIST_H));
               y += LIST_H + 2.0F;
            }
            y = this.section(y, 1);
            for (int i = 0; i < this.tabOrder.size(); i++) {
               this.rows.add(new Row("order", i, y, LIST_H));
               y += LIST_H + 2.0F;
            }
         }
      }
      y += 8.0F;
      this.contentHeight = y;
      float maxScroll = Math.max(y - (this.panelH() - 56.0F), 0.0F);
      this.contentScroll[this.selectedTab.ordinal()] = MathHelper.clamp(this.contentScroll[this.selectedTab.ordinal()], 0.0F, maxScroll);
   }

   private float section(float y, int index) {
      y += 8.0F;
      this.rows.add(new Row("section", index, y, SECTION_H));
      return y + SECTION_H;
   }

   private float rowY(Row row) {
      return this.contentTop() + 48.0F + row.y - this.contentScroll[this.selectedTab.ordinal()];
   }

   private float frameContentTop() {
      return this.contentTop();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      this.buildRows();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 148));

      if (this.view() == ScreenView.FLOAT) {
         this.renderFloatWindow(draw);
      }

      this.renderHeader(draw, mouseX, mouseY);
      this.renderContent(draw, mouseX, mouseY);
      this.renderTabs(draw, mouseX, mouseY);
      this.renderBottomBar(draw, mouseX, mouseY);

      if (this.view() == ScreenView.DOCKED) {
         this.renderDockPanel(draw);
      }

      this.renderPreview(draw, mouseX, mouseY);

      if (this.importOpen) {
         this.renderImportDialog(draw, mouseX, mouseY);
      }
   }

   private void renderFloatWindow(CustomDrawContext draw) {
      float x = this.frameX();
      float y = this.frameY();
      float w = FLOAT_W;
      float h = FLOAT_H;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x - 8.0F, y - 8.0F, w + 16.0F, h + 16.0F, BorderRadius.all(16.0F), new ColorRGBA(0, 0, 0, 90));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(12.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(12.0F), this.border().withAlpha(200));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, FLOAT_TITLE_H, BorderRadius.all(12.0F), this.accent().withAlpha(30));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y + FLOAT_TITLE_H - 1.0F, w, 1.0F, BorderRadius.all(0.5F), this.border().withAlpha(90));
   }

   private void renderHeader(CustomDrawContext draw, float mouseX, float mouseY) {
      float x0 = this.frameX();
      float titleSize = this.view() == ScreenView.MINIMAL ? 8.0F : 9.0F;
      String title = this.view() == ScreenView.FLOAT ? "Редактор таргетхуда — окно" : "Редактор таргетхуда";
      draw.drawText(Fonts.SEMIBOLD.getFont(titleSize), title, x0 + MARGIN, this.view() == ScreenView.MINIMAL ? this.frameY() + 10.0F : 14.0F, TEXT);

      if (this.view() != ScreenView.FLOAT && this.view() != ScreenView.MINIMAL) {
         draw.drawText(Fonts.REGULAR.getFont(5.5F), "Здесь настраивается таргетхуд · Enter — сохранить", x0 + MARGIN + Fonts.SEMIBOLD.getWidth("Редактор таргетхуда", 9.0F) + 12.0F, 17.5F, DIM);
      }

      float x = this.closeX();
      float y = this.closeY();
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, 26.0F, 24.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, 26.0F, 24.0F, BorderRadius.all(6.0F), hovered ? this.accent().withAlpha(150) : new ColorRGBA(24, 24, 28).withAlpha(220));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, 26.0F, 24.0F, 1.0F, BorderRadius.all(6.0F), hovered ? this.accent().withAlpha(170) : this.border());
      draw.drawText(Fonts.REGULAR.getFont(8.0F), "✕", x + 13.0F - Fonts.REGULAR.getWidth("✕", 8.0F) / 2.0F, y + 6.0F, TEXT);

      if (this.view() != ScreenView.FLOAT && this.view() != ScreenView.MINIMAL) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.frameX() + MARGIN, HEADER_H - 1.0F, this.frameW() - MARGIN * 2.0F, 1.0F, BorderRadius.all(0.5F), this.border().withAlpha(90));
      }
   }

   private float closeX() {
      if (this.view() == ScreenView.FLOAT) {
         return this.frameX() + FLOAT_W - MARGIN - 26.0F;
      }
      if (this.view() == ScreenView.MINIMAL) {
         return this.frameX() + MIN_W - MARGIN - 26.0F;
      }
      return this.frameX() + this.frameW() - MARGIN - 26.0F;
   }

   private float closeY() {
      if (this.view() == ScreenView.FLOAT) {
         return this.frameY() + (FLOAT_TITLE_H - 24.0F) / 2.0F;
      }
      if (this.view() == ScreenView.MINIMAL) {
         return this.frameY() + 10.0F;
      }
      return 12.0F;
   }

   private void renderTabs(CustomDrawContext draw, float mouseX, float mouseY) {
      switch (this.view()) {
         case TOP -> this.renderTopTabs(draw, mouseX, mouseY);
         case CARDS -> this.renderCardsTabs(draw, mouseX, mouseY);
         case MINIMAL -> this.renderMinimalTabs(draw, mouseX, mouseY);
         default -> this.renderRailTabs(draw, mouseX, mouseY);
      }
   }

   private void renderRailTabs(CustomDrawContext draw, float mouseX, float mouseY) {
      float x = this.frameX() + MARGIN;
      float y = this.tableTop();
      float w = this.railW();
      float h = this.bottomBarY() - y;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(8.0F), this.border());
      draw.drawText(Fonts.SEMIBOLD.getFont(6.0F), "РАЗДЕЛЫ".toUpperCase(Locale.ROOT), x + 14.0F, y + 12.0F, DIM);

      List<Tab> tabs = this.visibleTabs();
      for (int i = 0; i < tabs.size(); i++) {
         Tab tab = tabs.get(i);
         float[] r = this.tabRect(i);
         boolean selected = tab == this.selectedTab;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, r[0], r[1], r[2], r[3]);
         DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1], r[2], r[3], BorderRadius.all(6.0F), selected ? this.accent().withAlpha(55) : hovered ? ROW_HOVER : new ColorRGBA(0, 0, 0, 0));
         if (selected) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1] + 6.0F, 3.0F, r[3] - 12.0F, BorderRadius.all(1.5F), this.accent());
         }
         draw.drawText(Fonts.REGULAR.getFont(6.0F), tab.label, r[0] + 12.0F, r[1] + 9.5F, selected ? TEXT : SUBTEXT);
      }
   }

   private void renderTopTabs(CustomDrawContext draw, float mouseX, float mouseY) {
      float x = this.frameX();
      float y = this.frameY() + TOP_Y;
      float w = this.frameW();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, TAB_STRIP_H, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, TAB_STRIP_H, 1.0F, BorderRadius.all(8.0F), this.border());

      List<Tab> tabs = this.visibleTabs();
      for (int i = 0; i < tabs.size(); i++) {
         Tab tab = tabs.get(i);
         float[] r = this.tabRect(i);
         boolean selected = tab == this.selectedTab;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, r[0], r[1], r[2], r[3]);
         DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1], r[2], r[3], BorderRadius.all(6.0F), selected ? this.accent().withAlpha(55) : hovered ? ROW_HOVER : new ColorRGBA(0, 0, 0, 0));
         if (selected) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1] + 6.0F, 3.0F, r[3] - 12.0F, BorderRadius.all(1.5F), this.accent());
         }
         float labelW = Fonts.REGULAR.getWidth(tab.label, 6.0F);
         draw.drawText(Fonts.REGULAR.getFont(6.0F), tab.label, r[0] + r[2] / 2.0F - labelW / 2.0F, r[1] + 9.5F, selected ? TEXT : SUBTEXT);
      }
   }

   private void renderCardsTabs(CustomDrawContext draw, float mouseX, float mouseY) {
      float x = this.frameX() + MARGIN;
      float y = this.tableTop();
      float w = this.railW();
      float h = this.bottomBarY() - y;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(8.0F), this.border());
      draw.drawText(Fonts.SEMIBOLD.getFont(6.0F), "РАЗДЕЛЫ".toUpperCase(Locale.ROOT), x + 14.0F, y + 12.0F, DIM);

      List<Tab> tabs = this.visibleTabs();
      for (int i = 0; i < tabs.size(); i++) {
         Tab tab = tabs.get(i);
         float[] r = this.tabRect(i);
         boolean selected = tab == this.selectedTab;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, r[0], r[1], r[2], r[3]);
         DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1], r[2], r[3], BorderRadius.all(8.0F), selected ? this.accent().withAlpha(80) : hovered ? ROW_HOVER : CARD);
         if (selected) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), r[0], r[1], r[2], r[3], 1.5F, BorderRadius.all(8.0F), this.accent());
         }
         draw.drawText(Fonts.SEMIBOLD.getFont(6.0F), tab.label, r[0] + r[2] / 2.0F - Fonts.SEMIBOLD.getWidth(tab.label, 6.0F) / 2.0F, r[1] + 9.0F, selected ? TEXT : SUBTEXT);
      }
   }

   private void renderMinimalTabs(CustomDrawContext draw, float mouseX, float mouseY) {
      List<Tab> tabs = this.visibleTabs();
      for (int i = 0; i < tabs.size(); i++) {
         Tab tab = tabs.get(i);
         float[] r = this.tabRect(i);
         boolean selected = tab == this.selectedTab;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, r[0], r[1], r[2], r[3]);
         DrawUtil.drawRoundedRect(draw.getMatrices(), r[0], r[1], r[2], r[3], BorderRadius.all(13.0F), selected ? this.accent().withAlpha(90) : hovered ? ROW_HOVER : new ColorRGBA(0, 0, 0, 0));
         float labelW = Fonts.REGULAR.getWidth(tab.label, 6.0F);
         draw.drawText(Fonts.REGULAR.getFont(6.0F), tab.label, r[0] + r[2] / 2.0F - labelW / 2.0F, r[1] + 7.0F, selected ? TEXT : SUBTEXT);
      }
   }

   private void renderContent(CustomDrawContext draw, float mouseX, float mouseY) {
      float x = this.contentX();
      float y = this.contentTop();
      float w = this.contentW();
      float py = this.view() == ScreenView.MINIMAL ? this.frameY() : y;
      float h = this.bottomBarY() - py;

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, py, w, h, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, py, w, h, 1.0F, BorderRadius.all(8.0F), this.border());

      draw.drawText(Fonts.SEMIBOLD.getFont(7.0F), this.selectedTab.label, x + PAD, y + 12.0F, TEXT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PAD, y + 34.0F, w - PAD * 2.0F, 1.0F, BorderRadius.all(0.5F), this.border().withAlpha(90));

      float scrollTop = y + 42.0F;
      float scrollH = h - 42.0F - 6.0F;
      draw.enableScissor((int) x, (int) scrollTop, (int) (x + w), (int) (scrollTop + scrollH));

      for (Row row : this.rows) {
         float rowY = this.rowY(row);
         if (rowY < scrollTop - row.h || rowY > scrollTop + scrollH) {
            continue;
         }
         this.renderRow(draw, row, rowY, x + PAD, w - PAD * 2.0F, mouseX, mouseY);
      }
      draw.disableScissor();

      this.renderScrollbar(draw, x + w - 7.0F, scrollTop, scrollH, this.contentScroll[this.selectedTab.ordinal()], this.contentHeight, scrollH);
   }

   private void renderRow(CustomDrawContext draw, Row row, float rowY, float x, float w, float mouseX, float mouseY) {
      ColorRGBA accent = this.accent();
      switch (row.kind) {
         case "section" -> {
            draw.drawText(Fonts.REGULAR.getFont(5.5F), this.sectionTitle(row.index).toUpperCase(Locale.ROOT), x, rowY + 1.0F, DIM);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY + 13.0F, 3.0F, 1.0F, BorderRadius.all(0.5F), accent.withAlpha(140));
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 7.0F, rowY + 13.5F, w - 7.0F, 1.0F, BorderRadius.all(0.5F), new ColorRGBA(60, 60, 68).withAlpha(120));
         }
         case "slider" -> {
            NumberSetting setting = this.tabSliders().get(row.index);
            this.renderSlider(draw, accent, rowY, x, w, setting, this.dragSlider == row.index);
         }
         case "toggle" -> {
            BooleanSetting setting = this.tabToggles().get(row.index);
            this.renderToggle(draw, accent, mouseX, rowY, x, w, setting);
         }
         case "mode" -> this.renderMode(draw, accent, rowY, x, w, mouseX, mouseY);
         case "color" -> this.renderColorRow(draw, accent, rowY, x, w, this.colors()[row.index], mouseX, mouseY);
         case "picker" -> {
            if (this.activeColor != null) {
               this.renderPicker(draw, accent, mouseX, mouseY, rowY, x, w);
            }
         }
         case "image" -> this.renderImageRow(draw, accent, mouseX, mouseY, row.index, rowY, x, w);
         case "type" -> this.renderTypeRow(draw, accent, row.index, rowY, x, w, mouseX, mouseY);
         case "preset" -> this.renderPresetRow(draw, accent, row.index, rowY, x, w, mouseX, mouseY);
         case "button" -> this.renderButton(draw, row.index, rowY, x, w, mouseX, mouseY);
         case "view" -> this.renderViewRow(draw, accent, row.index, rowY, x, w, mouseX, mouseY);
         case "order" -> this.renderOrderRow(draw, accent, row.index, rowY, x, w, mouseX, mouseY);
         case "hint" -> {
            if (this.selectedTab == Tab.IMAGES) {
               draw.drawText(Fonts.REGULAR.getFont(5.0F), "PNG-картинки из папки игры. Выбери файл в диалоге.", x, rowY + 2.0F, DIM);
            } else {
               draw.drawText(Fonts.REGULAR.getFont(5.5F), "Сохрани свой таргетхуд снизу", x, rowY + 2.0F, DIM);
            }
         }
      }
   }

   private String sectionTitle(int index) {
      return switch (this.selectedTab) {
         case BAR, HEAD -> "Настройки";
         case STYLE -> index == 0 ? "Отображение" : "Оформление";
         case COLORS -> "Цвета";
         case IMAGES -> "Фон и голова";
         case PRESETS -> index == 0 ? "Тип таргетхуда" : "Мои таргетхуды";
         case LAYOUT -> index == 0 ? "Виды экрана" : "Порядок вкладок";
      };
   }

   private void renderSlider(CustomDrawContext draw, ColorRGBA accent, float rowY, float x, float w, NumberSetting setting, boolean dragging) {
      String valueText = String.format(Locale.US, "%.2f", setting.getCurrent());
      float valueW = Fonts.REGULAR.getWidth(valueText, 5.5F);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), setting.getName(), x, rowY + 2.0F, SUBTEXT);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), valueText, x + w - valueW, rowY + 2.0F, TEXT);

      float target = (setting.getCurrent() - setting.getMin()) / (setting.getMax() - setting.getMin());
      float anim = this.sliderAnim.getOrDefault(setting, target);
      float k = EditClickGUI.INSTANCE.getSliderSmoothness();
      anim += (target - anim) * (dragging ? Math.max(k, 0.45F) : k);
      if (Math.abs(target - anim) < 0.002F) {
         anim = target;
      }
      this.sliderAnim.put(setting, anim);

      float trackY = rowY + 16.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, w, 3.0F, BorderRadius.all(1.5F), new ColorRGBA(58, 58, 64).withAlpha(200));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, w * anim, 3.0F, BorderRadius.all(1.5F), accent.withAlpha(dragging ? 255 : 220));
      float knobX = x + w * anim - 5.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX, trackY - 3.5F, 10.0F, 10.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), knobX, trackY - 3.5F, 10.0F, 10.0F, 1.0F, BorderRadius.all(5.0F), accent.withAlpha(dragging ? 255 : 180));
   }

   private void renderToggle(CustomDrawContext draw, ColorRGBA accent, float mouseX, float rowY, float x, float w, BooleanSetting setting) {
      boolean enabled = setting.isEnabled();
      float anim = this.toggleAnim.getOrDefault(setting, 0.0F);
      anim += ((enabled ? 1.0F : 0.0F) - anim) * Math.min(1.0F, mc.getRenderTickCounter().getTickDelta(false) * 25.0F);
      if (Math.abs((enabled ? 1.0F : 0.0F) - anim) < 0.002F) {
         anim = enabled ? 1.0F : 0.0F;
      }
      this.toggleAnim.put(setting, anim);

      draw.drawText(Fonts.REGULAR.getFont(6.0F), setting.getName(), x, rowY + 4.0F, SUBTEXT);
      float sw = 36.0F;
      float sh = 16.0F;
      float tx = x + w - sw;
      boolean hovered = MathUtil.isHovered(mouseX, rowY, tx, rowY + 3.0F, sw, sh);
      ToggleSwitch.render(draw, tx, rowY + 2.0F, sw, sh, this.toggleAnim.getOrDefault(setting, 0.0F), accent.brighter(0.15F), new ColorRGBA(48, 48, 54), hovered ? 1.0F : 0.9F);
   }

   private void renderMode(CustomDrawContext draw, ColorRGBA accent, float rowY, float x, float w, float mouseX, float mouseY) {
      float modeW = (w - 8.0F) / 3.0F;
      for (int i = 0; i < MODES.length; i++) {
         float bx = x + i * (modeW + 4.0F);
         boolean selected = this.module.displayMode.is(MODES[i]);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, bx, rowY, modeW, 26.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), bx, rowY, modeW, 26.0F, BorderRadius.all(6.0F), selected ? accent.withAlpha(190) : hovered ? ROW_HOVER : CARD);
         if (selected) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), bx, rowY, modeW, 26.0F, 1.0F, BorderRadius.all(6.0F), accent.brighter(0.15F).withAlpha(140));
         }
         float labelW = Fonts.REGULAR.getWidth(MODES[i], 5.5F);
         draw.drawText(Fonts.REGULAR.getFont(5.5F), MODES[i], bx + modeW / 2.0F - labelW / 2.0F, rowY + 8.0F, TEXT);
      }
   }

   private void renderColorRow(CustomDrawContext draw, ColorRGBA accent, float rowY, float x, float w, ColorSetting setting, float mouseX, float mouseY) {
      float alpha = this.module.customColors.isEnabled() ? 1.0F : 0.35F;
      boolean active = this.activeColor == setting;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, 22.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY + 1.0F, w, 22.0F, BorderRadius.all(6.0F), active ? accent.withAlpha(70) : hovered ? ROW_HOVER : CARD);
      if (active) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, rowY + 4.0F, 3.0F, 16.0F, BorderRadius.all(1.5F), accent);
      }
      draw.drawText(Fonts.REGULAR.getFont(6.0F), this.colorName(setting), x + 12.0F, rowY + 6.0F, TEXT.withAlpha(255.0F * alpha));
      float sx = x + w - 18.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), sx, rowY + 5.0F, 14.0F, 14.0F, BorderRadius.all(7.0F), setting.getColor().withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), sx, rowY + 5.0F, 14.0F, 14.0F, 1.0F, BorderRadius.all(7.0F), new ColorRGBA(0, 0, 0).withAlpha(140));
   }

   private String colorName(ColorSetting setting) {
      ColorSetting[] colors = this.colors();
      for (int i = 0; i < colors.length; i++) {
         if (colors[i] == setting) {
            return COLOR_NAMES[i];
         }
      }
      return "Цвет";
   }

   private void renderPicker(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY, float py, float x, float w) {
      float pickerW = w - 26.0F;
      if (this.draggingPicker) {
         float saturation = MathHelper.clamp((mouseX - x - 4.0F) / (pickerW - 8.0F), 0.0F, 1.0F);
         float brightness = 1.0F - MathHelper.clamp((mouseY - py - 4.0F) / (PICKER_H - 8.0F), 0.0F, 1.0F);
         this.hsb[1] = saturation;
         this.hsb[2] = brightness;
         this.applyColor();
      }
      if (this.draggingHue) {
         this.hsb[0] = MathHelper.clamp((mouseY - py) / PICKER_H, 0.0F, 1.0F);
         this.applyColor();
      }
      float alpha = this.module.customColors.isEnabled() ? 1.0F : 0.35F;

      ColorRGBA hue = ColorRGBA.fromHSB(this.hsb[0], 1.0F, 1.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, py, pickerW, PICKER_H, BorderRadius.all(6.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), ColorRGBA.BLACK.withAlpha(255.0F * alpha), hue.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, py, pickerW, PICKER_H, 1.0F, BorderRadius.all(6.0F), new ColorRGBA(0, 0, 0).withAlpha(160));

      float knobX = x + 4.0F + this.hsb[1] * (pickerW - 8.0F);
      float knobY = py + 4.0F + (1.0F - this.hsb[2]) * (PICKER_H - 8.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(180.0F * alpha));
      DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));

      float hueX = x + pickerW + 8.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), hueX - 4.0F, py, 14.0F, PICKER_H, BorderRadius.all(7.0F), new ColorRGBA(20, 20, 24).withAlpha(255.0F * alpha));
      DrawUtil.drawHueBar(draw.getMatrices(), hueX - 2.0F, py, 10.0F, PICKER_H, 255.0F * alpha);
      float knobY2 = py + this.hsb[0] * PICKER_H;
      DrawUtil.drawRoundedBorder(draw.getMatrices(), hueX - 4.0F, py, 14.0F, PICKER_H, 1.0F, BorderRadius.all(7.0F), new ColorRGBA(0, 0, 0).withAlpha(140));
      DrawUtil.drawRoundedRect(draw.getMatrices(), hueX - 3.0F, knobY2 - 3.0F, 12.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
   }

   private void renderImageRow(CustomDrawContext draw, ColorRGBA accent, float mouseX, float mouseY, int index, float rowY, float x, float w) {
      StringSetting setting = index == 0 ? this.module.bgImage : this.module.headImage;
      String label = index == 0 ? "Импорт фона" : "Импорт головы";
      String value = setting.getValue();
      boolean hasImage = !value.isEmpty();
      boolean error = this.module.hasImageError(value);

      float importW = 130.0F;
      boolean importHovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, importW, 20.0F);
      boolean clearHovered = MathUtil.isHovered(mouseX, mouseY, x + importW + 4.0F, rowY, 30.0F, 20.0F);
      ColorRGBA importBg;
      if (error) {
         importBg = DANGER.withAlpha(importHovered ? 255 : 210);
      } else if (hasImage) {
         importBg = accent.withAlpha(importHovered ? 255 : 200);
      } else {
         importBg = importHovered ? accent.withAlpha(110) : CARD;
      }
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, importW, 20.0F, BorderRadius.all(6.0F), importBg);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), label, x + importW / 2.0F - Fonts.REGULAR.getWidth(label, 6.0F) / 2.0F, rowY + 6.0F, TEXT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + importW + 4.0F, rowY, 30.0F, 20.0F, BorderRadius.all(6.0F), clearHovered ? DANGER.withAlpha(210) : CARD);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "✕", x + importW + 4.0F + 15.0F - Fonts.REGULAR.getWidth("✕", 6.0F) / 2.0F, rowY + 6.0F, TEXT);

      ColorRGBA statusColor;
      String status;
      if (value.isEmpty()) {
         status = "не выбрано";
         statusColor = DIM;
      } else if (error) {
         status = "ошибка: " + this.trimPath(value);
         statusColor = DANGER;
      } else {
         status = this.trimPath(value);
         statusColor = new ColorRGBA(150, 220, 150);
      }
      draw.drawText(Fonts.REGULAR.getFont(5.0F), status, x + importW + 4.0F + 34.0F, rowY + 6.0F, statusColor);
   }

   private void renderTypeRow(CustomDrawContext draw, ColorRGBA accent, int index, float rowY, float x, float w, float mouseX, float mouseY) {
      boolean selected = this.module.type.is(TYPES[index]);
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, w, LIST_H - 2.0F, BorderRadius.all(5.0F), selected ? accent.withAlpha(60) : hovered ? ROW_HOVER : new ColorRGBA(0, 0, 0, 0));
      if (selected) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, rowY + 4.5F, 3.0F, LIST_H - 13.0F, BorderRadius.all(1.5F), accent);
      }
      draw.drawText(Fonts.REGULAR.getFont(6.0F), TYPES[index], x + 12.0F, rowY + 5.5F, selected ? TEXT : SUBTEXT);
   }

   private void renderPresetRow(CustomDrawContext draw, ColorRGBA accent, int index, float rowY, float x, float w, float mouseX, float mouseY) {
      TargetHudPreset preset = this.presetManager.getPresets().get(index);
      boolean applied = preset.getName().equals(this.appliedName);
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, w, LIST_H - 2.0F, BorderRadius.all(5.0F), applied ? accent.withAlpha(55) : hovered ? ROW_HOVER : CARD);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 6.0F, rowY + 4.5F, 12.0F, 12.0F, BorderRadius.all(3.5F), new ColorRGBA(preset.getBarColor()));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), preset.getName(), x + 24.0F, rowY + 5.5F, applied ? TEXT : SUBTEXT);

      boolean delHovered = MathUtil.isHovered(mouseX, mouseY, x + w - 22.0F, rowY, 18.0F, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - 22.0F, rowY + 3.5F, 18.0F, LIST_H - 9.0F, BorderRadius.all(4.0F), delHovered ? DANGER.withAlpha(160) : new ColorRGBA(0, 0, 0, 0));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "✕", x + w - 13.0F - Fonts.REGULAR.getWidth("✕", 5.5F) / 2.0F, rowY + 6.0F, delHovered ? TEXT : DIM);
   }

   private void renderViewRow(CustomDrawContext draw, ColorRGBA accent, int index, float rowY, float x, float w, float mouseX, float mouseY) {
      boolean selected = index == this.screenViewIndex;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, w, LIST_H - 2.0F, BorderRadius.all(5.0F), selected ? accent.withAlpha(60) : hovered ? ROW_HOVER : new ColorRGBA(0, 0, 0, 0));
      if (selected) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, rowY + 4.5F, 3.0F, LIST_H - 13.0F, BorderRadius.all(1.5F), accent);
      }
      draw.drawText(Fonts.REGULAR.getFont(6.0F), VIEW_LABEL[index], x + 12.0F, rowY + 4.0F, selected ? TEXT : SUBTEXT);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), VIEW_DESC[index], x + 12.0F, rowY + 12.5F, DIM);
   }

   private void renderOrderRow(CustomDrawContext draw, ColorRGBA accent, int index, float rowY, float x, float w, float mouseX, float mouseY) {
      Tab tab = this.tabOrder.get(index);
      boolean up = index > 0;
      boolean down = index < this.tabOrder.size() - 1;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, w, LIST_H - 2.0F, BorderRadius.all(5.0F), hovered ? ROW_HOVER : CARD);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), String.valueOf(index + 1) + ". " + tab.label, x + 12.0F, rowY + 5.5F, this.selectedTab == tab ? TEXT : SUBTEXT);

      float upX = x + w - 46.0F;
      boolean upHovered = up && MathUtil.isHovered(mouseX, mouseY, upX, rowY, 20.0F, LIST_H - 2.0F);
      boolean downHovered = down && MathUtil.isHovered(mouseX, mouseY, upX + 24.0F, rowY, 20.0F, LIST_H - 2.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), upX, rowY + 3.5F, 20.0F, LIST_H - 9.0F, BorderRadius.all(4.0F), upHovered ? accent.withAlpha(120) : new ColorRGBA(0, 0, 0, 0));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "▲", upX + 10.0F - Fonts.REGULAR.getWidth("▲", 6.0F) / 2.0F, rowY + 5.5F, up ? (upHovered ? TEXT : DIM) : new ColorRGBA(70, 70, 78).withAlpha(120));
      DrawUtil.drawRoundedRect(draw.getMatrices(), upX + 24.0F, rowY + 3.5F, 20.0F, LIST_H - 9.0F, BorderRadius.all(4.0F), downHovered ? accent.withAlpha(120) : new ColorRGBA(0, 0, 0, 0));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "▼", upX + 34.0F - Fonts.REGULAR.getWidth("▼", 6.0F) / 2.0F, rowY + 5.5F, down ? (downHovered ? TEXT : DIM) : new ColorRGBA(70, 70, 78).withAlpha(120));
   }

   private void renderButton(CustomDrawContext draw, int index, float rowY, float x, float w, float mouseX, float mouseY) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, rowY, w, 22.0F);
      String label = index == 0 ? "Сбросить позицию" : "Сбросить настройки";
      ColorRGBA bg = index == 1 ? (hovered ? DANGER.withAlpha(180) : new ColorRGBA(42, 42, 48).withAlpha(255)) : (hovered ? this.accent().withAlpha(130) : new ColorRGBA(42, 42, 48).withAlpha(255));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, rowY, w, 22.0F, BorderRadius.all(6.0F), bg);
      draw.drawText(Fonts.REGULAR.getFont(6.0F), label, x + w / 2.0F - Fonts.REGULAR.getWidth(label, 6.0F) / 2.0F, rowY + 7.0F, TEXT);
   }

   private void renderBottomBar(CustomDrawContext draw, float mouseX, float mouseY) {
      float y = this.bottomBarY();
      float x0 = this.view() == ScreenView.FLOAT ? this.frameX() : this.view() == ScreenView.MINIMAL ? this.frameX() : MARGIN;
      float w0 = this.view() == ScreenView.FLOAT ? FLOAT_W - MARGIN * 2.0F : this.view() == ScreenView.MINIMAL ? MIN_W - MARGIN * 2.0F : this.width - MARGIN * 2.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x0, y, w0, BOTTOM_BAR_H, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x0, y, w0, BOTTOM_BAR_H, 1.0F, BorderRadius.all(8.0F), this.border());

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Название", x0 + PAD, y + 8.0F, SUBTEXT);
      float fieldX = x0 + PAD + Fonts.REGULAR.getWidth("Название", 6.0F) + 10.0F;
      float fieldW = 200.0F;
      float fieldH = 22.0F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, fieldX, y + 7.0F, fieldW, fieldH);
      DrawUtil.drawRoundedRect(draw.getMatrices(), fieldX, y + 7.0F, fieldW, fieldH, BorderRadius.all(6.0F), INPUT_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), fieldX, y + 7.0F, fieldW, fieldH, 1.0F, BorderRadius.all(6.0F), this.nameInput.focused ? this.accent().withAlpha(220) : hovered ? new ColorRGBA(100, 100, 110).withAlpha(110) : new ColorRGBA(70, 70, 78).withAlpha(90));
      if (this.nameInput.text.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(6.0F), this.nameInput.focused ? "" : "Название таргетхуда", fieldX + 7.0F, y + 9.5F, DIM);
      } else {
         draw.drawText(Fonts.REGULAR.getFont(6.0F), this.nameInput.text, fieldX + 7.0F, y + 9.5F, TEXT);
      }
      if (this.nameInput.focused && System.currentTimeMillis() % 1000L < 500L) {
         float caretX = fieldX + 7.0F + Fonts.REGULAR.getWidth(this.nameInput.text, 6.0F) + 2.0F;
         DrawUtil.drawRect(draw.getMatrices(), caretX, y + 9.0F, 1.0F, 10.0F, TEXT);
      }

      float buttonX = fieldX + fieldW + 8.0F;
      float buttonW = 170.0F;
      boolean saveHovered = MathUtil.isHovered(mouseX, mouseY, buttonX, y + 7.0F, buttonW, fieldH);
      DrawUtil.drawRoundedRect(draw.getMatrices(), buttonX, y + 7.0F, buttonW, fieldH, BorderRadius.all(6.0F), this.accent().withAlpha(saveHovered ? 255 : 150));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), buttonX, y + 7.0F, buttonW, fieldH, 1.0F, BorderRadius.all(6.0F), this.accent().brighter(0.2F).withAlpha(saveHovered ? 190 : 120));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Сохранить таргетхуд", buttonX + buttonW / 2.0F - Fonts.REGULAR.getWidth("Сохранить таргетхуд", 6.0F) / 2.0F, y + 9.5F, TEXT);
   }

   private void renderDockPanel(CustomDrawContext draw) {
      float x = this.dockX();
      float y = this.contentTop();
      float w = DOCK_W;
      float h = this.bottomBarY() - y;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(8.0F), PANEL_BG);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(8.0F), this.border());
      draw.drawText(Fonts.SEMIBOLD.getFont(6.0F), "ПРЕВЬЮ".toUpperCase(Locale.ROOT), x + 14.0F, y + 12.0F, DIM);

      float[] size = this.module.currentSize();
      float ox = x + (DOCK_W - size[0]) / 2.0F;
      float oy = y + 90.0F;
      float savedX = this.module.x.getCurrent();
      float savedY = this.module.y.getCurrent();
      this.module.x.setCurrent(ox);
      this.module.y.setCurrent(oy);
      try {
         this.module.renderPreview(draw);
      } catch (Exception e) {
         e.printStackTrace();
      }
      this.module.x.setCurrent(savedX);
      this.module.y.setCurrent(savedY);
      String hint = "Превью фиксировано здесь";
      draw.drawText(Fonts.REGULAR.getFont(5.0F), hint, x + DOCK_W / 2.0F - Fonts.REGULAR.getWidth(hint, 5.0F) / 2.0F, y + h - 18.0F, DIM);
   }

   private float dockX() {
      return this.frameX() + this.frameW() - MARGIN - DOCK_W;
   }

   private void renderPreview(CustomDrawContext draw, float mouseX, float mouseY) {
      if (this.view() == ScreenView.DOCKED) {
         return;
      }
      try {
         this.module.renderPreview(draw);
      } catch (Exception e) {
         e.printStackTrace();
      }
      float[] size = this.module.currentSize();
      float hx = this.module.x.getCurrent();
      float hy = this.module.y.getCurrent();
      if (this.dragging || MathUtil.isHovered(mouseX, mouseY, hx, hy, size[0], size[1])) {
         DrawUtil.drawRoundedBorder(draw.getMatrices(), hx - 1.0F, hy - 1.0F, size[0] + 2.0F, size[1] + 2.0F, 1.0F, BorderRadius.all(6.0F), this.accent().withAlpha(255));
      }
   }

   private void renderScrollbar(CustomDrawContext draw, float x, float y, float h, float scroll, float contentH, float visibleH) {
      if (contentH <= visibleH + 0.5F) {
         return;
      }
      float thumbH = Math.max(24.0F, visibleH * (visibleH / contentH));
      float maxScroll = contentH - visibleH;
      float thumbY = y + (h - thumbH) * (scroll / maxScroll);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, 3.0F, h, BorderRadius.all(1.5F), new ColorRGBA(255, 255, 255).withAlpha(24));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, thumbY, 3.0F, thumbH, BorderRadius.all(1.5F), this.accent().withAlpha(170));
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

      float cx = this.closeX();
      float cy = this.closeY();
      if (MathUtil.isHovered(mouseX, mouseY, cx, cy, 26.0F, 24.0F)) {
         this.close();
         return true;
      }

      if (this.inRail(mouseX, mouseY) || this.inTabStrip(mouseX, mouseY)) {
         this.handleTabClick(mouseX, mouseY);
         return true;
      }
      if (this.inContent(mouseX, mouseY)) {
         if (this.handleContentClick(mouseX, mouseY)) {
            return true;
         }
      }
      if (this.handleBottomBarClick(mouseX, mouseY)) {
         return true;
      }

      float[] size = this.module.currentSize();
      float hx = this.module.x.getCurrent();
      float hy = this.module.y.getCurrent();
      if (this.view() != ScreenView.DOCKED && MathUtil.isHovered(mouseX, mouseY, hx, hy, size[0], size[1])) {
         this.dragging = true;
         this.dragOffsetX = (float) mouseX - hx;
         this.dragOffsetY = (float) mouseY - hy;
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void handleTabClick(double mouseX, double mouseY) {
      List<Tab> tabs = this.visibleTabs();
      for (int i = 0; i < tabs.size(); i++) {
         if (this.hitTab(i, mouseX, mouseY)) {
            Tab tab = tabs.get(i);
            if (this.selectedTab != tab) {
               this.selectedTab = tab;
               this.dragSlider = -1;
               this.activeColor = null;
            }
            return;
         }
      }
   }

   private boolean handleContentClick(double mouseX, double mouseY) {
      for (Row row : this.rows) {
         float rowY = this.rowY(row);
         if (mouseY < rowY || mouseY > rowY + row.h) {
            continue;
         }
         float x = this.contentX() + PAD;
         float w = this.contentW() - PAD * 2.0F;
         switch (row.kind) {
            case "slider" -> {
               if (MathUtil.isHovered(mouseX, mouseY, x, rowY, w, 24.0F)) {
                  this.dragSlider = row.index;
                  this.updateSliderValue(mouseX);
                  return true;
               }
            }
            case "toggle" -> {
               BooleanSetting setting = this.tabToggles().get(row.index);
               float sw = 36.0F;
               float tx = x + w - sw;
               if (MathUtil.isHovered(mouseX, mouseY, tx, rowY + 2.0F, sw, 16.0F)) {
                  setting.setEnabled(!setting.isEnabled());
                  return true;
               }
            }
            case "mode" -> {
               float modeW = (w - 8.0F) / 3.0F;
               for (int i = 0; i < MODES.length; i++) {
                  if (MathUtil.isHovered(mouseX, mouseY, x + i * (modeW + 4.0F), rowY, modeW, 26.0F)) {
                     this.module.displayMode.set(MODES[i]);
                     return true;
                  }
               }
            }
            case "color" -> {
               ColorSetting setting = this.colors()[row.index];
               if (MathUtil.isHovered(mouseX, mouseY, x, rowY, w, 22.0F)) {
                  if (this.activeColor == setting) {
                     this.activeColor = null;
                  } else {
                     this.activeColor = setting;
                     this.initHsb(setting);
                  }
                  return true;
               }
            }
            case "picker" -> {
               if (this.activeColor != null) {
                  float pickerW = w - 26.0F;
                  float hueX = x + pickerW + 8.0F;
                  if (MathUtil.isHovered(mouseX, mouseY, hueX - 4.0F, rowY, 14.0F, PICKER_H)) {
                     this.draggingHue = true;
                     return true;
                  }
                  if (MathUtil.isHovered(mouseX, mouseY, x, rowY, pickerW, PICKER_H)) {
                     this.draggingPicker = true;
                     return true;
                  }
               }
            }
            case "image" -> {
               StringSetting setting = row.index == 0 ? this.module.bgImage : this.module.headImage;
               if (MathUtil.isHovered(mouseX, mouseY, x, rowY, 130.0F, 20.0F)) {
                  this.openImport(setting);
                  return true;
               }
               if (MathUtil.isHovered(mouseX, mouseY, x + 134.0F, rowY, 30.0F, 20.0F)) {
                  setting.setValue("");
                  this.module.clearImageCache();
                  return true;
               }
            }
            case "type" -> {
               this.module.type.set(TYPES[row.index]);
               return true;
            }
            case "preset" -> {
               TargetHudPreset preset = this.presetManager.getPresets().get(row.index);
               if (MathUtil.isHovered(mouseX, mouseY, x + w - 22.0F, rowY, 18.0F, LIST_H - 2.0F)) {
                  this.presetManager.deletePreset(preset.getName());
                  if (this.appliedName.equals(preset.getName())) {
                     this.appliedName = "";
                  }
                  return true;
               }
               this.module.applyPreset(preset);
               this.appliedName = preset.getName();
               this.nameInput.text = preset.getName();
               return true;
            }
            case "button" -> {
               if (row.index == 0) {
                  this.module.x.setCurrent(4.0F);
                  this.module.y.setCurrent(4.0F);
               } else {
                  this.resetAll();
               }
               return true;
            }
            case "view" -> {
               this.screenViewIndex = row.index;
               return true;
            }
            case "order" -> {
               List<Tab> tabs = this.tabOrder;
               if (row.index > 0 && MathUtil.isHovered(mouseX, mouseY, x + w - 46.0F, rowY, 20.0F, LIST_H - 2.0F)) {
                  Collections.swap(tabs, row.index, row.index - 1);
                  return true;
               }
               if (row.index < tabs.size() - 1 && MathUtil.isHovered(mouseX, mouseY, x + w - 22.0F, rowY, 20.0F, LIST_H - 2.0F)) {
                  Collections.swap(tabs, row.index, row.index + 1);
                  return true;
               }
               this.selectedTab = tabs.get(row.index);
               return true;
            }
            default -> {
            }
         }
      }
      return false;
   }

   private boolean handleBottomBarClick(double mouseX, double mouseY) {
      float y = this.bottomBarY();
      float x0 = this.view() == ScreenView.FLOAT ? this.frameX() : this.view() == ScreenView.MINIMAL ? this.frameX() : MARGIN;
      float fieldX = x0 + PAD + Fonts.REGULAR.getWidth("Название", 6.0F) + 10.0F;
      if (MathUtil.isHovered(mouseX, mouseY, fieldX, y + 7.0F, 200.0F, 22.0F)) {
         this.nameInput.focused = true;
         return true;
      }
      if (MathUtil.isHovered(mouseX, mouseY, fieldX + 200.0F + 8.0F, y + 7.0F, 170.0F, 22.0F)) {
         this.savePreset();
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
      if (button == 0) {
         this.dragSlider = -1;
         this.dragging = false;
         this.draggingPicker = false;
         this.draggingHue = false;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.importOpen) {
         this.importScroll -= (float) verticalAmount * 12.0F;
         return true;
      }
      if (this.inContent(mouseX, mouseY)) {
         float[] scroll = this.contentScroll;
         scroll[this.selectedTab.ordinal()] -= (float) verticalAmount * 12.0F;
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
      List<NumberSetting> sliders = this.tabSliders();
      if (this.dragSlider < 0 || this.dragSlider >= sliders.size()) {
         return;
      }
      NumberSetting setting = sliders.get(this.dragSlider);
      float trackW = this.contentW() - PAD * 2.0F;
      float x = this.contentX() + PAD;
      float percent = MathHelper.clamp((float) (mouseX - x) / trackW, 0.0F, 1.0F);
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
      this.module.barRadius.setCurrent(2.0F);
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

   private void renderImportDialog(CustomDrawContext draw, float mouseX, float mouseY) {
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));
      float x = (this.width - IMPORT_W) / 2.0F;
      float y = (this.height - IMPORT_H) / 2.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x - 6.0F, y - 6.0F, IMPORT_W + 12.0F, IMPORT_H + 12.0F, BorderRadius.all(10.0F), new ColorRGBA(0, 0, 0, 170));
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, IMPORT_W, IMPORT_H, BorderRadius.all(8.0F), new ColorRGBA(19, 19, 22).withAlpha(255));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, IMPORT_W, IMPORT_H, 1.0F, BorderRadius.all(8.0F), this.border());
      draw.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Импорт изображения (PNG)", x + 12.0F, y + 10.0F, TEXT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 12.0F, y + 24.0F, IMPORT_W - 24.0F, 1.0F, BorderRadius.all(0.5F), this.accent().withAlpha(90));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), this.importDir == null ? "" : this.importDir.toString(), x + 12.0F, y + 29.0F, DIM);

      float listY = y + 42.0F;
      float listH = IMPORT_H - 42.0F - 34.0F;
      List<Path> entries = this.importEntries();
      float contentH = entries.size() * 16.0F + 16.0F;
      this.importScroll = MathHelper.clamp(this.importScroll, 0.0F, Math.max(contentH - listH, 0.0F));
      draw.enableScissor((int) x, (int) listY, (int) (x + IMPORT_W), (int) (listY + listH));
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
      this.renderScrollbar(draw, x + IMPORT_W - 7.0F, listY, listH, this.importScroll, contentH, listH);

      float buttonY = y + IMPORT_H - 28.0F;
      boolean cancelHovered = MathUtil.isHovered(mouseX, mouseY, x + IMPORT_W - 88.0F, buttonY, 80.0F, 18.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + IMPORT_W - 88.0F, buttonY, 80.0F, 18.0F, BorderRadius.all(4.0F), cancelHovered ? new ColorRGBA(64, 64, 72).withAlpha(255) : new ColorRGBA(42, 42, 48).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Отмена", x + IMPORT_W - 48.0F - Fonts.REGULAR.getWidth("Отмена", 6.0F) / 2.0F, buttonY + 6.0F, TEXT);
   }

   private void renderImportRow(CustomDrawContext draw, float mouseX, float x, float rowY, String label, boolean dir) {
      boolean hovered = MathUtil.isHovered(mouseX, rowY, x + 4.0F, rowY, IMPORT_W - 8.0F, 15.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 4.0F, rowY, IMPORT_W - 8.0F, 15.0F, BorderRadius.all(3.0F), hovered ? this.accent().withAlpha(90) : new ColorRGBA(30, 30, 34).withAlpha(150));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), label, x + 12.0F, rowY + 4.0F, dir ? new ColorRGBA(160, 200, 255) : TEXT);
   }

   private void handleImportClick(double mouseX, double mouseY) {
      float x = (this.width - IMPORT_W) / 2.0F;
      float y = (this.height - IMPORT_H) / 2.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + IMPORT_W - 88.0F, y + IMPORT_H - 28.0F, 80.0F, 18.0F)) {
         this.importOpen = false;
         return;
      }
      float listY = y + 42.0F;
      float listH = IMPORT_H - 42.0F - 34.0F;
      if (mouseX >= x && mouseX <= x + IMPORT_W && mouseY >= listY && mouseY <= listY + listH) {
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

   private static final class Row {
      final String kind;
      final int index;
      final float y;
      final float h;

      Row(String kind, int index, float y, float h) {
         this.kind = kind;
         this.index = index;
         this.y = y;
         this.h = h;
      }
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
}