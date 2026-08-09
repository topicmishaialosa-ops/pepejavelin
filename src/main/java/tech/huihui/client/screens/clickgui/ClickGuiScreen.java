package tech.huihui.client.screens.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.base.theme.ThemeManager;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.impl.render.ClickGUI;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.client.screens.theme.ThemeEditorScreen;
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
   private static final float THEME_WIDTH = 150.0F;
   private static final float THEME_HEIGHT = 18.0F;
   private static final float THEME_ROW_HEIGHT = 16.0F;
   private static final float EDIT_BTN_SIZE = 18.0F;

   private final List<Panel> panels = new ArrayList();
   private final Animation openAnimation = new Animation(300L, Easing.EXPO_OUT);
   private final Animation themeAnim = new Animation(180L, Easing.CUBIC_OUT);
   private boolean closing;
   private boolean themeOpen;
   private String searchText = "";
   private boolean searchFocused;
   private float scale = 1.0F;
   private Module hoveredModule;

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
      this.themeOpen = false;
   }

   public void setHoveredModule(Module module) {
      this.hoveredModule = module;
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

   ColorRGBA panelBg() {
      return EditClickGUI.INSTANCE.getBgColor().getColor();
   }

   ColorRGBA panelBorder() {
      return EditClickGUI.INSTANCE.getBorderColor().getColor();
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
      this.scale = MathHelper.clamp(fitScale * EditClickGUI.INSTANCE.getScale().getCurrent(), 0.35F, fitScale);

      float panelX = ((float) screenWidth - rowWidth) / 2.0F;
      float panelY = (float) screenHeight / 2.0F - height / 2.0F;
      for (Panel panel : this.panels) {
         panel.setX(panelX);
         panel.setY(panelY);
         panelX += width + gap;
      }
   }

   private float topBarWidth() {
      return EDIT_BTN_SIZE + 4.0F + THEME_WIDTH + 8.0F + SEARCH_WIDTH;
   }

   private float topBarStart() {
      return (float) screenWidth / 2.0F - this.topBarWidth() / 2.0F;
   }

   private float searchX() {
      return this.topBarStart() + EDIT_BTN_SIZE + 4.0F + THEME_WIDTH + 8.0F;
   }

   private float searchY() {
      return (float) screenHeight / 2.0F - this.panelHeight() / 2.0F - 26.0F;
   }

   private float themeX() {
      return this.topBarStart() + EDIT_BTN_SIZE + 4.0F;
   }

   private float editBtnX() {
      return this.topBarStart();
   }

   private float editBtnY() {
      return this.themeY();
   }

   private boolean isEditBtnHovered(float mouseX, float mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.editBtnX(), this.editBtnY(), EDIT_BTN_SIZE, EDIT_BTN_SIZE);
   }

   private float themeY() {
      return this.searchY();
   }

   private float themeListY() {
      return this.themeY() + THEME_HEIGHT + 4.0F;
   }

   private float themeListHeight() {
      return (float) HuihuiClient.getInstance().getThemeManager().getThemes().size() * THEME_ROW_HEIGHT;
   }

   private boolean isThemeBoxHovered(float mouseX, float mouseY) {
      return MathUtil.isHovered(mouseX, mouseY, this.themeX(), this.themeY(), THEME_WIDTH, THEME_HEIGHT);
   }

   private Theme themeAt(float mouseX, float mouseY) {
      float x = this.themeX();
      float y = this.themeListY();
      if (mouseX < x || mouseX > x + THEME_WIDTH || mouseY < y) {
         return null;
      }
      int index = (int) ((mouseY - y) / THEME_ROW_HEIGHT);
      if (index < 0 || index >= HuihuiClient.getInstance().getThemeManager().getThemes().size()) {
         return null;
      }
      return HuihuiClient.getInstance().getThemeManager().getThemes().get(index);
   }

   private void selectTheme(Theme theme) {
      ThemeManager themeManager = HuihuiClient.getInstance().getThemeManager();
      if (themeManager.getCurrentTheme() != theme) {
         theme.getAnimation().setValue(0.0F);
         theme.startAnimation(themeManager.getCurrentTheme().getColor1(), themeManager.getCurrentTheme().getColor2());
         themeManager.setCurrentTheme(theme);
      }
      this.themeOpen = false;
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
      this.renderTheme(draw, theme, anim, mouseX, mouseY);
      this.hoveredModule = null;

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

      this.renderDescription(draw, theme, anim);

      context.getMatrices().pop();
   }

   private void renderDescription(CustomDrawContext draw, Theme theme, float alpha) {
      Module module = this.hoveredModule;
      if (module == null) {
         return;
      }
      String description = module.getInfo().description();
      if (description == null || description.isEmpty()) {
         return;
      }
      Font font = Fonts.REGULAR.getFont(5.5F);
      float textWidth = font.width(description);
      float boxWidth = textWidth + 16.0F;
      float boxHeight = 18.0F;
      float x = ((float) screenWidth - boxWidth) / 2.0F;
      float y = (float) screenHeight - boxHeight - 8.0F;
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, boxWidth, boxHeight, BorderRadius.all(6.0F), this.panelBg().withAlpha((int) (200.0F * alpha)));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, boxWidth, boxHeight, 1.0F, BorderRadius.all(6.0F), theme.getColor().withAlpha(90.0F * alpha));
      draw.drawText(font, description, x + 8.0F, y + 6.0F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));
   }

   private void renderSearch(CustomDrawContext draw, Theme theme, float alpha) {
      float x = this.searchX();
      float y = this.searchY();
      boolean active = this.searchFocused || !this.searchText.isEmpty();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, SEARCH_WIDTH, SEARCH_HEIGHT, BorderRadius.all(6.0F), this.panelBg().withAlpha((int) (200.0F * alpha)));
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

   private void renderTheme(CustomDrawContext draw, Theme theme, float alpha, int rawMouseX, int rawMouseY) {
      this.themeAnim.update(this.themeOpen);
      float x = this.themeX();
      float y = this.themeY();
      float mx = this.inverseX(rawMouseX);
      float my = this.inverseY(rawMouseY);
      ThemeManager themeManager = HuihuiClient.getInstance().getThemeManager();
      Theme current = themeManager.getCurrentTheme();

      boolean hovered = this.isThemeBoxHovered(mx, my);
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, THEME_WIDTH, THEME_HEIGHT, BorderRadius.all(6.0F), this.panelBg().withAlpha((int) (200.0F * alpha)));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, THEME_WIDTH, THEME_HEIGHT, 1.0F, BorderRadius.all(6.0F), theme.getColor().withAlpha(hovered ? 130.0F : 90.0F).withAlpha(255.0F * alpha));

      float ebx = this.editBtnX();
      float eby = this.editBtnY();
      boolean editHovered = this.isEditBtnHovered(mx, my);
      DrawUtil.drawRoundedRect(draw.getMatrices(), ebx, eby, EDIT_BTN_SIZE, EDIT_BTN_SIZE, BorderRadius.all(6.0F), editHovered ? theme.getColor().withAlpha(120.0F * alpha) : this.panelBg().withAlpha((int) (200.0F * alpha)));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), ebx, eby, EDIT_BTN_SIZE, EDIT_BTN_SIZE, 1.0F, BorderRadius.all(6.0F), theme.getColor().withAlpha(editHovered ? 200.0F : 90.0F).withAlpha(255.0F * alpha));
      ColorRGBA pencil = (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 11.5F, eby + 4.5F), new Vec2f(ebx + 5.0F, eby + 11.0F), pencil);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 5.0F, eby + 11.0F), new Vec2f(ebx + 7.5F, eby + 13.5F), pencil);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 12.5F, eby + 5.5F), new Vec2f(ebx + 9.5F, eby + 8.5F), pencil);

      DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, y + 5.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), current.getColor().withAlpha(255.0F * alpha), current.getColor().withAlpha(255.0F * alpha), current.getSecondColor().withAlpha(255.0F * alpha), current.getSecondColor().withAlpha(255.0F * alpha));
      draw.drawText(Fonts.REGULAR.getFont(5.5F), current.getName(), x + 17.0F, y + 6.0F, (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha));

      float cxp = x + THEME_WIDTH - 13.0F;
      float cyp = y + THEME_HEIGHT / 2.0F;
      ColorRGBA chevron = (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp - 2.0F), new Vec2f(cxp, cyp + 1.0F), chevron);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp + 1.0F), new Vec2f(cxp + 3.0F, cyp - 2.0F), chevron);

      if (this.themeOpen) {
         float listY = this.themeListY();
         float listH = this.themeListHeight() * this.themeAnim.getValue();
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, listY, THEME_WIDTH, Math.max(listH, 2.0F), BorderRadius.all(6.0F), this.panelBg().withAlpha((int) (225.0F * alpha)));
         DrawUtil.drawRoundedBorder(draw.getMatrices(), x, listY, THEME_WIDTH, Math.max(listH, 2.0F), 1.0F, BorderRadius.all(6.0F), theme.getColor().withAlpha(70.0F * alpha));
         this.scissor(draw, x, listY, THEME_WIDTH, listH);
         int row = 0;
         for (Theme candidate : themeManager.getThemes()) {
            float ry = listY + (float) row * THEME_ROW_HEIGHT;
            boolean selected = candidate == current;
            boolean rowHovered = MathUtil.isHovered(mx, my, x, ry, THEME_WIDTH, THEME_ROW_HEIGHT);
            if (selected) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry, THEME_WIDTH - 4.0F, THEME_ROW_HEIGHT, BorderRadius.all(4.0F), theme.getColor().withAlpha(60.0F * alpha));
            } else if (rowHovered) {
               DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry, THEME_WIDTH - 4.0F, THEME_ROW_HEIGHT, BorderRadius.all(4.0F), (new ColorRGBA(60, 60, 60)).withAlpha(120.0F * alpha));
            }
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, ry + 4.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), candidate.getColor(), candidate.getColor(), candidate.getSecondColor(), candidate.getSecondColor());
            draw.drawText(Fonts.REGULAR.getFont(5.0F), candidate.getName(), x + 17.0F, ry + 5.0F, (new ColorRGBA(selected ? 255 : 200, selected ? 255 : 200, selected ? 255 : 200)).withAlpha(255.0F * alpha));
            row++;
         }
         draw.disableScissor();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float mx = this.inverseX(mouseX);
      float my = this.inverseY(mouseY);
      if (this.isSearchHovered(mx, my)) {
         this.searchFocused = true;
         this.themeOpen = false;
         return true;
      }
      if (this.isThemeBoxHovered(mx, my)) {
         this.themeOpen = !this.themeOpen;
         this.searchFocused = false;
         return true;
      }
      if (this.isEditBtnHovered(mx, my)) {
         this.themeOpen = false;
         this.searchFocused = false;
         ThemeEditorScreen.openEditor();
         return true;
      }
      if (this.themeOpen) {
         Theme theme = this.themeAt(mx, my);
         if (theme != null) {
            this.selectTheme(theme);
            return true;
         }
         this.themeOpen = false;
         if (MathUtil.isHovered(mx, my, this.themeX(), this.themeListY(), THEME_WIDTH, this.themeListHeight())) {
            return true;
         }
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
         if (this.themeOpen) {
            this.themeOpen = false;
            return true;
         }
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
