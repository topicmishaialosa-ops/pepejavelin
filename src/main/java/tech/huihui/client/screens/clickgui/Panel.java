package tech.huihui.client.screens.clickgui;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
@Setter
public class Panel {
   private final ClickGuiScreen screen;
   private final Category category;
   private final List<ModuleComponent> modules = new ArrayList();
   private float x;
   private float y;
   private float scroll;
   private float animatedScroll;
   private boolean draggingScrollbar;
   private float lastMouseY;
   private float max;

   public Panel(ClickGuiScreen screen, Category category) {
      this.screen = screen;
      this.category = category;

      for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
         if (module.getCategory() == category) {
            this.modules.add(new ModuleComponent(screen, module));
         }
      }
   }

   private float getScrollRange() {
      float header = ClickGuiScreen.HEADER_HEIGHT;
      float height = this.screen.panelHeight();
      return this.max - (height - header - 10.0F);
   }

   private float getScrollbarHeight(float height) {
      float header = ClickGuiScreen.HEADER_HEIGHT;
      return MathHelper.clamp((height - header - 10.0F) * (height - header - 10.0F) / this.max, 10.0F, height - header - 10.0F);
   }

   private float getScrollbarY(float height) {
      float header = ClickGuiScreen.HEADER_HEIGHT;
      float scrollRange = this.getScrollRange();
      float scrollbarHeight = this.getScrollbarHeight(height);
      float scrollbarY = this.y + header + (-this.scroll / scrollRange) * (height - header - 4.0F - scrollbarHeight);
      return MathHelper.clamp(scrollbarY, this.y + header, this.y + height - scrollbarHeight - 4.0F);
   }

   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      float width = this.screen.panelWidth();
      float height = this.screen.panelHeight();
      float header = ClickGuiScreen.HEADER_HEIGHT;
      float radius = this.screen.panelRadius();
      this.animatedScroll += (this.scroll - this.animatedScroll) * 0.2F;

      float maxHeight = 0.0F;
      for (ModuleComponent module : this.modules) {
         if (this.screen.matchesSearch(module.getModule())) {
            maxHeight += module.getCurrentHeight() + 3.5F;
         }
      }
      this.max = Math.max(0.0F, maxHeight - 3.5F);

      float scrollRange = this.getScrollRange();
      if (scrollRange > 0.0F) {
         this.scroll = MathHelper.clamp(this.scroll, -scrollRange, 0.0F);
         this.animatedScroll = MathHelper.clamp(this.animatedScroll, -scrollRange, 0.0F);
      } else {
         this.scroll = 0.0F;
         this.animatedScroll = 0.0F;
      }

      float opacity = (float) this.screen.panelOpacity() * alpha;
      ColorRGBA bg = EditClickGUI.INSTANCE.getBgColor().getColor().withAlpha((int) opacity);
      if (EditClickGUI.INSTANCE.getBlur().isEnabled()) {
         DrawUtil.drawBlur(draw.getMatrices(), this.x, this.y, width, height, 11.0F, BorderRadius.all(radius), bg);
      }
      if (EditClickGUI.INSTANCE.getGradientEnabled().isEnabled()) {
         ColorRGBA bottom = EditClickGUI.INSTANCE.getGradientColor().getColor().withAlpha((int) opacity);
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x, this.y, width, height, BorderRadius.all(radius), bg, bottom, bottom, bg);
      } else {
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x, this.y, width, height, BorderRadius.all(radius), bg);
      }
      ColorRGBA border = EditClickGUI.INSTANCE.getBorderColor().getColor().withAlpha(84.0F * alpha);
      DrawUtil.drawRoundedBorder(draw.getMatrices(), this.x, this.y, width, height, 1.0F, BorderRadius.all(radius), border);

      float contentX = this.x + 13.5F;
      draw.drawText(Fonts.ICONS.getFont(7.0F), this.category.getIcon(), contentX, this.y + header / 2.0F - 4.0F, theme.getColor().withAlpha(255.0F * alpha));
      draw.drawText(Fonts.MEDIUM.getFont(7.5F), this.category.getName(), contentX + 15.0F, this.y + header / 2.0F - 4.5F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));

      this.screen.scissor(draw, this.x + 1.0F, this.y + header, width - 2.0F, height - header - 2.0F);
      float moduleY = this.y + header + this.animatedScroll;
      for (ModuleComponent module : this.modules) {
         if (this.screen.matchesSearch(module.getModule())) {
            module.setX(this.x + 6.5F);
            module.setY(moduleY);
            module.setWidth(width - 13.0F);
            module.render(draw, theme, mouseX, mouseY, alpha);
            moduleY += module.getCurrentHeight() + 3.5F;
         }
      }
      draw.disableScissor();

      if (scrollRange > 0.0F) {
         float scrollbarHeight = this.getScrollbarHeight(height);
         float scrollbarY = this.getScrollbarY(height);
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + width - 3.0F, scrollbarY, 4.0F, scrollbarHeight, BorderRadius.all(2.0F), (new ColorRGBA(153, 153, 153)).withAlpha(100.0F * alpha));
      }
   }

   public boolean mouseClick(float mouseX, float mouseY, int button) {
      if (!MathUtil.isHovered(mouseX, mouseY, this.x, this.y, this.screen.panelWidth(), this.screen.panelHeight())) {
         return false;
      }
      for (ModuleComponent module : this.modules) {
         if (this.screen.matchesSearch(module.getModule()) && module.mouseClick(mouseX, mouseY, button)) {
            return true;
         }
      }
      float height = this.screen.panelHeight();
      if (button == 0 && this.max > height - ClickGuiScreen.HEADER_HEIGHT - 10.0F) {
         float scrollbarHeight = this.getScrollbarHeight(height);
         float scrollbarY = this.getScrollbarY(height);
         if (mouseX >= this.x + this.screen.panelWidth() - 3.5F && mouseX <= this.x + this.screen.panelWidth() + 1.0F && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            this.draggingScrollbar = true;
            this.lastMouseY = mouseY;
            return true;
         }
      }
      return false;
   }

   public void mouseDragged(float mouseX, float mouseY, int button) {
      if (button == 0 && this.draggingScrollbar) {
         float height = this.screen.panelHeight();
         float scrollRange = this.getScrollRange();
         float scrollbarHeight = this.getScrollbarHeight(height);
         float travelBar = height - ClickGuiScreen.HEADER_HEIGHT - 4.0F - scrollbarHeight;
         this.scroll -= (mouseY - this.lastMouseY) * scrollRange / travelBar;
         this.lastMouseY = mouseY;
         this.scroll = MathHelper.clamp(this.scroll, -scrollRange, 0.0F);
      }
   }

   public void mouseRelease(float mouseX, float mouseY, int button) {
      this.draggingScrollbar = false;
      for (ModuleComponent module : this.modules) {
         if (this.screen.matchesSearch(module.getModule())) {
            module.mouseRelease(mouseX, mouseY, button);
         }
      }
   }

   public void mouseScrolled(double amount) {
      this.scroll += (float) (amount * 20.0D);
   }

   public boolean keyPressed(int key, int scanCode, int modifiers) {
      for (ModuleComponent module : this.modules) {
         if (module.keyPressed(key, scanCode, modifiers)) {
            return true;
         }
      }
      return false;
   }

   public boolean charTyped(char codePoint, int modifiers) {
      for (ModuleComponent module : this.modules) {
         if (module.charTyped(codePoint, modifiers)) {
            return true;
         }
      }
      return false;
   }
}
