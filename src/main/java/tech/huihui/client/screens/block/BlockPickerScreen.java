package tech.huihui.client.screens.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.setting.impl.BlockMapSetting;
import tech.huihui.client.modules.impl.render.BlockESP;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class BlockPickerScreen extends Screen implements IMinecraft {
   private static final float MARGIN = 10.0F;
   private static final float PANEL_W = 240.0F;
   private static final float CELL_SIZE = 64.0F;
   private static final float GRID_Y = 56.0F;
   private static final float SEARCH_Y = 30.0F;
   private static final float SEARCH_H = 18.0F;
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;
   private static final float SWATCH = 22.0F;
   private static final float SWATCH_GAP = 4.0F;
   private static final float SLIDER_HEIGHT = 14.0F;
   private static final float PALETTE_COLS = 8.0F;

   private static final int[] PALETTE = new int[]{
      0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFFA8E10C, 0xFF00E5A0, 0xFF00B8D9, 0xFF4DA6FF, 0xFF5E5CE6,
      0xFFBF5AF2, 0xFFFF2D92, 0xFFFF9FC7, 0xFF00FF7F, 0xFFFFD700, 0xFF964B00, 0xFF8E8E93, 0xFFFFFFFF,
      0xFF111111, 0xFF00FFFF
   };

   private final List<Block> blocks = new ArrayList();
   private final BlockMapSetting setting;
   private Block editing;
   private String searchBuffer = "";
   private boolean searchFocused;
   private float listScroll;
   private int draggingChannel = -1;

   public BlockPickerScreen() {
      super(Text.literal("Выбор блоков"));
      this.setting = BlockESP.INSTANCE.getBlocks();
      for (int i = 0; i < Registries.BLOCK.size(); i++) {
         Block block = Registries.BLOCK.get(i);
         if (block != null) {
            this.blocks.add(block);
         }
      }
      this.blocks.sort(Comparator.comparing((block) -> block.getName().getString()));
   }

   public static void open() {
      if (mc.currentScreen instanceof BlockPickerScreen) {
         return;
      }
      mc.setScreen(new BlockPickerScreen());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private float gridWidth() {
      return this.width - MARGIN * 2.0F - PANEL_W;
   }

   private int columns() {
      return Math.max(1, (int) (this.gridWidth() / CELL_SIZE));
   }

   private int paletteRows() {
      return (int) Math.ceil((double) PALETTE.length / (double) PALETTE_COLS);
   }

   private float paletteTop() {
      return GRID_Y + 98.0F;
   }

   private float sliderTop() {
      return GRID_Y + 200.0F;
   }

   private float previewY() {
      return GRID_Y + 288.0F;
   }

   private float buttonsY() {
      return GRID_Y + 322.0F;
   }

   private List<Block> filtered() {
      String query = this.searchBuffer.trim().toLowerCase();
      if (query.isEmpty()) {
         return this.blocks;
      }
      List<Block> result = new ArrayList();
      for (Block block : this.blocks) {
         String name = block.getName().getString().toLowerCase();
         String id = BlockMapSetting.getId(block).toLowerCase();
         if (name.contains(query) || id.contains(query)) {
            result.add(block);
         }
      }
      return result;
   }

   private void clampScroll(int count) {
      int rows = (int) Math.ceil((double) count / (double) this.columns());
      float max = Math.max(0.0F, (float) rows * CELL_SIZE - (this.height - GRID_Y - MARGIN));
      this.listScroll = MathHelper.clamp(this.listScroll, 0.0F, max);
   }

   private void selectBlock(Block block) {
      String id = BlockMapSetting.getId(block);
      if (!this.setting.contains(id)) {
         int color = HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB();
         this.setting.set(id, color);
         BlockESP.INSTANCE.markDirty();
      }
      this.editing = block;
   }

   private void updateSlider(int mouseX, int channel) {
      if (this.editing == null) {
         return;
      }
      float x = this.width - PANEL_W - MARGIN + 12.0F;
      float w = PANEL_W - 24.0F;
      float percent = MathHelper.clamp((float) (mouseX - x) / w, 0.0F, 1.0F);
      int value = Math.round(percent * 255.0F);
      ColorRGBA color = new ColorRGBA(this.setting.getColor(BlockMapSetting.getId(this.editing)));
      int red = color.getRed();
      int green = color.getGreen();
      int blue = color.getBlue();
      if (channel == 0) {
         red = value;
      } else if (channel == 1) {
         green = value;
      } else {
         blue = value;
      }
      this.setting.set(BlockMapSetting.getId(this.editing), new ColorRGBA(red, green, blue, 255).getRGB());
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      if (this.draggingChannel >= 0) {
         this.updateSlider(mouseX, this.draggingChannel);
      }
      CustomDrawContext draw = CustomDrawContext.of(context);
      ColorRGBA themeColor = HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor();

      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 130));

      draw.drawText(Fonts.REGULAR.getFont(6.5F), "Выбор блоков", MARGIN, MARGIN, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "ЛКМ — добавить/выбрать блок, ПКМ — убрать", MARGIN, MARGIN + 13.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
      boolean exitHovered = MathUtil.isHovered(mouseX, mouseY, exitX, EXIT_OFFSET, EXIT_WIDTH, EXIT_HEIGHT);
      DrawUtil.drawRoundedRect(draw.getMatrices(), exitX, EXIT_OFFSET, EXIT_WIDTH, EXIT_HEIGHT, BorderRadius.all(3.0F), exitHovered ? themeColor.withAlpha(110) : new ColorRGBA(15, 15, 15).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), "Выход", exitX + EXIT_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Выход", 5.5F) / 2.0F, EXIT_OFFSET + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      this.renderSearch(draw, themeColor, mouseX, mouseY);
      this.renderGrid(draw, themeColor, mouseX, mouseY);
      this.renderPanel(draw, themeColor, mouseX, mouseY);
   }

   private void renderSearch(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float x = MARGIN;
      float y = SEARCH_Y;
      float w = this.gridWidth();
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, SEARCH_H);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, SEARCH_H, BorderRadius.all(4.0F), new ColorRGBA(15, 15, 15).withAlpha(230));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, SEARCH_H, 1.0F, BorderRadius.all(4.0F), this.searchFocused ? themeColor.withAlpha(160) : (hovered ? themeColor.withAlpha(80) : new ColorRGBA(50, 50, 50).withAlpha(255)));
      if (this.searchBuffer.isEmpty() && !this.searchFocused) {
         draw.drawText(Fonts.REGULAR.getFont(5.0F), "Поиск блока...", x + 6.0F, y + 5.0F, new ColorRGBA(100, 100, 100).withAlpha(255));
      } else {
         String text = this.searchBuffer + (this.searchFocused && System.currentTimeMillis() % 1000L > 500L ? "|" : "");
         draw.drawText(Fonts.REGULAR.getFont(5.0F), text, x + 6.0F, y + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      }
   }

   private void renderGrid(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float x = MARGIN;
      float y = GRID_Y;
      float w = this.gridWidth();
      float h = this.height - GRID_Y - MARGIN;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(6.0F), new ColorRGBA(15, 15, 15).withAlpha(200));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(40));

      List<Block> filtered = this.filtered();
      this.clampScroll(filtered.size());
      int columns = this.columns();
      draw.enableScissor((int) x + 2, (int) y + 2, (int) (x + w - 2), (int) (y + h - 2));
      for (int i = 0; i < filtered.size(); i++) {
         Block block = filtered.get(i);
         float cellX = x + (float) (i % columns) * CELL_SIZE;
         float cellY = y - this.listScroll + (float) (i / columns) * CELL_SIZE;
         if (cellY + CELL_SIZE < y || cellY > y + h) {
            continue;
         }
         this.renderCell(draw, block, cellX, cellY, mouseX, mouseY);
      }
      draw.disableScissor();
   }

   private void renderCell(CustomDrawContext draw, Block block, float x, float y, int mouseX, int mouseY) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, CELL_SIZE - 4.0F, CELL_SIZE - 4.0F);
      String id = BlockMapSetting.getId(block);
      boolean selected = this.setting.contains(id);
      Integer color = selected ? this.setting.getColor(id) : null;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, y + 2.0F, CELL_SIZE - 4.0F, CELL_SIZE - 4.0F, BorderRadius.all(4.0F), new ColorRGBA(hovered ? 30 : 22, hovered ? 30 : 22, hovered ? 34 : 25).withAlpha(255));
      ColorRGBA border = selected ? new ColorRGBA(color) : (hovered ? new ColorRGBA(255, 255, 255).withAlpha(90) : new ColorRGBA(42, 42, 46).withAlpha(255));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x + 2.0F, y + 2.0F, CELL_SIZE - 4.0F, CELL_SIZE - 4.0F, selected ? 1.5F : 1.0F, BorderRadius.all(4.0F), border);

      ItemStack stack = new ItemStack(block.asItem());
      if (!stack.isEmpty()) {
         draw.getMatrices().push();
         draw.getMatrices().translate(x + CELL_SIZE / 2.0F - 8.0F, y + 8.0F, 0.0F);
         draw.getMatrices().scale(2.0F, 2.0F, 1.0F);
         draw.drawItem(stack, 0, 0);
         draw.getMatrices().pop();
      } else {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + CELL_SIZE / 2.0F - 12.0F, y + 8.0F, 24.0F, 24.0F, BorderRadius.all(4.0F), new ColorRGBA(70, 70, 75).withAlpha(255));
      }

      String name = block.getName().getString();
      float maxW = CELL_SIZE - 8.0F;
      while (Fonts.REGULAR.getWidth(name, 4.5F) > maxW && name.length() > 1) {
         name = name.substring(0, name.length() - 1);
      }
      draw.drawText(Fonts.REGULAR.getFont(4.5F), name, x + (CELL_SIZE - 4.0F) / 2.0F - Fonts.REGULAR.getWidth(name, 4.5F) / 2.0F, y + 42.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private void renderPanel(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY) {
      float px = this.width - PANEL_W - MARGIN;
      float py = GRID_Y;
      float ph = this.height - GRID_Y - MARGIN;
      DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, PANEL_W, ph, BorderRadius.all(6.0F), new ColorRGBA(15, 15, 15).withAlpha(215));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), px, py, PANEL_W, ph, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(60));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Настройка блока", px + 12.0F, py + 8.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      if (this.editing == null) {
         draw.drawText(Fonts.REGULAR.getFont(5.0F), "Кликни по блоку слева,", px + 12.0F, py + 40.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
         draw.drawText(Fonts.REGULAR.getFont(5.0F), "чтобы настроить его цвет", px + 12.0F, py + 50.0F, new ColorRGBA(153, 153, 153).withAlpha(255));
         return;
      }

      String id = BlockMapSetting.getId(this.editing);
      int color = this.setting.getColor(id);
      String name = this.editing.getName().getString();
      float maxNameW = PANEL_W - 24.0F;
      while (Fonts.REGULAR.getWidth(name, 6.0F) > maxNameW && name.length() > 1) {
         name = name.substring(0, name.length() - 1);
      }
      draw.drawText(Fonts.REGULAR.getFont(6.0F), name, px + 12.0F, py + 32.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      ColorRGBA current = new ColorRGBA(color);
      DrawUtil.drawRoundedRect(draw.getMatrices(), px + 12.0F, py + 52.0F, 44.0F, 24.0F, BorderRadius.all(4.0F), current);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), px + 12.0F, py + 52.0F, 44.0F, 24.0F, 1.0F, BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255).withAlpha(70));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "#" + String.format("%02X%02X%02X", current.getRed(), current.getGreen(), current.getBlue()), px + 64.0F, py + 58.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Цвет", px + 12.0F, this.paletteTop() - 14.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
      for (int i = 0; i < PALETTE.length; i++) {
         float sx = px + 12.0F + (float) (i % (int) PALETTE_COLS) * (SWATCH + SWATCH_GAP);
         float sy = this.paletteTop() + (float) (i / (int) PALETTE_COLS) * (SWATCH + SWATCH_GAP);
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, sx, sy, SWATCH, SWATCH);
         boolean active = (color & 16777215) == (PALETTE[i] & 16777215);
         DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy, SWATCH, SWATCH, BorderRadius.all(4.0F), ColorRGBA.fromInt(PALETTE[i]));
         DrawUtil.drawRoundedBorder(draw.getMatrices(), sx, sy, SWATCH, SWATCH, active ? 2.0F : 1.0F, BorderRadius.all(4.0F), active ? new ColorRGBA(255, 255, 255).withAlpha(255) : (hovered ? new ColorRGBA(255, 255, 255).withAlpha(120) : new ColorRGBA(0, 0, 0).withAlpha(140)));
      }

      draw.drawText(Fonts.REGULAR.getFont(5.0F), "RGB", px + 12.0F, this.sliderTop() - 14.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
      float sliderW = PANEL_W - 24.0F;
      for (int channel = 0; channel < 3; channel++) {
         this.renderSlider(draw, themeColor, mouseX, mouseY, px + 12.0F, this.sliderTop() + (float) channel * (SLIDER_HEIGHT + 12.0F), channel == 0 ? "R" : channel == 1 ? "G" : "B", channel, color, sliderW);
      }

      ColorRGBA preview = new ColorRGBA(current.getRed(), current.getGreen(), current.getBlue());
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Предпросмотр", px + 12.0F, this.previewY() - 16.0F, new ColorRGBA(200, 200, 200).withAlpha(255));
      DrawUtil.drawRoundedRect(draw.getMatrices(), px + 12.0F, this.previewY(), PANEL_W - 24.0F, 20.0F, BorderRadius.all(4.0F), preview, preview, preview.mix(ColorRGBA.BLACK, 0.35F), preview.mix(ColorRGBA.BLACK, 0.35F));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "#" + String.format("%02X%02X%02X", current.getRed(), current.getGreen(), current.getBlue()), px + 16.0F, this.previewY() + 5.0F, new ColorRGBA(30, 30, 30).withAlpha(200));

      float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
      this.renderButton(draw, themeColor, mouseX, mouseY, px + 12.0F, this.buttonsY(), btnW, "Убрать");
      this.renderButton(draw, themeColor, mouseX, mouseY, px + 12.0F + btnW + 10.0F, this.buttonsY(), btnW, "Готово");
   }

   private void renderSlider(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY, float x, float y, String label, int channel, int color, float w) {
      ColorRGBA base = new ColorRGBA(color);
      int red = base.getRed();
      int green = base.getGreen();
      int blue = base.getBlue();
      int value = channel == 0 ? red : channel == 1 ? green : blue;
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x, y, new ColorRGBA(200, 200, 200).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), String.valueOf(value), x + w - Fonts.REGULAR.getWidth(String.valueOf(value), 5.0F), y, new ColorRGBA(153, 153, 153).withAlpha(255));

      float trackY = y + 10.0F;
      ColorRGBA c0 = new ColorRGBA(channel == 0 ? 0 : red, channel == 1 ? 0 : green, channel == 2 ? 0 : blue);
      ColorRGBA c1 = new ColorRGBA(channel == 0 ? 255 : red, channel == 1 ? 255 : green, channel == 2 ? 255 : blue);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, w, 4.0F, BorderRadius.all(2.0F), c0, c0, c1, c1);
      float knob = w * (float) value / 255.0F;
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, trackY - 4.0F, w, 12.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + knob - 3.0F, trackY - 3.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), (this.draggingChannel == channel || hovered) ? themeColor.withAlpha(255) : themeColor.withAlpha(180));
   }

   private void renderButton(CustomDrawContext draw, ColorRGBA themeColor, int mouseX, int mouseY, float x, float y, float w, String label) {
      boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, 18.0F);
      ColorRGBA fill = hovered ? themeColor.withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, 18.0F, BorderRadius.all(4.0F), fill);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), label, x + w / 2.0F - Fonts.REGULAR.getWidth(label, 5.0F) / 2.0F, y + 6.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         float exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
         if (MathUtil.isHovered(mouseX, mouseY, exitX, EXIT_OFFSET, EXIT_WIDTH, EXIT_HEIGHT)) {
            this.close();
            return true;
         }
         float gx = MARGIN;
         float gy = GRID_Y;
         float gw = this.gridWidth();
         float gh = this.height - GRID_Y - MARGIN;
         if (MathUtil.isHovered(mouseX, mouseY, gx, SEARCH_Y, gw, SEARCH_H)) {
            this.searchFocused = true;
            return true;
         }
         this.searchFocused = false;
         if (MathUtil.isHovered(mouseX, mouseY, gx, gy, gw, gh)) {
            List<Block> filtered = this.filtered();
            int columns = this.columns();
            int index = (int) ((mouseX - gx) / CELL_SIZE) + (int) ((mouseY - (gy - this.listScroll)) / CELL_SIZE) * columns;
            if (index >= 0 && index < filtered.size()) {
               this.selectBlock(filtered.get(index));
            }
            return true;
         }
         if (this.editing != null) {
            float px = this.width - PANEL_W - MARGIN;
            for (int i = 0; i < PALETTE.length; i++) {
               float sx = px + 12.0F + (float) (i % (int) PALETTE_COLS) * (SWATCH + SWATCH_GAP);
               float sy = this.paletteTop() + (float) (i / (int) PALETTE_COLS) * (SWATCH + SWATCH_GAP);
               if (MathUtil.isHovered(mouseX, mouseY, sx, sy, SWATCH, SWATCH)) {
                  this.setting.set(BlockMapSetting.getId(this.editing), PALETTE[i]);
                  return true;
               }
            }
            float sliderW = PANEL_W - 24.0F;
            for (int channel = 0; channel < 3; channel++) {
               float sy2 = this.sliderTop() + (float) channel * (SLIDER_HEIGHT + 12.0F) + 6.0F;
               if (MathUtil.isHovered(mouseX, mouseY, px + 12.0F, sy2, sliderW, 12.0F)) {
                  this.draggingChannel = channel;
                  this.updateSlider((int) mouseX, channel);
                  return true;
               }
            }
            float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
            if (MathUtil.isHovered(mouseX, mouseY, px + 12.0F, this.buttonsY(), btnW, 18.0F)) {
               this.setting.remove(BlockMapSetting.getId(this.editing));
               BlockESP.INSTANCE.markDirty();
               this.editing = null;
               return true;
            }
            if (MathUtil.isHovered(mouseX, mouseY, px + 12.0F + btnW + 10.0F, this.buttonsY(), btnW, 18.0F)) {
               this.close();
               return true;
            }
         }
         return true;
      }
      if (button == 1) {
         float gx = MARGIN;
         float gy = GRID_Y;
         float gw = this.gridWidth();
         float gh = this.height - GRID_Y - MARGIN;
         if (MathUtil.isHovered(mouseX, mouseY, gx, gy, gw, gh)) {
            List<Block> filtered = this.filtered();
            int columns = this.columns();
            int index = (int) ((mouseX - gx) / CELL_SIZE) + (int) ((mouseY - (gy - this.listScroll)) / CELL_SIZE) * columns;
            if (index >= 0 && index < filtered.size()) {
               Block block = filtered.get(index);
               String id = BlockMapSetting.getId(block);
               if (this.setting.contains(id)) {
                  this.setting.remove(id);
                  BlockESP.INSTANCE.markDirty();
                  if (this.editing == block) {
                     this.editing = null;
                  }
               }
            }
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.draggingChannel = -1;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float gx = MARGIN;
      float gy = GRID_Y;
      float gw = this.gridWidth();
      float gh = this.height - GRID_Y - MARGIN;
      if (MathUtil.isHovered(mouseX, mouseY, gx, gy, gw, gh)) {
         this.listScroll -= (float) verticalAmount * 32.0F;
         this.clampScroll(this.filtered().size());
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.searchFocused) {
         if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.searchBuffer.isEmpty()) {
               this.searchBuffer = this.searchBuffer.substring(0, this.searchBuffer.length() - 1);
            }
         } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.searchFocused = false;
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
      if (this.searchFocused) {
         if (this.searchBuffer.length() < 32) {
            this.searchBuffer = this.searchBuffer + chr;
         }
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   @Override
   public void close() {
      super.close();
   }
}
