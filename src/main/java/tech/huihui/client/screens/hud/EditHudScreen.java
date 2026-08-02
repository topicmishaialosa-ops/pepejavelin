package tech.huihui.client.screens.hud;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class EditHudScreen extends Screen implements IMinecraft {
   private static final int EXIT_WIDTH = 50;
   private static final int EXIT_HEIGHT = 16;
   private static final int EXIT_OFFSET = 10;
   private static final float PANEL_WIDTH = 200.0F;
   private static final float PANEL_OFFSET = 10.0F;
   private static final float PANEL_PADDING = 10.0F;
   private static final float ROW_HEIGHT = 20.0F;

   private final Interface module;
   private DraggableHudElement draggingElement;
   private float dragOffsetX;
   private float dragOffsetY;

   public EditHudScreen() {
      super(Text.literal("Редактор HUD"));
      this.module = Interface.INSTANCE;
   }

   public static void openEditor() {
      if (mc.currentScreen instanceof EditHudScreen) {
         return;
      }
      mc.setScreen(new EditHudScreen());
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private float panelX() {
      return this.width - PANEL_WIDTH - PANEL_OFFSET;
   }

   private float panelY() {
      return 30.0F;
   }

   private float panelBoxHeight() {
      return Math.max(240.0F, this.height - this.panelY() - 24.0F);
   }

   private boolean inPanel(double mouseX, double mouseY) {
      return mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_WIDTH && mouseY >= this.panelY() + 26.0F && mouseY <= this.panelY() + this.panelBoxHeight();
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

      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Редактор HUD", EXIT_OFFSET, EXIT_OFFSET, new ColorRGBA(222, 222, 222).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Тащи элементы мышью. Чекбоксы справа скрывают их. В чате тоже можно тащить", EXIT_OFFSET, EXIT_OFFSET + 12.0F, new ColorRGBA(153, 153, 153).withAlpha(255));

      this.renderElements(draw, theme, mouseX, mouseY);
      this.renderPanel(draw, theme, mouseX, mouseY);
   }

   private void renderElements(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      if (this.draggingElement != null) {
         this.draggingElement.set(draw, mouseX - this.dragOffsetX, mouseY - this.dragOffsetY, this.module, this.width, this.height);
      }
      List<DraggableHudElement> elements = this.module.getElements();
      for (int i = 0; i < elements.size(); i++) {
         DraggableHudElement element = elements.get(i);
         if (!this.isElementEnabled(i)) {
            continue;
         }
         try {
            element.render(draw);
         } catch (Exception var10) {
            var10.printStackTrace();
         }
         boolean hovered = element.isMouseOver(mouseX, mouseY);
         boolean dragging = element == this.draggingElement;
         if (hovered || dragging) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), element.getX() - 1.0F, element.getY() - 1.0F, element.getWidth() + 2.0F, element.getHeight() + 2.0F, 1.0F, BorderRadius.all(3.0F), theme.getColor().withAlpha(255));
         }
      }
   }

   private void renderPanel(CustomDrawContext draw, Theme theme, float mouseX, float mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      float boxH = this.panelBoxHeight();

      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, BorderRadius.all(5.0F), new ColorRGBA(15, 15, 15).withAlpha(210));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, PANEL_WIDTH, boxH, 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().darker(0.5F).withAlpha(180));
      draw.drawText(Fonts.REGULAR.getFont(6.0F), "Элементы", x + PANEL_PADDING, y + 12.0F, new ColorRGBA(222, 222, 222).withAlpha(255));

      List<DraggableHudElement> elements = this.module.getElements();
      MultiBooleanSetting setting = this.module.getElementsSetting();
      for (int i = 0; i < elements.size(); i++) {
         float rowY = y + 32.0F + (float)i * ROW_HEIGHT;
         boolean enabled = this.isElementEnabled(i);
         String name = i < setting.getBooleanSettings().size() ? setting.getBooleanSettings().get(i).getName() : elements.get(i).getName();
         draw.drawText(Fonts.REGULAR.getFont(5.0F), name, x + PANEL_PADDING, rowY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
         float tx = x + PANEL_WIDTH - PANEL_PADDING - 50.0F;
         boolean hovered = MathUtil.isHovered(mouseX, mouseY, tx, rowY, 50.0F, 16.0F);
         DrawUtil.drawRoundedRect(draw.getMatrices(), tx, rowY, 50.0F, 16.0F, BorderRadius.all(8.0F), enabled ? theme.getColor().withAlpha(hovered ? 210 : 180) : hovered ? new ColorRGBA(58, 58, 58).withAlpha(255) : new ColorRGBA(40, 40, 40).withAlpha(255));
         draw.drawText(Fonts.REGULAR.getFont(5.0F), enabled ? "Вкл" : "Выкл", tx + 25.0F - Fonts.REGULAR.getWidth(enabled ? "Вкл" : "Выкл", 5.0F) / 2.0F, rowY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
      }

      float resetY = y + 32.0F + (float)elements.size() * ROW_HEIGHT + 10.0F;
      boolean resetHovered = MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F, BorderRadius.all(3.0F), resetHovered ? theme.getColor().withAlpha(110) : new ColorRGBA(40, 40, 40).withAlpha(255));
      draw.drawText(Fonts.REGULAR.getFont(5.0F), "Сбросить позиции", x + PANEL_WIDTH / 2.0F - Fonts.REGULAR.getWidth("Сбросить позиции", 5.0F) / 2.0F, resetY + 5.0F, new ColorRGBA(222, 222, 222).withAlpha(255));
   }

   private boolean isElementEnabled(int index) {
      MultiBooleanSetting setting = this.module.getElementsSetting();
      return index >= 0 && index < setting.getBooleanSettings().size() && setting.getBooleanSettings().get(index).isEnabled();
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
      if (this.inPanel(mouseX, mouseY)) {
         this.handlePanelClick(mouseX, mouseY);
         return true;
      }
      List<DraggableHudElement> elements = this.module.getElements();
      for (int i = elements.size() - 1; i >= 0; i--) {
         DraggableHudElement element = elements.get(i);
         if (this.isElementEnabled(i) && element.isMouseOver(mouseX, mouseY)) {
            this.draggingElement = element;
            this.dragOffsetX = (float) mouseX - element.getX();
            this.dragOffsetY = (float) mouseY - element.getY();
            this.module.setDraggingElement(element);
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private void handlePanelClick(double mouseX, double mouseY) {
      float x = this.panelX();
      float y = this.panelY();
      List<DraggableHudElement> elements = this.module.getElements();
      MultiBooleanSetting setting = this.module.getElementsSetting();
      for (int i = 0; i < elements.size(); i++) {
         float rowY = y + 32.0F + (float)i * ROW_HEIGHT;
         if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_WIDTH - PANEL_PADDING - 50.0F, rowY, 50.0F, 16.0F) && i < setting.getBooleanSettings().size()) {
            setting.getBooleanSettings().get(i).toggle();
            return;
         }
      }
      float resetY = y + 32.0F + (float)elements.size() * ROW_HEIGHT + 10.0F;
      if (MathUtil.isHovered(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2.0F, 16.0F)) {
         this.module.resetElementPositions(this.width, this.height);
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.draggingElement != null) {
         this.draggingElement.release();
         this.draggingElement = null;
         this.module.setDraggingElement(null);
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }
}
