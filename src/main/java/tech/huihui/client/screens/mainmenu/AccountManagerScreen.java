package tech.huihui.client.screens.mainmenu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.accounts.Account;
import tech.huihui.client.accounts.AccountManager;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class AccountManagerScreen extends Screen implements IClient {
   private static final float PANEL_WIDTH = 340.0F;
   private static final float PANEL_HEIGHT = 360.0F;
   private static final float ROW_HEIGHT = 34.0F;

   private final Animation openAnimation = new Animation(300L, Easing.EXPO_OUT);
   private final List<MenuButton> actionButtons = new ArrayList();
   private final MenuButton backButton = new MenuButton("Назад", () -> mc.setScreen(new MainMenuScreen()));
   private final MenuButton addButton = new MenuButton("Добавить", this::addAccount);
   private TextFieldWidget nameField;
   private float scrollOffset;
   private String statusText = "";
   private long statusTime;
   private int hoveredAccount = -1;

   public AccountManagerScreen() {
      super(Text.of("Account Manager"));
      this.actionButtons.add(new MenuButton("Выбрать", () -> this.selectAccount(this.hoveredAccount)));
      this.actionButtons.add(new MenuButton("Удалить", () -> this.deleteAccount(this.hoveredAccount)));
      this.actionButtons.add(new MenuButton("Рандом", this::randomAccount));
   }

   @Override
   protected void init() {
      this.openAnimation.animateTo(1.0F);
      this.nameField = this.addDrawableChild(new TextFieldWidget(mc.textRenderer, 0, 0, 130, 20, Text.empty()));
      this.nameField.setMaxLength(16);
      this.nameField.setDrawsBackground(false);
      this.nameField.setFocused(false);
      super.init();
   }

   @Override
   public void close() {
      mc.setScreen(new MainMenuScreen());
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.openAnimation.update();
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();
      float anim = this.openAnimation.getValue();
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();

      float panelX = (screenW - PANEL_WIDTH) / 2.0F;
      float panelY = (screenH - PANEL_HEIGHT) / 2.0F;

      CustomDrawContext draw = CustomDrawContext.of(context);
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, screenW, screenH, BorderRadius.all(0.0F), new ColorRGBA(7, 7, 9).withAlpha(190.0F * anim));
      DrawUtil.drawRoundedRect(draw.getMatrices(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, BorderRadius.all(12.0F), new ColorRGBA(15, 15, 17).withAlpha(235.0F * anim));
      DrawUtil.drawRoundedBorder(draw.getMatrices(), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 1.0F, BorderRadius.all(12.0F), themeColor.withAlpha(80.0F * anim));

      draw.drawText(Fonts.REGULAR.getFont(6.5F), "Аккаунт менеджер", panelX + 14.0F, panelY + 10.0F, (new ColorRGBA(235, 235, 235)).withAlpha(255.0F * anim));
      this.backButton.set(panelX + PANEL_WIDTH - 76.0F, panelY + 8.0F, 64.0F, 20.0F);
      this.backButton.update((float) mouseX, (float) mouseY);
      this.backButton.render(draw, themeColor, anim);

      String current = AccountManager.INSTANCE.currentName();
      draw.drawText(Fonts.REGULAR.getFont(4.5F), "Текущий: " + current, panelX + 14.0F, panelY + 26.0F, themeColor.withAlpha(255.0F * anim));
      if (!this.statusText.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(4.0F), this.statusText, panelX + 14.0F, panelY + 38.0F, (new ColorRGBA(120, 220, 130)).withAlpha(255.0F * anim));
      }

      float listY = panelY + 52.0F;
      float listBottom = panelY + PANEL_HEIGHT - 66.0F;
      this.hoveredAccount = this.hoveredIndex(mouseX, mouseY);
      draw.enableScissor((int) panelX, (int) listY - 2, (int) (panelX + PANEL_WIDTH), (int) listBottom + 2);
      List<Account> accounts = AccountManager.INSTANCE.getAccounts();
      float contentHeight = (float) accounts.size() * (ROW_HEIGHT + 6.0F);
      float maxScroll = Math.max(contentHeight - (listBottom - listY), 0.0F);
      this.scrollOffset = Math.max(-maxScroll, Math.min(0.0F, this.scrollOffset));
      if (accounts.isEmpty()) {
         draw.drawText(Fonts.REGULAR.getFont(4.5F), "Нет аккаунтов. Добавь оффлайн-никнейм ниже.", panelX + 14.0F, listY + 4.0F, (new ColorRGBA(110, 110, 120)).withAlpha(255.0F * anim));
      } else {
         for (int i = 0; i < accounts.size(); i++) {
            float rowY = listY + this.scrollOffset + (float) i * (ROW_HEIGHT + 6.0F);
            if (rowY + ROW_HEIGHT < listY || rowY > listBottom) {
               continue;
            }
            Account account = accounts.get(i);
            boolean isCurrent = account.getName().equals(current);
            boolean hovered = this.hoveredAccount == i;
            ColorRGBA bg = hovered ? themeColor.withAlpha(45.0F * anim) : (isCurrent ? themeColor.withAlpha(22.0F * anim) : new ColorRGBA(255, 255, 255).withAlpha(7.0F * anim));
            DrawUtil.drawRoundedRect(draw.getMatrices(), panelX + 8.0F, rowY, PANEL_WIDTH - 16.0F, ROW_HEIGHT, BorderRadius.all(8.0F), bg);
            DrawUtil.drawRoundedBorder(draw.getMatrices(), panelX + 8.0F, rowY, PANEL_WIDTH - 16.0F, ROW_HEIGHT, 1.0F, BorderRadius.all(8.0F), themeColor.withAlpha(isCurrent ? 130.0F * anim : 0.0F));
            draw.drawText(Fonts.REGULAR.getFont(5.0F), account.getName(), panelX + 18.0F, rowY + 10.0F, (new ColorRGBA(235, 235, 235)).withAlpha(255.0F * anim));
            draw.drawText(Fonts.REGULAR.getFont(3.5F), "Оффлайн", panelX + PANEL_WIDTH - 18.0F - Fonts.REGULAR.getFont(3.5F).width("Оффлайн"), rowY + 11.0F, (new ColorRGBA(110, 110, 120)).withAlpha(255.0F * anim));
         }
      }
      draw.disableScissor();

      float actionsY = panelY + PANEL_HEIGHT - 56.0F;
      float buttonWidth = (PANEL_WIDTH - 24.0F - 16.0F) / 3.0F;
      for (int i = 0; i < this.actionButtons.size(); i++) {
         MenuButton button = this.actionButtons.get(i);
         button.set(panelX + 12.0F + (float) i * (buttonWidth + 8.0F), actionsY, buttonWidth, 20.0F);
         button.update((float) mouseX, (float) mouseY);
         button.render(draw, themeColor, anim);
      }

      float fieldY = panelY + PANEL_HEIGHT - 30.0F;
      this.nameField.setX((int) (panelX + 12.0F));
      this.nameField.setY((int) fieldY);
      this.nameField.setWidth((int) (PANEL_WIDTH - 24.0F - 90.0F - 6.0F));
      this.nameField.setHeight(20);
      DrawUtil.drawRoundedRect(draw.getMatrices(), panelX + 12.0F, fieldY, this.nameField.getWidth(), 20.0F, BorderRadius.all(7.0F), new ColorRGBA(255, 255, 255).withAlpha(9.0F * anim));
      if (this.nameField.getText().isEmpty() && !this.nameField.isFocused()) {
         draw.drawText(Fonts.REGULAR.getFont(4.0F), "Никнейм...", panelX + 16.0F, fieldY + 7.0F, (new ColorRGBA(110, 110, 120)).withAlpha(255.0F * anim));
      }
      this.nameField.render(context, mouseX, mouseY, delta);

      this.addButton.set(panelX + PANEL_WIDTH - 102.0F, fieldY, 90.0F, 20.0F);
      this.addButton.update((float) mouseX, (float) mouseY);
      this.addButton.render(draw, themeColor, anim);

      if (this.statusText.isEmpty() || System.currentTimeMillis() - this.statusTime > 3000L) {
         this.statusText = "";
      }
   }

   private int hoveredIndex(int mouseX, int mouseY) {
      List<Account> accounts = AccountManager.INSTANCE.getAccounts();
      if (accounts.isEmpty()) {
         return -1;
      }
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();
      float panelX = (screenW - PANEL_WIDTH) / 2.0F;
      float panelY = (screenH - PANEL_HEIGHT) / 2.0F;
      float listY = panelY + 52.0F;
      float listBottom = panelY + PANEL_HEIGHT - 66.0F;
      if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH || mouseY < listY || mouseY > listBottom) {
         return -1;
      }
      int index = (int) ((mouseY - listY - this.scrollOffset) / (ROW_HEIGHT + 6.0F));
      return index >= 0 && index < accounts.size() ? index : -1;
   }

   private void selectAccount(int index) {
      if (index < 0) {
         return;
      }
      Account account = AccountManager.INSTANCE.getAccounts().get(index);
      AccountManager.INSTANCE.switchTo(account);
      this.statusText = "Выбран аккаунт: " + account.getName();
      this.statusTime = System.currentTimeMillis();
   }

   private void deleteAccount(int index) {
      if (index < 0) {
         return;
      }
      Account account = AccountManager.INSTANCE.getAccounts().get(index);
      AccountManager.INSTANCE.remove(account);
      this.statusText = "Удалён: " + account.getName();
      this.statusTime = System.currentTimeMillis();
   }

   private void randomAccount() {
      List<Account> accounts = AccountManager.INSTANCE.getAccounts();
      if (accounts.isEmpty()) {
         this.statusText = "Нет аккаунтов";
         this.statusTime = System.currentTimeMillis();
         return;
      }
      Account current = AccountManager.INSTANCE.findByName(AccountManager.INSTANCE.currentName());
      Account pick;
      if (accounts.size() > 1 && current != null) {
         do {
            pick = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
         } while (pick == current);
      } else {
         pick = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size()));
      }
      AccountManager.INSTANCE.switchTo(pick);
      this.statusText = "Случайный: " + pick.getName();
      this.statusTime = System.currentTimeMillis();
   }

   private void addAccount() {
      String name = this.nameField.getText().trim();
      if (name.isEmpty()) {
         this.statusText = "Введи никнейм";
         this.statusTime = System.currentTimeMillis();
         return;
      }
      AccountManager.INSTANCE.add(name);
      Account account = AccountManager.INSTANCE.findByName(name);
      if (account != null) {
         AccountManager.INSTANCE.switchTo(account);
      }
      this.nameField.setText("");
      this.nameField.setFocused(false);
      this.statusText = "Добавлен: " + name;
      this.statusTime = System.currentTimeMillis();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float mx = (float) mouseX;
      float my = (float) mouseY;
      if (this.backButton.isHovered(mx, my)) {
         this.backButton.click();
         return true;
      }
      for (MenuButton menuButton : this.actionButtons) {
         if (menuButton.isHovered(mx, my)) {
            menuButton.click();
            return true;
         }
      }
      if (this.addButton.isHovered(mx, my)) {
         this.addButton.click();
         return true;
      }
      int index = this.hoveredIndex((int) mx, (int) my);
      if (index >= 0) {
         if (button == 1) {
            this.deleteAccount(index);
         } else if (button == 0) {
            this.selectAccount(index);
         }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (!AccountManager.INSTANCE.getAccounts().isEmpty()) {
         this.scrollOffset += (float) verticalAmount * 12.0F;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }
}
