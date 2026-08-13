package tech.huihui.client.screens.mainmenu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.accounts.AccountManager;
import tech.huihui.client.modules.impl.misc.RenamePasterClient;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class MainMenuScreen extends Screen implements IClient {
   private static final float PANEL_WIDTH = 240.0F;
   private static final float PANEL_HEIGHT = 224.0F;
   private static final float BUTTON_HEIGHT = 32.0F;
   private static final float BUTTON_GAP = 8.0F;

   private final Animation openAnimation = new Animation(500L, Easing.EXPO_OUT);
   private final List<MenuButton> buttons = new ArrayList();

   public MainMenuScreen() {
      super(Text.of(RenamePasterClient.getClientName() + " Menu"));
      this.buttons.add(new MenuButton("Одиночная игра", () -> mc.setScreen(new SelectWorldScreen(this))));
      this.buttons.add(new MenuButton("Мультиплеер", () -> mc.setScreen(new MultiplayerScreen(this))));
      this.buttons.add(new MenuButton("Аккаунты", () -> mc.setScreen(new AccountManagerScreen())));
      this.buttons.add(new MenuButton("Настройки", () -> mc.setScreen(new OptionsScreen(this, mc.options))));
      this.buttons.add(new MenuButton("Выход", () -> mc.scheduleStop()));
   }

   @Override
   protected void init() {
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
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();
      float anim = this.openAnimation.getValue();
      float time = (float) (System.currentTimeMillis() % 36000L) / 36000.0F * 2.0F * (float) Math.PI;
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();

      float panelX = (screenW - PANEL_WIDTH) / 2.0F;
      float panelY = (screenH - PANEL_HEIGHT) / 2.0F + 8.0F;

      CustomDrawContext draw = CustomDrawContext.of(context);
      this.renderBackground(draw, themeColor, screenW, screenH, panelX, panelY, time);
      this.renderTitle(draw, themeColor, screenW, panelY, anim);

      DrawUtil.drawShadow(draw.getMatrices(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 6.0F, BorderRadius.all(12.0F), new ColorRGBA(0, 0, 0).withAlpha(160.0F * anim));
      DrawUtil.drawRoundedRect(draw.getMatrices(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, BorderRadius.all(12.0F),
         new ColorRGBA(21, 21, 27).withAlpha(240.0F * anim), new ColorRGBA(18, 18, 24).withAlpha(240.0F * anim),
         new ColorRGBA(14, 14, 19).withAlpha(240.0F * anim), new ColorRGBA(23, 23, 31).withAlpha(240.0F * anim));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 1.0F, BorderRadius.all(12.0F), themeColor.withAlpha(70.0F * anim));

      float buttonsStart = panelY + 14.0F;
      for (int i = 0; i < this.buttons.size(); i++) {
         MenuButton button = this.buttons.get(i);
         float start = 0.07F * (float) i;
         float progress = MathHelper.clamp((anim - start) / (1.0F - start), 0.0F, 1.0F);
         float buttonY = buttonsStart + (float) i * (BUTTON_HEIGHT + BUTTON_GAP) + (1.0F - progress) * 16.0F;
         button.set(panelX + 14.0F, buttonY, PANEL_WIDTH - 28.0F, BUTTON_HEIGHT);
         button.update((float) mouseX, (float) mouseY);
         button.render(draw, themeColor, progress);
      }

      String current = "Аккаунт: " + AccountManager.INSTANCE.currentName();
      draw.drawText(Fonts.REGULAR.getFont(3.5F), current, panelX + (PANEL_WIDTH - Fonts.REGULAR.getFont(3.5F).width(current)) / 2.0F, panelY + PANEL_HEIGHT - 12.0F, themeColor.withAlpha(255.0F * anim));

      String version = "v0.1  ·  1.21.4";
      draw.drawText(Fonts.REGULAR.getFont(3.0F), version, screenW - Fonts.REGULAR.getFont(3.0F).width(version) - 8.0F, screenH - 12.0F, (new ColorRGBA(110, 110, 120)).withAlpha(255.0F * anim));
   }

   private void renderBackground(CustomDrawContext draw, ColorRGBA themeColor, float screenW, float screenH, float panelX, float panelY, float time) {
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, screenW, screenH, BorderRadius.all(0.0F),
         new ColorRGBA(11, 11, 15), new ColorRGBA(9, 9, 13), new ColorRGBA(6, 6, 10), new ColorRGBA(14, 14, 20));
      float driftX = MathHelper.sin(time) * 26.0F;
      float driftY = MathHelper.cos(time * 1.4F) * 18.0F;
      DrawUtil.drawGlow(draw.getMatrices(), panelX - 150.0F + driftX, panelY - 120.0F + driftY, 440.0F, 280.0F, 90);
      DrawUtil.drawGlow(draw.getMatrices(), panelX + PANEL_WIDTH - 130.0F - driftX, panelY + PANEL_HEIGHT - 40.0F - driftY, 380.0F, 220.0F, 70);
   }

   private void renderTitle(CustomDrawContext draw, ColorRGBA themeColor, float screenW, float panelY, float alpha) {
      String title = "JAVELIN";
      float size = 22.0F;
      float width = Fonts.ROUND_BOLD.getFont(size).width(title);
      float titleX = (screenW - width) / 2.0F;
      float titleY = panelY - 82.0F;
      DrawUtil.drawGlow(draw.getMatrices(), titleX - 34.0F, titleY - 16.0F, width + 68.0F, 48.0F, 16);
      draw.drawText(Fonts.ROUND_BOLD.getFont(size), title, titleX, titleY, (new ColorRGBA(240, 240, 246)).withAlpha(255.0F * alpha));
      String subtitle = RenamePasterClient.getClientName();
      draw.drawText(Fonts.COMFORTA_REGULAR.getFont(5.0F), subtitle, (screenW - Fonts.COMFORTA_REGULAR.getFont(5.0F).width(subtitle)) / 2.0F, panelY - 50.0F, themeColor.withAlpha(255.0F * alpha));
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for (MenuButton menuButton : this.buttons) {
         if (menuButton.isHovered((float) mouseX, (float) mouseY)) {
            menuButton.click();
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }
}
