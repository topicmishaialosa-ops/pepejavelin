package tech.huihui.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import tech.huihui.base.autobuy.item.CollectorItemBuy;
import tech.huihui.base.autobuy.item.ItemBuy;
import tech.huihui.base.config.AutoBuyConfig;
import tech.huihui.client.modules.impl.misc.AutoBuy;
import tech.huihui.client.modules.impl.misc.Collector;

public final class AutoBuyEditorScreen extends Screen {
   private static final int PANEL_WIDTH = 460;
   private final int topic;
   private final List<ItemBuy> autoBuyItems;
   private final List<CollectorItemBuy> collectorItems;
   private final List<ItemBuy> filteredItems = new ArrayList<>();
   private final StringBuilder priceBuffer = new StringBuilder();
   private int scroll;
   private int selected = -1;
   private boolean editingPrice;
   private String searchText = "";

   public AutoBuyEditorScreen(int topic) {
      super(Text.literal("Редактор автозакупки"));
      this.topic = topic;
      this.autoBuyItems = AutoBuy.INSTANCE.getEntries();
      this.collectorItems = Collector.INSTANCE.getEntries();
      this.refresh();
   }

   private void refresh() {
      this.filteredItems.clear();
      String query = this.searchText.toLowerCase(Locale.ROOT);
      if (this.topic == 0) {
         for (ItemBuy item : this.autoBuyItems) {
            if (query.isEmpty() || item.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) {
               this.filteredItems.add(item);
            }
         }
      } else {
         for (CollectorItemBuy item : this.collectorItems) {
            if (query.isEmpty() || item.getDisplayName().toLowerCase(Locale.ROOT).contains(query)) {
               this.filteredItems.add(item);
            }
         }
      }

      int visible = this.visibleCount();
      if (this.scroll > Math.max(0, this.filteredItems.size() - visible)) {
         this.scroll = Math.max(0, this.filteredItems.size() - visible);
      }

      if (this.selected >= this.filteredItems.size()) {
         this.selected = -1;
      }
   }

   private int visibleCount() {
      return Math.max(0, (this.height - 80 - 30) / 34);
   }

   private int panelX() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private int rowY(int index) {
      return 80 + (index - this.scroll) * 34;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      int panelX = this.panelX();
      context.fill(panelX - 8, 20, panelX + PANEL_WIDTH + 8, this.height - 10, -1610612736);
      context.drawText(this.textRenderer, this.topic == 0 ? "Редактор автозакупки" : "Редактор коллектора", panelX + 8, 30, -1, false);
      this.drawTabs(context, panelX, mouseX, mouseY);
      this.drawSearchBox(context, panelX, mouseX, mouseY);
      this.drawSaveLoad(context, panelX, mouseX, mouseY);
      this.drawItemList(context, panelX, mouseX, mouseY);
   }

   private void drawTabs(DrawContext context, int panelX, int mouseX, int mouseY) {
      for (int i = 0; i < 2; i++) {
         int x = panelX + 150 + i * 96;
         boolean hovered = isInside(mouseX, mouseY, x, 28, 90, 18);
         boolean active = this.topic == i;
         context.fill(x, 28, x + 90, 46, active ? 0xFF4C78A8 : hovered ? 0xFF303030 : 0xFF1E1E1E);
         context.drawCenteredTextWithShadow(this.textRenderer, i == 0 ? "Автозакупка" : "Коллектор", x + 45, 32, active ? -1 : 0xFF9E9E9E);
      }
   }

   private void drawSearchBox(DrawContext context, int panelX, int mouseX, int mouseY) {
      int x = panelX + 350;
      context.fill(x, 28, x + 100, 46, 0xFF1E1E1E);
      context.drawText(this.textRenderer, Text.literal("Поиск: " + this.searchText), x + 4, 32, 0xFF9E9E9E, false);
   }

   private void drawSaveLoad(DrawContext context, int panelX, int mouseX, int mouseY) {
      for (int i = 0; i < 2; i++) {
         int x = panelX + 8 + i * 66;
         boolean hovered = isInside(mouseX, mouseY, x, 52, 60, 18);
         context.fill(x, 52, x + 60, 70, hovered ? 0xFF3A3A3A : 0xFF252525);
         context.drawCenteredTextWithShadow(this.textRenderer, i == 0 ? "Сохранить" : "Загрузить", x + 30, 56, -1);
      }
   }

   private void drawItemList(DrawContext context, int panelX, int mouseX, int mouseY) {
      for (int i = this.scroll; i < this.filteredItems.size(); i++) {
         int y = this.rowY(i);
         if (y < 80 || y + 34 > this.height - 10) {
            continue;
         }

         ItemBuy item = this.filteredItems.get(i);
         boolean selectedRow = i == this.selected;
         boolean hovered = isInside(mouseX, mouseY, panelX + 8, y, PANEL_WIDTH - 16, 32);
         context.fill(panelX + 8, y, panelX + PANEL_WIDTH - 8, y + 32, selectedRow ? 0xFF3D5C7C : hovered ? 0xFF2E2E2E : 0xFF1C1C1C);
         if (selectedRow || hovered) {
            context.fill(panelX + 8, y, panelX + 11, y + 32, 0xFF4C78A8);
         }

         context.drawItem(item.getItemStack(), panelX + 14, y + 8);
         if (isInside(mouseX, mouseY, panelX + 14, y + 8, 16, 16)) {
            context.drawItemTooltip(this.textRenderer, item.getItemStack(), mouseX, mouseY);
         }

         context.drawText(this.textRenderer, item.getDisplayName(), panelX + 36, y + 11, -1, false);
         boolean enabled = this.topic == 0 ? item.isEnabled() : ((CollectorItemBuy) item).isActive();
         context.drawText(this.textRenderer, Text.literal(enabled ? "§a§lВКЛ" : "§c§lВЫКЛ"), panelX + PANEL_WIDTH - 90, y + 11, -1, false);
         int value = this.topic == 0 ? item.getPrice() : ((CollectorItemBuy) item).getCount();
         String valueText = this.topic == 0 ? value + " $" : "x" + value;
         boolean editing = this.editingPrice && i == this.selected;
         context.drawText(this.textRenderer, Text.literal(editing ? "▌" + this.priceBuffer : valueText), panelX + PANEL_WIDTH - 200, y + 11, editing ? 0xFFFFAA00 : 0xFFDDDDDD, false);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int panelX = this.panelX();
      this.commitPrice();
      for (int i = 0; i < 2; i++) {
         int x = panelX + 150 + i * 96;
         if (isInside((int) mouseX, (int) mouseY, x, 28, 90, 18)) {
            this.client.setScreen(new AutoBuyEditorScreen(i));
            return true;
         }
      }

      for (int i = 0; i < 2; i++) {
         int x = panelX + 8 + i * 66;
         if (isInside((int) mouseX, (int) mouseY, x, 52, 60, 18)) {
            if (i == 0) {
               this.save();
            } else {
               this.load();
            }

            return true;
         }
      }

      for (int i = this.scroll; i < this.filteredItems.size(); i++) {
         int y = this.rowY(i);
         if (y < 80 || y + 34 > this.height - 10) {
            continue;
         }

         if (!isInside((int) mouseX, (int) mouseY, panelX + 8, y, PANEL_WIDTH - 16, 32)) {
            continue;
         }

         this.selected = i;
         if (isInside((int) mouseX, (int) mouseY, panelX + PANEL_WIDTH - 200, y, 100, 32)) {
            this.editingPrice = true;
            this.priceBuffer.setLength(0);
            this.priceBuffer.append(this.topic == 0
               ? String.valueOf(this.filteredItems.get(i).getPrice())
               : String.valueOf(((CollectorItemBuy) this.filteredItems.get(i)).getCount()));
            return true;
         }

         if (isInside((int) mouseX, (int) mouseY, panelX + PANEL_WIDTH - 90, y, 82, 32)) {
            ItemBuy item = this.filteredItems.get(i);
            if (this.topic == 0) {
               item.setEnabled(!item.isEnabled());
            } else {
               ((CollectorItemBuy) item).setActive(!((CollectorItemBuy) item).isActive());
            }

            this.save();
            return true;
         }

         this.editingPrice = false;
         return true;
      }

      this.editingPrice = false;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.editingPrice && Character.isDigit(chr) && this.priceBuffer.length() < 9) {
         this.priceBuffer.append(chr);
         return true;
      }

      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.editingPrice) {
         if (keyCode == 257 || keyCode == 335) {
            this.commitPrice();
            return true;
         }

         if (keyCode == 259 && this.priceBuffer.length() > 0) {
            this.priceBuffer.deleteCharAt(this.priceBuffer.length() - 1);
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scroll = (int) Math.max(0, Math.min(this.filteredItems.size() - this.visibleCount(), this.scroll - Math.round(verticalAmount)));
      return true;
   }

   private void commitPrice() {
      if (!this.editingPrice || this.selected < 0 || this.selected >= this.filteredItems.size()) {
         this.editingPrice = false;
         return;
      }

      try {
         int value = Math.max(0, Integer.parseInt(this.priceBuffer.toString()));
         ItemBuy item = this.filteredItems.get(this.selected);
         if (this.topic == 0) {
            item.setPrice(value);
         } else {
            ((CollectorItemBuy) item).setCount(value);
         }

         this.save();
      } catch (NumberFormatException ignored) {
      }

      this.editingPrice = false;
   }

   private void save() {
      if (this.topic == 0) {
         AutoBuyConfig.saveAutoBuy(this.autoBuyItems);
      } else {
         AutoBuyConfig.saveCollect(this.collectorItems);
      }
   }

   private void load() {
      if (this.topic == 0) {
         AutoBuyConfig.loadAutoBuy(this.autoBuyItems);
      } else {
         AutoBuyConfig.loadCollect(this.collectorItems);
      }
   }

   private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
