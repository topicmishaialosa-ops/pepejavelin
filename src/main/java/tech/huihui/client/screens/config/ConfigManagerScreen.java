package tech.huihui.client.screens.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.config.ConfigManager;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.os.OperatingSystem;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class ConfigManagerScreen extends Screen implements IMinecraft {
   private static final float MARGIN = 12.0F;
   private static final float ROW_H = 30.0F;
   private static final float ROW_GAP = 6.0F;
   private static final float ACTION_W = 170.0F;
   private static final float BTN_H = 26.0F;
   private static final float BTN_GAP = 6.0F;

   private final List<String> configs = new ArrayList<>();
   private final List<Float> hover = new ArrayList<>();
   private TextFieldWidget nameField;
   private float listScroll;
   private int selectedIndex = -1;
   private String notice;
   private long noticeUntil;
   private boolean dirty = true;

   public ConfigManagerScreen() {
      super(Text.literal("Управление конфигами"));
      this.refresh();
   }

   public static void open() {
      if (mc.currentScreen instanceof ConfigManagerScreen) {
         return;
      }
      mc.setScreen(new ConfigManagerScreen());
   }

   private void refresh() {
      ConfigManager manager = HuihuiClient.getInstance().getConfigManager();
      List<String> names = manager.configNames();
      this.configs.clear();
      this.hover.clear();
      for (String name : names) {
         String clean = name.endsWith(".huihui") ? name.substring(0, name.length() - 7) : name;
         this.configs.add(clean);
         this.hover.add(0.0F);
      }
      if (this.selectedIndex >= this.configs.size()) {
         this.selectedIndex = -1;
      }
      this.dirty = false;
   }

   @Override
   protected void init() {
      super.init();
      this.nameField = this.addDrawableChild(new TextFieldWidget(mc.textRenderer, 0, 0, 120, 20, Text.empty()));
      this.nameField.setMaxLength(32);
      this.nameField.setDrawsBackground(false);
      if (this.selectedIndex >= 0 && this.selectedIndex < this.configs.size()) {
         this.nameField.setText(this.configs.get(this.selectedIndex));
      }
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

   private float listX() {
      return MARGIN;
   }

   private float listW() {
      return this.width - MARGIN * 2.0F - ACTION_W - 10.0F;
   }

   private float listY() {
      return MARGIN + 34.0F;
   }

   private float listBottom() {
      return this.height - MARGIN;
   }

   private float totalHeight() {
      return (float) this.configs.size() * (ROW_H + ROW_GAP);
   }

   private float maxScroll() {
      return Math.max(0.0F, this.totalHeight() - (this.listBottom() - this.listY()));
   }

   private void notice(String text) {
      this.notice = text;
      this.noticeUntil = System.currentTimeMillis() + 2500L;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      if (this.dirty) {
         this.refresh();
      }
      CustomDrawContext draw = CustomDrawContext.of(context);
      draw.fill(0, 0, this.width, this.height, 0x99000000);

      if (this.hover.size() != this.configs.size()) {
         this.hover.clear();
         for (int i = 0; i < this.configs.size(); i++) {
            this.hover.add(0.0F);
         }
      }

      draw.drawText(Fonts.BOLD.getFont(10.0F), "Управление конфигами", MARGIN, MARGIN, new ColorRGBA(224, 226, 232));
      draw.drawText(font(5.0F), "ЛКМ — выбор конфига, ESC — закрыть", MARGIN, MARGIN + 12.0F, new ColorRGBA(150, 154, 164));

      this.renderList(draw, mouseX, mouseY);
      this.renderActions(draw, mouseX, mouseY, context, delta);

      if (this.notice != null && System.currentTimeMillis() < this.noticeUntil) {
         String text = this.notice;
         float tw = Fonts.REGULAR.getWidth(text, 5.0F);
         draw.drawText(font(5.0F), text, this.width / 2.0F - tw / 2.0F, this.height - MARGIN - 6.0F, new ColorRGBA(230, 232, 238));
      }
   }

   private void renderList(CustomDrawContext draw, int mouseX, int mouseY) {
      float x = this.listX();
      float y0 = this.listY();
      float bottom = this.listBottom();
      float w = this.listW();
      ColorRGBA theme = this.accent();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x - 4.0F, y0 - 4.0F, w + 8.0F, bottom - y0 + 8.0F, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 180));

      if (this.configs.isEmpty()) {
         draw.drawText(font(5.0F), "Конфигов пока нет — введи имя справа и нажми «Сохранить»", x + 8.0F, y0 + 8.0F, new ColorRGBA(150, 154, 164));
      }

      for (int i = 0; i < this.configs.size(); i++) {
         float y = y0 + (float) i * (ROW_H + ROW_GAP) - this.listScroll;
         if (y + ROW_H < y0 || y > bottom) {
            continue;
         }
         boolean selected = i == this.selectedIndex;
         float hp = this.hover.get(i) + ((this.isRowHovered(i, mouseX, mouseY) ? 1.0F : 0.0F) - this.hover.get(i)) * 0.15F;
         this.hover.set(i, hp);

         int base = 22 + (int) (hp * 6.0F);
         ColorRGBA bg = selected
            ? new ColorRGBA(Math.min(255, base + theme.getRed() / 6), Math.min(255, base + theme.getGreen() / 6), Math.min(255, base + theme.getBlue() / 6), 235)
            : new ColorRGBA(base, base + 2, base + 6, 235);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, ROW_H, BorderRadius.all(5.0F), bg);
         if (selected) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, ROW_H, 1.0F, BorderRadius.all(5.0F), theme);
         }
         draw.drawText(font(5.5F), this.configs.get(i), x + 10.0F, y + ROW_H / 2.0F - 4.0F, selected ? ColorRGBA.WHITE : new ColorRGBA(210, 212, 220));
         if (selected) {
            draw.drawText(font(4.5F), "Выбран", x + w - 8.0F - Fonts.REGULAR.getWidth("Выбран", 4.5F), y + ROW_H / 2.0F - 4.0F, theme);
         }
      }

      if (this.maxScroll() > 0.0F) {
         float viewH = bottom - y0;
         float thumbH = Math.max(24.0F, viewH * viewH / this.totalHeight());
         float thumbY = y0 + (bottom - y0 - thumbH) * (this.listScroll / this.maxScroll());
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + w + 2.0F, y0, 2.0F, viewH, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 18));
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + w + 2.0F, thumbY, 2.0F, thumbH, BorderRadius.all(1.0F), theme.withAlpha(200));
      }
   }

   private void renderActions(CustomDrawContext draw, int mouseX, int mouseY, DrawContext context, float delta) {
      float x = this.width - MARGIN - ACTION_W;
      float y = this.listY();
      float w = ACTION_W;
      ColorRGBA theme = this.accent();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y - 4.0F, w, this.listBottom() - y + 8.0F, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 180));

      draw.drawText(Fonts.BOLD.getFont(6.0F), "Имя конфига", x + 8.0F, y + 4.0F, new ColorRGBA(180, 184, 192));
      float fieldY = y + 18.0F;
      this.nameField.setX((int) (x + 8.0F));
      this.nameField.setY((int) fieldY);
      this.nameField.setWidth((int) (w - 16.0F));
      this.nameField.setHeight(20);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 8.0F, fieldY, this.nameField.getWidth(), 20.0F, BorderRadius.all(7.0F), new ColorRGBA(255, 255, 255).withAlpha(9));
      if (this.nameField.getText().isEmpty() && !this.nameField.isFocused()) {
         draw.drawText(font(4.5F), "Например: pvp, crystal, farm...", x + 14.0F, fieldY + 6.0F, new ColorRGBA(140, 144, 152));
      }
      this.nameField.render(context, mouseX, mouseY, delta);

      float by = y + 48.0F;
      by = this.renderButton(draw, x, by, w, "Сохранить", theme, mouseX, mouseY) + BTN_GAP;
      by = this.renderButton(draw, x, by, w, "Применить", theme, mouseX, mouseY) + BTN_GAP;
      by = this.renderButton(draw, x, by, w, "Открыть папку", theme, mouseX, mouseY) + BTN_GAP;
      this.renderButton(draw, x, by, w, "Удалить", new ColorRGBA(220, 70, 70), mouseX, mouseY);

      draw.drawText(font(4.0F), "Конфиги шифруются и хранятся в Huihui/configs", x + 8.0F, this.listBottom() - 8.0F, new ColorRGBA(120, 124, 132));
   }

   private float renderButton(CustomDrawContext draw, float x, float y, float w, String label, ColorRGBA accent, int mouseX, int mouseY) {
      boolean hovered = mouseX >= x + 8.0F && mouseX <= x + w - 8.0F && mouseY >= y && mouseY <= y + BTN_H;
      ColorRGBA bg = hovered ? accent.withAlpha(180) : accent.withAlpha(100);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 8.0F, y, w - 16.0F, BTN_H, BorderRadius.all(6.0F), bg);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x + 8.0F, y, w - 16.0F, BTN_H, 1.0F, BorderRadius.all(6.0F), accent.withAlpha(120));
      float tw = Fonts.BOLD.getWidth(label, 5.5F);
      draw.drawText(Fonts.BOLD.getFont(5.5F), label, x + w / 2.0F - tw / 2.0F, y + BTN_H / 2.0F - 4.0F, hovered ? ColorRGBA.WHITE : new ColorRGBA(220, 222, 228));
      return y + BTN_H;
   }

   private boolean isRowHovered(int index, int mouseX, int mouseY) {
      if (mouseY < this.listY() || mouseY > this.listBottom()) {
         return false;
      }
      float x = this.listX();
      float y = this.listY() + (float) index * (ROW_H + ROW_GAP) - this.listScroll;
      return mouseX >= x && mouseX <= x + this.listW() && mouseY >= y && mouseY <= y + ROW_H;
   }

   private int indexAt(int mouseX, int mouseY) {
      for (int i = 0; i < this.configs.size(); i++) {
         if (this.isRowHovered(i, mouseX, mouseY)) {
            return i;
         }
      }
      return -1;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int index = this.indexAt((int) mouseX, (int) mouseY);
         if (index >= 0) {
            this.selectedIndex = index;
            if (this.nameField != null) {
               this.nameField.setText(this.configs.get(index));
            }
            return true;
         }
         float x = this.width - MARGIN - ACTION_W;
         float y = this.listY();
         float w = ACTION_W;
         boolean inActions = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= this.listBottom();
         if (inActions) {
            float by = y + 48.0F;
            if (this.buttonAt(mouseX, mouseY, by)) {
               this.saveClicked();
               return true;
            }
            by += BTN_H + BTN_GAP;
            if (this.buttonAt(mouseX, mouseY, by)) {
               this.applyClicked();
               return true;
            }
            by += BTN_H + BTN_GAP;
            if (this.buttonAt(mouseX, mouseY, by)) {
               this.openFolderClicked();
               return true;
            }
            by += BTN_H + BTN_GAP;
            if (this.buttonAt(mouseX, mouseY, by)) {
               this.deleteClicked();
               return true;
            }
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private boolean buttonAt(double mouseX, double mouseY, float y) {
      float x = this.width - MARGIN - ACTION_W;
      return mouseX >= x + 8.0F && mouseX <= x + ACTION_W - 8.0F && mouseY >= y && mouseY <= y + BTN_H;
   }

   private void saveClicked() {
      String name = this.nameField != null ? this.nameField.getText().trim() : "";
      if (name.isEmpty()) {
         this.notice("Введи имя конфига");
         return;
      }
      boolean ok = HuihuiClient.getInstance().getConfigManager().saveConfig(name);
      this.notice(ok ? "Конфигурация «" + name + "» сохранена" : "Ошибка при сохранении");
      if (ok) {
         this.dirty = true;
         int idx = this.configs.indexOf(name);
         this.selectedIndex = idx >= 0 ? idx : this.configs.size();
      }
   }

   private void applyClicked() {
      if (this.selectedIndex < 0 || this.selectedIndex >= this.configs.size()) {
         this.notice("Сначала выбери конфиг");
         return;
      }
      String name = this.configs.get(this.selectedIndex);
      boolean ok = HuihuiClient.getInstance().getConfigManager().loadConfig(name);
      this.notice(ok ? "Конфигурация «" + name + "» применена" : "Ошибка при загрузке");
   }

   private void openFolderClicked() {
      boolean ok = OperatingSystem.openFolder(HuihuiClient.getInstance().getConfigManager().configDirectory);
      this.notice(ok ? "Открываю папку с конфигами..." : "Не удалось открыть папку");
   }

   private void deleteClicked() {
      if (this.selectedIndex < 0 || this.selectedIndex >= this.configs.size()) {
         this.notice("Сначала выбери конфиг");
         return;
      }
      String name = this.configs.get(this.selectedIndex);
      boolean ok = HuihuiClient.getInstance().getConfigManager().deleteConfig(name);
      this.notice(ok ? "Конфиг «" + name + "» удалён" : "Ошибка при удалении");
      if (ok) {
         this.selectedIndex = -1;
         this.dirty = true;
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.nameField != null && this.nameField.isFocused()) {
         if (keyCode == 257 || keyCode == 335) {
            this.saveClicked();
            return true;
         }
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (mouseX >= this.listX() - 4.0F && mouseX <= this.listX() + this.listW() + 8.0F && mouseY >= this.listY() - 4.0F) {
         this.listScroll = MathHelper.clamp(this.listScroll - (float) verticalAmount * 20.0F, 0.0F, this.maxScroll());
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
