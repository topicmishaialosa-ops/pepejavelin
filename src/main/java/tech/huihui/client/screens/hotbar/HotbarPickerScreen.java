package tech.huihui.client.screens.hotbar;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.impl.render.CustomHotbar;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class HotbarPickerScreen extends Screen implements IMinecraft {
   private static final float MARGIN = 12.0F;
   private static final float CARD_W = 132.0F;
   private static final float CARD_H = 78.0F;
   private static final float CARD_GAP = 8.0F;
   private static final float TAB_W = 74.0F;
   private static final float TAB_H = 20.0F;

   private enum Tab {
      HOTBAR,
      STATUS
   }

   private final Map<Integer, Float> hover = new HashMap<>();
   private float listScroll;
   private int appliedIndex;
   private int appliedStatusIndex;
   private Tab tab = Tab.HOTBAR;

   public HotbarPickerScreen() {
      super(Text.literal("Выбор стиля хотбара"));
      this.appliedIndex = CustomHotbar.INSTANCE.currentStyleIndex();
      this.appliedStatusIndex = CustomHotbar.INSTANCE.currentStatusStyleIndex();
   }

   public static void open() {
      if (mc.currentScreen instanceof HotbarPickerScreen) {
         return;
      }
      mc.setScreen(new HotbarPickerScreen());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return true;
   }

   private Font font(float size) {
      return Fonts.REGULAR.getFont(size);
   }

   private ColorRGBA accent() {
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      return theme != null ? theme.getColor() : new ColorRGBA(96, 130, 255);
   }

   private float gridY() {
      return MARGIN + 34.0F;
   }

   private float gridBottom() {
      return this.height - MARGIN;
   }

   private int columns() {
      return Math.max(1, (int) ((this.width - MARGIN * 2.0F) / (CARD_W + CARD_GAP)));
   }

   private int rows() {
      return MathHelper.ceil((float) this.count() / (float) this.columns());
   }

   private int count() {
      return this.tab == Tab.HOTBAR ? CustomHotbar.styleCount() : CustomHotbar.statusCount();
   }

   private String[] names() {
      return this.tab == Tab.HOTBAR ? CustomHotbar.getStyles() : CustomHotbar.getStatusStyles();
   }

   private int applied() {
      return this.tab == Tab.HOTBAR ? this.appliedIndex : this.appliedStatusIndex;
   }

   private float totalHeight() {
      return (float) this.rows() * (CARD_H + CARD_GAP);
   }

   private float maxScroll() {
      return Math.max(0.0F, this.totalHeight() - (this.gridBottom() - this.gridY()));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      draw.fill(0, 0, this.width, this.height, 0x99000000);

      int count = this.count();
      for (int i = 0; i < count; i++) {
         float value = this.hover.getOrDefault(i, 0.0F);
         this.hover.put(i, value + ((this.isHovered(i, mouseX, mouseY) ? 1.0F : 0.0F) - value) * 0.15F);
      }

      String title = this.tab == Tab.HOTBAR ? "Выбор стиля хотбара" : "Выбор сердец и голода";
      draw.drawText(Fonts.BOLD.getFont(10.0F), title, MARGIN, MARGIN, new ColorRGBA(224, 226, 232));
      draw.drawText(font(5.0F), "ЛКМ — применить стиль, ESC — закрыть", MARGIN, MARGIN + 12.0F, new ColorRGBA(150, 154, 164));

      this.renderTabs(draw, mouseX, mouseY);
      this.renderGrid(draw, mouseX, mouseY, count);
   }

   private void renderTabs(CustomDrawContext draw, int mouseX, int mouseY) {
      float py = MARGIN;
      ColorRGBA theme = this.accent();
      this.renderPill(draw, this.width - MARGIN - TAB_W * 2.0F - 4.0F, py, TAB_W, TAB_H, "Хотбар", this.tab == Tab.HOTBAR, theme, mouseX, mouseY);
      this.renderPill(draw, this.width - MARGIN - TAB_W, py, TAB_W, TAB_H, "Статы", this.tab == Tab.STATUS, theme, mouseX, mouseY);
   }

   private void renderPill(CustomDrawContext draw, float x, float y, float w, float h, String label, boolean active, ColorRGBA theme, int mouseX, int mouseY) {
      boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
      ColorRGBA bg = active ? theme.withAlpha(160) : new ColorRGBA(24, 27, 34, 200);
      if (hovered && !active) {
         bg = bg.brighter(0.2F);
      }
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(5.0F), bg);
      if (active) {
         DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(5.0F), theme);
      }
      float tw = Fonts.BOLD.getWidth(label, 6.0F);
      draw.drawText(Fonts.BOLD.getFont(6.0F), label, x + w / 2.0F - tw / 2.0F, y + h / 2.0F - 4.0F, active ? ColorRGBA.WHITE : new ColorRGBA(150, 154, 164));
   }

   private void renderGrid(CustomDrawContext draw, int mouseX, int mouseY, int count) {
      float gridX = MARGIN;
      float gridY = this.gridY();
      float bottom = this.gridBottom();
      int cols = this.columns();
      String[] names = this.names();
      int applied = this.applied();

      DrawUtil.drawRoundedRect(draw.getMatrices(), gridX - 4.0F, gridY - 4.0F, this.width - MARGIN * 2.0F + 8.0F, bottom - gridY + 8.0F, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 180));

      for (int i = 0; i < count; i++) {
         int col = i % cols;
         int row = i / cols;
         float x = gridX + col * (CARD_W + CARD_GAP);
         float y = gridY + row * (CARD_H + CARD_GAP) - this.listScroll;

         if (y + CARD_H < gridY || y > bottom) {
            continue;
         }

         boolean isApplied = i == applied;
         float hp = this.hover.getOrDefault(i, 0.0F);
         ColorRGBA themeColor = this.accent();

         int base = 22 + (int) (hp * 6.0F);
         ColorRGBA bg = isApplied ? new ColorRGBA(Math.min(255, base + themeColor.getRed() / 6), Math.min(255, base + themeColor.getGreen() / 6), Math.min(255, base + themeColor.getBlue() / 6), 235) : new ColorRGBA(base, base + 2, base + 6, 235);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, CARD_W, CARD_H, BorderRadius.all(5.0F), bg);

         if (this.tab == Tab.HOTBAR) {
            this.drawMini(draw, i, x, y + 10.0F, CARD_W);
         } else {
            this.drawMiniStatus(draw, i, x, y + 10.0F, CARD_W);
         }

         draw.drawText(font(6.0F), names[i], x + 8.0F, y + CARD_H - 15.0F, new ColorRGBA(230, 232, 238));
         if (isApplied) {
            draw.drawText(font(4.5F), "Текущий", x + CARD_W - 8.0F - Fonts.REGULAR.getWidth("Текущий", 4.5F), y + CARD_H - 15.0F, themeColor);
         }

         if (hp > 0.0F) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, y + CARD_H - 2.0F, CARD_W * hp, 2.0F, BorderRadius.ZERO, themeColor.withAlpha(120));
         }
      }

      if (this.maxScroll() > 0.0F) {
         float viewH = this.gridBottom() - this.gridY();
         float thumbH = Math.max(24.0F, viewH * viewH / this.totalHeight());
         float thumbY = gridY + (this.gridBottom() - gridY - thumbH) * (this.listScroll / this.maxScroll());
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.width - MARGIN - 3.0F, gridY, 2.0F, viewH, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 18));
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.width - MARGIN - 3.0F, thumbY, 2.0F, thumbH, BorderRadius.all(1.0F), this.accent().withAlpha(200));
      }
   }

   private void drawMini(CustomDrawContext draw, int index, float x, float y, float w) {
      float slot = w / 11.0F;
      float gap = slot * 0.2F;
      float pad = slot * 0.15F;
      float pw = 9.0F * slot + 8.0F * gap + 2.0F * pad;
      float ph = slot + 2.0F * pad;
      float px = x + (w - pw) / 2.0F;
      float py = y + 2.0F;
      float scale = slot / 22.0F;
      float pr = CustomHotbar.panelRadiusOf(index) * scale;
      float sr = CustomHotbar.slotRadiusOf(index) * scale;

      ColorRGBA[] palette = PALETTES[index];
      ColorRGBA bg = palette[0];
      ColorRGBA bd = palette[1];
      ColorRGBA ac = palette[2];

      if (CustomHotbar.offhandLeftOf(index)) {
         px += slot + gap;
      }

      boolean noPanel = index == 2 || index == 6 || index == 11 || index == 15;
      if (!noPanel) {
         BorderRadius radius = BorderRadius.all(pr);
         if (index == 4) {
            float time = (System.currentTimeMillis() % 4000L) / 4000.0F;
            ColorRGBA c1 = ColorRGBA.fromHSB(time, 0.8F, 0.35F);
            ColorRGBA c2 = ColorRGBA.fromHSB(time + 0.25F, 0.8F, 0.35F);
            ColorRGBA c3 = ColorRGBA.fromHSB(time + 0.5F, 0.8F, 0.35F);
            ColorRGBA c4 = ColorRGBA.fromHSB(time + 0.75F, 0.8F, 0.35F);
            DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, pw, ph, radius, c1, c2, c3, c4);
         } else if (index == 8) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, pw, ph, radius, new ColorRGBA(110, 112, 118, 255), new ColorRGBA(52, 54, 60, 255), new ColorRGBA(52, 54, 60, 255), new ColorRGBA(24, 25, 30, 255));
         } else if (index == 19) {
            ColorRGBA a = new ColorRGBA(80, 30, 130, 255);
            ColorRGBA b = new ColorRGBA(180, 60, 220, 255);
            DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, pw, ph, radius, a, b, b, a);
         } else {
            DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, pw, ph, radius, bg);
         }
         DrawUtil.drawRoundedBorder(draw.getMatrices(), px, py, pw, ph, 0.6F, radius, bd);
      }

      for (int i = 0; i < 9; i++) {
         float sx = px + pad + (float) i * (slot + gap);
         float sy = py + pad;
         boolean selected = i == 4;
         BorderRadius round = BorderRadius.all(sr);
         ColorRGBA sbg = index == 15 ? ColorRGBA.fromHSB((float) i / 9.0F, 0.45F, 0.3F) : (selected ? ac.withAlpha(150) : new ColorRGBA(0, 0, 0, 90));
         DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy, slot, slot, round, sbg);
         if (index == 2) {
            if (selected) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy + slot + 1.0F, slot, 1.5F, BorderRadius.all(1.0F), ac);
            }
         } else {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), sx, sy, slot, slot, selected ? 1.0F : 0.5F, round, selected ? ac : bd);
         }
      }
   }

   private void drawMiniStatus(CustomDrawContext draw, int index, float x, float y, float w) {
      float cell = w / 11.0F;
      float gap = cell * 0.2F;
      float pad = cell * 0.15F;
      float pw = 10.0F * cell + 9.0F * gap + 2.0F * pad;
      float ph = 2.0F * cell + gap + 2.0F * pad;
      float px = x + (w - pw) / 2.0F;
      float py = y + 2.0F;
      ColorRGBA[] palette = STATUS_PALETTES[index];
      ColorRGBA empty = index == 7 ? new ColorRGBA(255, 255, 255, 20) : new ColorRGBA(0, 0, 0, 90);

      if (palette[0].getAlpha() > 0) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, pw, ph, BorderRadius.all(4.0F), palette[0]);
         DrawUtil.drawRoundedBorder(draw.getMatrices(), px, py, pw, ph, 0.6F, BorderRadius.all(4.0F), palette[1]);
      }

      for (int i = 0; i < 10; i++) {
         float cx = px + pad + (float) i * (cell + gap);
         DrawUtil.drawRoundedRect(draw.getMatrices(), cx, py + pad, cell, cell, BorderRadius.all(cell * 0.22F), i < 7 ? palette[3] : empty);
         DrawUtil.drawRoundedRect(draw.getMatrices(), cx, py + pad + cell + gap, cell, cell, BorderRadius.all(cell * 0.22F), i < 8 ? palette[4] : empty);
      }
   }

   private boolean isHovered(int index, int mouseX, int mouseY) {
      if (mouseY < this.gridY() || mouseY > this.gridBottom()) {
         return false;
      }
      int cols = this.columns();
      float x = MARGIN + (index % cols) * (CARD_W + CARD_GAP);
      float y = this.gridY() + (index / cols) * (CARD_H + CARD_GAP) - this.listScroll;
      return mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H;
   }

   private int indexAt(int mouseX, int mouseY) {
      for (int i = 0; i < this.count(); i++) {
         if (this.isHovered(i, mouseX, mouseY)) {
            return i;
         }
      }
      return -1;
   }

   private boolean tabAt(int mouseX, int mouseY) {
      float py = MARGIN;
      boolean inY = mouseY >= py && mouseY <= py + TAB_H;
      if (inY && mouseX >= this.width - MARGIN - TAB_W * 2.0F - 4.0F && mouseX <= this.width - MARGIN - TAB_W - 4.0F + TAB_W) {
         this.tab = Tab.HOTBAR;
         return true;
      }
      if (inY && mouseX >= this.width - MARGIN - TAB_W && mouseX <= this.width - MARGIN) {
         this.tab = Tab.STATUS;
         return true;
      }
      return false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.tabAt((int) mouseX, (int) mouseY)) {
         return true;
      }
      int index = this.indexAt((int) mouseX, (int) mouseY);
      if (index >= 0) {
         if (this.tab == Tab.HOTBAR) {
            CustomHotbar.INSTANCE.style.set(CustomHotbar.getStyles()[index]);
            this.appliedIndex = index;
         } else {
            CustomHotbar.INSTANCE.statusStyle.set(CustomHotbar.getStatusStyles()[index]);
            this.appliedStatusIndex = index;
         }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (mouseX >= MARGIN - 4.0F && mouseX <= this.width - MARGIN + 4.0F && mouseY >= this.gridY() - 4.0F) {
         this.listScroll = MathHelper.clamp(this.listScroll - (float) verticalAmount * 20.0F, 0.0F, this.maxScroll());
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   private static final ColorRGBA[][] PALETTES = new ColorRGBA[][]{
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(100, 100, 115), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(25, 25, 25), new ColorRGBA(70, 70, 70), new ColorRGBA(0, 255, 0)},
      {new ColorRGBA(0, 0, 0, 0), new ColorRGBA(90, 90, 100), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(30, 32, 40), new ColorRGBA(90, 95, 110), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(30, 30, 30), new ColorRGBA(255, 255, 255), new ColorRGBA(255, 80, 80)},
      {new ColorRGBA(255, 255, 255, 30), new ColorRGBA(255, 255, 255), new ColorRGBA(255, 255, 255)},
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(110, 115, 130), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(12, 14, 20), new ColorRGBA(80, 90, 255), new ColorRGBA(0, 255, 255)},
      {new ColorRGBA(52, 54, 60), new ColorRGBA(160, 165, 175), new ColorRGBA(255, 209, 0)},
      {new ColorRGBA(20, 24, 60), new ColorRGBA(100, 120, 255), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(0, 0, 0, 170), new ColorRGBA(110, 115, 130), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(0, 0, 0, 170), new ColorRGBA(140, 145, 160), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(100, 100, 115), new ColorRGBA(255, 90, 90)},
      {new ColorRGBA(16, 18, 24), new ColorRGBA(90, 95, 110), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(110, 115, 130), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(20, 20, 30), new ColorRGBA(0, 0, 0), new ColorRGBA(255, 255, 255)},
      {new ColorRGBA(12, 14, 18, 120), new ColorRGBA(110, 115, 130), new ColorRGBA(58, 152, 255)},
      {new ColorRGBA(18, 20, 26), new ColorRGBA(58, 152, 255), new ColorRGBA(255, 200, 60)},
      {new ColorRGBA(0, 0, 0, 150), new ColorRGBA(110, 115, 130), new ColorRGBA(0, 255, 170)},
      {new ColorRGBA(40, 20, 60), new ColorRGBA(170, 80, 255), new ColorRGBA(255, 80, 255)}
   };
   private static final ColorRGBA[][] STATUS_PALETTES = new ColorRGBA[][]{
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(100, 100, 115), new ColorRGBA(58, 152, 255), new ColorRGBA(225, 55, 65), new ColorRGBA(235, 185, 70)},
      {new ColorRGBA(0, 0, 0, 160), new ColorRGBA(100, 100, 115), new ColorRGBA(58, 152, 255), new ColorRGBA(225, 55, 65), new ColorRGBA(235, 185, 70)},
      {new ColorRGBA(25, 25, 25), new ColorRGBA(70, 70, 70), new ColorRGBA(58, 152, 255), new ColorRGBA(215, 45, 45), new ColorRGBA(240, 180, 60)},
      {new ColorRGBA(12, 14, 20), new ColorRGBA(0, 230, 255), new ColorRGBA(0, 255, 255), new ColorRGBA(0, 230, 255), new ColorRGBA(255, 90, 220)},
      {new ColorRGBA(52, 54, 60), new ColorRGBA(160, 165, 175), new ColorRGBA(255, 209, 0), new ColorRGBA(230, 70, 60), new ColorRGBA(255, 200, 80)},
      {new ColorRGBA(20, 24, 60), new ColorRGBA(100, 120, 255), new ColorRGBA(58, 152, 255), new ColorRGBA(58, 152, 255), new ColorRGBA(255, 170, 60)},
      {new ColorRGBA(30, 30, 30), new ColorRGBA(255, 255, 255), new ColorRGBA(255, 80, 80), new ColorRGBA(255, 80, 255), new ColorRGBA(80, 255, 190)},
      {new ColorRGBA(255, 255, 255, 30), new ColorRGBA(255, 255, 255), new ColorRGBA(255, 255, 255), new ColorRGBA(255, 255, 255, 200), new ColorRGBA(255, 255, 255, 170)},
      {new ColorRGBA(0, 0, 0, 0), new ColorRGBA(90, 90, 100), new ColorRGBA(58, 152, 255), new ColorRGBA(225, 55, 65), new ColorRGBA(235, 185, 70)},
      {new ColorRGBA(0, 0, 0, 0), new ColorRGBA(90, 90, 100), new ColorRGBA(58, 152, 255), new ColorRGBA(225, 55, 65), new ColorRGBA(235, 185, 70)}
   };
}
