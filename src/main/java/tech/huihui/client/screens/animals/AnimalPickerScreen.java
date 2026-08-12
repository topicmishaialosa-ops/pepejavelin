package tech.huihui.client.screens.animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.impl.cosmetics.AnimalsModule;
import tech.huihui.client.modules.impl.cosmetics.Companion;
import tech.huihui.client.modules.impl.cosmetics.CompanionRegistry;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class AnimalPickerScreen extends Screen implements IMinecraft {
   private static final float MARGIN = 12.0F;
   private static final float SEARCH_H = 22.0F;
   private static final float CARD_W = 150.0F;
   private static final float CARD_H = 40.0F;
   private static final float CARD_GAP = 8.0F;

   private final List<Companion> filtered = new ArrayList<>();
   private final Map<Companion, Float> hover = new HashMap<>();
   private String searchBuffer = "";
   private boolean searchFocused;
   private float listScroll;

   public AnimalPickerScreen() {
      super(Text.literal("Выбор животных"));
      this.filtered.addAll(CompanionRegistry.ALL);
   }

   public static void open() {
      if (mc.currentScreen instanceof AnimalPickerScreen) {
         return;
      }
      mc.setScreen(new AnimalPickerScreen());
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

   private float textWidth(String text, float size) {
      return Fonts.REGULAR.getWidth(text, size);
   }

   private ColorRGBA accent() {
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      return theme != null ? theme.getColor() : new ColorRGBA(96, 130, 255);
   }

   private float gridY() {
      return MARGIN + 24.0F + SEARCH_H + 8.0F;
   }

   private float gridBottom() {
      return this.height - MARGIN;
   }

   private int columns() {
      return Math.max(1, (int) ((this.width - MARGIN * 2.0F) / (CARD_W + CARD_GAP)));
   }

   private float totalHeight() {
      return MathHelper.ceil((float) this.filtered.size() / (float) this.columns()) * (CARD_H + CARD_GAP);
   }

   private float maxScroll() {
      return Math.max(0.0F, this.totalHeight() - (this.gridBottom() - this.gridY()));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      CustomDrawContext draw = CustomDrawContext.of(context);
      draw.fill(0, 0, this.width, this.height, 0x99000000);

      for (Companion companion : this.filtered) {
         float value = this.hover.getOrDefault(companion, 0.0F);
         this.hover.put(companion, value + ((this.isHovered(companion, mouseX, mouseY) ? 1.0F : 0.0F) - value) * 0.15F);
      }

      draw.drawText(Fonts.BOLD.getFont(10.0F), "Выбор животных", MARGIN, MARGIN, new ColorRGBA(224, 226, 232));
      draw.drawText(font(5.0F), "ЛКМ — выбрать/убрать компаньона", MARGIN, MARGIN + 12.0F, new ColorRGBA(150, 154, 164));

      this.renderSearch(draw, mouseX, mouseY);
      this.renderGrid(draw, mouseX, mouseY);
   }

   private void renderSearch(CustomDrawContext draw, int mouseX, int mouseY) {
      float x = MARGIN;
      float y = MARGIN + 24.0F;
      float w = Math.min(260.0F, this.width - MARGIN * 2.0F);

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, SEARCH_H, BorderRadius.all(5.0F), new ColorRGBA(18, 20, 26, 235));
      draw.drawText(Fonts.LUPA.getFont(5.0F), "\uf002", x + 6.0F, y + 7.5F, new ColorRGBA(140, 144, 154));

      if (this.searchBuffer.isEmpty()) {
         draw.drawText(font(6.0F), "Поиск животного...", x + 15.0F, y + 7.0F, new ColorRGBA(120, 124, 134));
      } else {
         draw.drawText(font(6.0F), this.searchBuffer, x + 15.0F, y + 7.0F, new ColorRGBA(230, 232, 238));
      }

      int count = this.filtered.size();
      draw.drawText(font(5.0F), "Выбрано: " + AnimalsModule.INSTANCE.getSelectedIds().size() + " из " + CompanionRegistry.ALL.size() + " (" + count + ")", x + w + 10.0F, y + 8.0F, new ColorRGBA(150, 154, 164));
   }

   private void renderGrid(CustomDrawContext draw, int mouseX, int mouseY) {
      float gridX = MARGIN;
      float gridY = this.gridY();
      float bottom = this.gridBottom();
      int cols = this.columns();

      DrawUtil.drawRoundedRect(draw.getMatrices(), gridX - 4.0F, gridY - 4.0F, this.width - MARGIN * 2.0F + 8.0F, bottom - gridY + 8.0F, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 180));

      int index = 0;
      for (Companion companion : this.filtered) {
         int col = index % cols;
         int row = index / cols;
         float x = gridX + col * (CARD_W + CARD_GAP);
         float y = gridY + row * (CARD_H + CARD_GAP) - this.listScroll;
         index++;

         if (y + CARD_H < gridY || y > bottom) {
            continue;
         }

         boolean selected = AnimalsModule.INSTANCE.isSelected(companion.id);
         float hp = this.hover.getOrDefault(companion, 0.0F);
         ColorRGBA themeColor = this.accent();

         int base = 22 + (int) (hp * 6.0F);
         ColorRGBA bg = selected ? new ColorRGBA(Math.min(255, base + themeColor.getRed() / 6), Math.min(255, base + themeColor.getGreen() / 6), Math.min(255, base + themeColor.getBlue() / 6), 235) : new ColorRGBA(base, base + 2, base + 6, 235);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, CARD_W, CARD_H, BorderRadius.all(5.0F), bg);

         DrawUtil.drawRoundedRect(draw.getMatrices(), x + 6.0F, y + 8.0F, 24.0F, 24.0F, BorderRadius.all(4.0F), new ColorRGBA(companion.accentColor));

         float nameX = x + 36.0F;
         draw.drawText(font(6.5F), companion.displayName, nameX, y + 6.0F, new ColorRGBA(230, 232, 238));

         if (selected) {
            draw.drawText(font(5.0F), "Выбрано", nameX, y + 18.0F, themeColor);
         } else {
            draw.drawText(font(5.0F), "Нажми, чтобы добавить", nameX, y + 18.0F, new ColorRGBA(140, 144, 154));
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

   private boolean isHovered(Companion companion, int mouseX, int mouseY) {
      if (mouseY < this.gridY() || mouseY > this.gridBottom()) {
         return false;
      }
      int cols = this.columns();
      int index = this.filtered.indexOf(companion);
      if (index < 0) {
         return false;
      }
      float x = MARGIN + (index % cols) * (CARD_W + CARD_GAP);
      float y = this.gridY() + (index / cols) * (CARD_H + CARD_GAP) - this.listScroll;
      return mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H;
   }

   private Companion companionAt(int mouseX, int mouseY) {
      for (Companion companion : this.filtered) {
         if (this.isHovered(companion, mouseX, mouseY)) {
            return companion;
         }
      }
      return null;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float sx = MARGIN;
      float sy = MARGIN + 24.0F;
      float sw = Math.min(260.0F, this.width - MARGIN * 2.0F);
      if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + SEARCH_H) {
         this.searchFocused = true;
         return true;
      }
      this.searchFocused = false;

      Companion companion = this.companionAt((int) mouseX, (int) mouseY);
      if (companion != null) {
         AnimalsModule.INSTANCE.setSelected(companion.id, !AnimalsModule.INSTANCE.isSelected(companion.id));
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

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_BACKSPACE && this.searchFocused && !this.searchBuffer.isEmpty()) {
         this.searchBuffer = this.searchBuffer.substring(0, this.searchBuffer.length() - 1);
         this.refilter();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.searchFocused && Character.isDefined(chr) && !Character.isISOControl(chr)) {
         this.searchBuffer += chr;
         this.refilter();
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   private void refilter() {
      this.listScroll = 0.0F;
      this.filtered.clear();
      String query = this.searchBuffer.trim().toLowerCase(Locale.ROOT);
      for (Companion companion : CompanionRegistry.ALL) {
         if (query.isEmpty() || companion.displayName.toLowerCase(Locale.ROOT).contains(query) || companion.id.toLowerCase(Locale.ROOT).contains(query)) {
            this.filtered.add(companion);
         }
      }
   }
}
