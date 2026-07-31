package tech.huihui.client.screens.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.impl.render.ClickGUI;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.utility.game.other.ReplaceUtil;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class ClickGuiScreen extends Screen implements IClient {
   public static final float HEADER_HEIGHT = 24.0F;
   public static final float MODULE_HEIGHT = 20.0F;
   private static final float SEARCH_WIDTH = 140.0F;
   private static final float SEARCH_HEIGHT = 18.0F;

   private final List<Panel> panels = new ArrayList();
   private final Animation openAnimation = new Animation(300L, Easing.EXPO_OUT);
   private boolean closing;
   private String searchText = "";
   private boolean searchFocused;
   private float scale = 1.0F;

   private static float transformScale = 1.0F;
   private static int screenWidth;
   private static int screenHeight;

   public static float transformX(float x) {
      return (float) screenWidth / 2.0F + (x - (float) screenWidth / 2.0F) * transformScale;
   }

   public static float transformY(float y) {
      return (float) screenHeight / 2.0F + (y - (float) screenHeight / 2.0F) * transformScale;
   }

   public ClickGuiScreen() {
      super(Text.of("ClickGUI"));
      for (Category category : Category.values()) {
         if (category != Category.THEMES) {
            this.panels.add(new Panel(this, category));
         }
      }
   }

   public void resetSearch() {
      this.searchText = "";
      this.searchFocused = false;
   }

   float panelWidth() {
      return EditClickGUI.INSTANCE.getWidth().getCurrent();
   }

   float panelGap() {
      return EditClickGUI.INSTANCE.getGap().getCurrent();
   }

   float panelHeight() {
      return EditClickGUI.INSTANCE.getHeight().getCurrent();
   }

   int panelOpacity() {
      return (int) EditClickGUI.INSTANCE.getOpacity().getCurrent();
   }

   float panelRadius() {
      return EditClickGUI.INSTANCE.getRadius().getCurrent();
   }

   private void updateLayout() {
      screenWidth = mw.getScaledWidth();
      screenHeight = mw.getScaledHeight();
      float width = this.panelWidth();
      float gap = this.panelGap();
      float height = this.panelHeight();
      float rowWidth = (float) this.panels.size() * (width + gap) - gap;
      float fitScale = Math.min((float) (screenWidth - 16) / rowWidth, (float) (screenHeight / 2 - 2) / (height / 2.0F + 26.0F));
      fitScale = MathHelper.clamp(fitScale, 0.5F, 1.0F);
      this.scale = MathHelper.clamp(fitScale * EditClickGUI.INSTANCE.getScale().getCurrent(), 0.35F, 1.5F);

      float panelX = ((float) screenWidth - rowWidth) / 2.0F;
      float panelY = (float) screenHeight / 2.0F - height / 2.0F;
      for (Panel panel : this.panels) {
         panel.setX(panelX);
         panel.setY(panelY);
         panelX += width + gap;
      }
   }

   private float searchX() {
      return (float) screenWidth / 2.0F - SEARCH_WIDTH / 2.0F;
   }

   private float searchY() {
      return (float) screenHeight / 2.0F - this.panelHeight() / 2.0F - 26.0F;
   }

   private boolean isSearchHovered(float mouseX, float mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.searchX(), this.searchY(), SEARCH_WIDTH, SEARCH_HEIGHT);
   }

   public boolean matchesSearch(Module module) {
      if (this.searchText.isEmpty()) {
         return true;
      }
      String text = this.searchText.toLowerCase(Locale.ROOT);
      String name = module.getName().toLowerCase(Locale.ROOT);
      String qwerty = ReplaceUtil.toQwerty(this.searchText).toLowerCase(Locale.ROOT);
      return name.contains(text) || text.contains(name) || name.contains(qwerty) || qwerty.contains(name);
   }

   public void scissor(CustomDrawContext draw, float x, float y, float width, float height) {
      draw.enableScissor((int) transformX(x), (int) transformY(y), (int) transformX(x + width), (int) transformY(y + height));
   }

   private float inverseX(double x) {
      float scale = Math.max(transformScale, 0.01F);
      return (float) screenWidth / 2.0F + (float) (x - (double) screenWidth / 2.0D) / scale;
   }

   private float inverseY(double y) {
      float scale = Math.max(transformScale, 0.01F);
      return (float) screenHeight / 2.0F + (float) (y - (double) screenHeight / 2.0D) / scale;
   }

   @Override
   protected void init() {
      this.closing = false;
      this.searchText = "";
      this.searchFocused = false;
      this.openAnimation.animateTo(1.0F);
      super.init();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.openAnimation.update();
      if (this.closing && this.openAnimation.getValue() <= 0.02F) {
         ClickGUI.INSTANCE.setEnabled(false);
         mc.setScreen(null);
         return;
      }
      this.updateLayout();
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      float anim = this.openAnimation.getValue();
      float s = Math.max(anim * this.scale, 0.001F);
      transformScale = s;

      context.getMatrices().push();
      context.getMatrices().translate((float) screenWidth / 2.0F, (float) screenHeight / 2.0F, 0.0F);
      context.getMatrices().scale(s, s, 1.0F);
      context.getMatrices().translate((float) (-screenWidth) / 2.0F, (float) (-screenHeight) / 2.0F, 0.0F);

      CustomDrawContext draw = CustomDrawContext.of(context);
      this.renderSearch(draw, theme, anim);

      float width = this.panelWidth();
      float height = this.panelHeight();
      float halfRest = (1.0F - s) / 2.0F;
      for (Panel panel : this.panels) {
         float testX = (panel.getX() + width * halfRest) * s + (float) (screenWidth - (int) (width * s)) * halfRest;
         float testY = (panel.getY() + height * halfRest) * s + (float) (screenHeight - (int) (height * s)) * halfRest;
         float testW = width * s;
         float testH = height * s;
         draw.enableScissor((int) testX - 2, (int) testY - 2, (int) (testX + testW) + 2, (int) (testY + testH) + 2);
         panel.render(draw, theme, this.inverseX(mouseX), this.inverseY(mouseY), anim);
         draw.disableScissor();
      }

      context.getMatrices().pop();
   }

   private void renderSearch(CustomDrawContext draw, Theme theme, float alpha) {
      float x = this.searchX();
      float y = this.searchY();
      boolean active = this.searchFocused || !this.searchText.isEmpty();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, SEARCH_WIDTH, SEARCH_HEIGHT, BorderRadius.all(6.0F), (new ColorRGBA(15, 15, 15)).withAlpha(200.0F * alpha));
      if (active) {
         DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, SEARCH_WIDTH, SEARCH_HEIGHT, 1.0F, BorderRadius.all(6.0F), theme.getColor().withAlpha(90.0F * alpha));
      }
      if (this.searchText.isEmpty() && !this.searchFocused) {
         draw.drawText(Fonts.LUPA.getFont(4.5F), "\uf002", x + 5.0F, y + 6.0F, (new ColorRGBA(120, 120, 120)).withAlpha(255.0F * alpha));
         draw.drawText(Fonts.REGULAR.getFont(6.0F), "Поиск", x + 13.0F, y + 6.0F, (new ColorRGBA(120, 120, 120)).withAlpha(255.0F * alpha));
      } else {
         String text = this.searchText + (this.searchFocused && System.currentTimeMillis() % 1000L > 500L ? "|" : "");
         this.scissor(draw, x, y, SEARCH_WIDTH, SEARCH_HEIGHT);
         draw.drawText(Fonts.REGULAR.getFont(6.0F), text, x + 6.0F, y + 6.0F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));
         draw.disableScissor();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float mx = this.inverseX(mouseX);
      float my = this.inverseY(mouseY);
      if (this.isSearchHovered(mx, my)) {
         this.searchFocused = true;
         return true;
      }
      this.searchFocused = false;
      for (Panel panel : this.panels) {
         if (panel.mouseClick(mx, my, button)) {
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      float mx = this.inverseX(mouseX);
      float my = this.inverseY(mouseY);
      for (Panel panel : this.panels) {
         panel.mouseDragged(mx, my, button);
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      float mx = this.inverseX(mouseX);
      float my = this.inverseY(mouseY);
      for (Panel panel : this.panels) {
         panel.mouseRelease(mx, my, button);
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float mx = this.inverseX(mouseX);
      float my = this.inverseY(mouseY);
      for (Panel panel : this.panels) {
         if (MathUtil.isHovered(mx, my, panel.getX(), panel.getY(), this.panelWidth(), this.panelHeight())) {
            panel.mouseScrolled(verticalAmount);
            return true;
         }
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.closing) {
         return true;
      }
      if (this.searchFocused) {
         if (keyCode == 259) {
            if (!this.searchText.isEmpty()) {
               this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
            }
         } else if (keyCode == 256 || keyCode == 257) {
            this.searchFocused = false;
         }
         return true;
      }
      for (Panel panel : this.panels) {
         if (panel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
         }
      }
      if (keyCode == 256) {
         this.closing = true;
         this.openAnimation.animateTo(0.0F);
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.closing) {
         return true;
      }
      if (this.searchFocused) {
         if (this.searchText.length() < 24) {
            this.searchText = this.searchText + chr;
         }
         return true;
      }
      for (Panel panel : this.panels) {
         if (panel.charTyped(chr, modifiers)) {
            return true;
         }
      }
      return super.charTyped(chr, modifiers);
   }
}
