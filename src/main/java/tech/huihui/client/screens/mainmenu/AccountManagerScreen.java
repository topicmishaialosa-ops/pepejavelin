package tech.huihui.client.screens.mainmenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.accounts.Account;
import tech.huihui.client.accounts.AccountManager;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class AccountManagerScreen extends Screen implements IClient {
   private final AnimationUtilState openState = new AnimationUtilState();
   private final List<Row> rows = new ArrayList<>();
   private final List<Account> pendingRemoval = new ArrayList<>();
   private final ModernTextField nameField;
   private Row hoveredRow;
   private Row dragRow;
   private float dragOffsetY;
   private boolean dragging;
   private Account lastSelected;
   private float scrollTarget;
   private float scrollOffset;

   public AccountManagerScreen() {
      super(Text.empty());
      this.nameField = new ModernTextField("Никнейм");
      AccountManager.INSTANCE.getAccounts().forEach(account -> this.rows.add(new Row(account)));
   }

   @Override
   protected void init() {
      super.init();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.openState.expand(mc.currentScreen instanceof AccountManagerScreen);
      this.openState.update(0.0F, 1.0F, 0.15F, delta);
      float fMin = Math.min(1.0F, this.openState.getValue() / 0.9F);
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();

      MainMenuScreen.renderParallaxBackground(context, screenW, screenH, mouseX, mouseY, 1.05F + (easeBack(fMin) * 0.2F));
      DrawUtil.drawBlur(context.getMatrices(), 0.0F, 0.0F, screenW, screenH, 8.0F, BorderRadius.ZERO, new ColorRGBA(255, 255, 255, (int) (90.0F * fMin)));

      float panelX = (screenW - 190.0F) / 2.0F;
      float panelY = (screenH - 250.0F) / 2.0F;
      float scale = easeBack(fMin) * 0.15F + 0.85F;
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(screenW * 0.5F, screenH * 0.5F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-screenW * 0.5F, -screenH * 0.5F, 0.0F);

      this.renderHeader(context, screenW, panelX, panelY, fMin);
      this.renderList(context, screenW, panelX, panelY, mouseX, mouseY, fMin);
      this.renderFooter(context, panelX, panelY, mouseX, mouseY, fMin);

      matrices.pop();
   }

   private void renderHeader(DrawContext context, float screenW, float panelX, float panelY, float open) {
      CustomDrawContext draw = CustomDrawContext.of(context);
      Account selected = AccountManager.INSTANCE.getSelected();
      String title = "Менеджер Аккаунтов";
      float titleW = Fonts.ROUND_BOLD.getFont(8.0F).width(title);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA c1 = theme.getColor().withAlpha(255.0F * open);
      draw.drawText(Fonts.ROUND_BOLD.getFont(8.0F), title, (screenW - titleW) / 2.0F, panelY - 38.0F, c1);
      String info = (selected != null ? selected.getName() : "Не выбран") + "  |  " + AccountManager.INSTANCE.getAccounts().size() + " аккаунтов";
      float infoW = Fonts.REGULAR.getFont(5.0F).width(info);
      draw.drawText(Fonts.REGULAR.getFont(5.0F), info, (screenW - infoW) / 2.0F, panelY - 20.0F, new ColorRGBA(190, 190, 205).withAlpha(230.0F * open));
      DrawUtil.drawRoundedRect(context.getMatrices(), panelX, panelY, 190.0F, 250.0F, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(198.0F * open));
      DrawUtil.drawRoundedBorder(context.getMatrices(), panelX, panelY, 190.0F, 250.0F, 0.5F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha(15.0F * open));
   }

   private void renderList(DrawContext context, float screenW, float panelX, float panelY, int mx, int my, float open) {
      MatrixStack matrices = context.getMatrices();
      CustomDrawContext draw = CustomDrawContext.of(context);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA accent = theme.getColor().withAlpha(255.0F * open);
      Account selected = AccountManager.INSTANCE.getSelected();
      float listTop = panelY + 10.0F;
      float listBottom = panelY + 218.0F;
      float listHeight = listBottom - listTop;
      float overflow = Math.min(0.0F, listHeight - (this.rows.size() * 29.0F));
      this.scrollTarget = Math.max(overflow, Math.min(0.0F, this.scrollTarget));
      this.scrollOffset = MathUtil.interpolate(this.scrollOffset, this.scrollTarget, Math.min(1.0F, 0.4F));
      float offset = this.scrollOffset;
      boolean drag = this.dragRow != null && this.dragging;
      float baseY = listTop + offset;
      float dragSlot = (my - this.dragOffsetY) - baseY;
      List<Row> visual = this.rows.stream().sorted(Comparator.comparing(row -> !row.account.isFavorited())).collect(Collectors.toCollection(ArrayList::new));
      if (drag) {
         this.reorder(visual, dragSlot);
      }
      this.hoveredRow = null;
      int index = 0;

      draw.enableScissor((int) panelX, (int) listTop - 2, (int) (panelX + 190.0F), (int) listBottom + 1);
      for (Row row : visual) {
         float targetSlot = row.expired ? row.deathSlot : index * 29.0F;
         if (row != this.dragRow || !drag) {
            row.render(context, accent, panelX + 1.0F, baseY, targetSlot, listTop, listBottom, mx, my, open);
         }
         if (!row.expired) {
            index++;
         }
      }
      if (drag) {
         this.dragRow.deathSlot = dragSlot;
         this.dragRow.render(context, accent, panelX + 1.0F, baseY, dragSlot, listTop, listBottom, mx, my, open);
      }
      draw.disableScissor();

      this.rows.removeIf(row -> row.expired && row.alpha[3] < 0.01F);
      if (!this.pendingRemoval.isEmpty()) {
         for (Account account : this.pendingRemoval) {
            AccountManager.INSTANCE.getAccounts().remove(account);
         }
         this.pendingRemoval.clear();
         AccountManager.INSTANCE.save();
      }
      if (selected != this.lastSelected) {
         this.lastSelected = selected;
         if (selected != null) {
            int selectedIndex = 0;
            for (Row row : this.rows) {
               if (row.account == selected) {
                  break;
               }
               selectedIndex++;
            }
            if (selectedIndex < this.rows.size()) {
               this.scrollTarget = Math.max(overflow, Math.min(0.0F, -((selectedIndex * 29.0F) - ((listHeight - 29.0F) / 2.0F))));
            }
         }
      }
      float content = Math.max(listHeight, this.rows.size() * 29.0F);
      float thumb = (listHeight * listHeight) / content;
      DrawUtil.drawRoundedRect(matrices, panelX + 182.5F, listTop, 1.5F, listHeight, BorderRadius.all(0.75F), new ColorRGBA(255, 255, 255).withAlpha(20.0F * open));
      DrawUtil.drawRoundedRect(matrices, panelX + 182.5F, listTop - ((offset / Math.max(1.0F, content - listHeight)) * (listHeight - thumb)), 1.5F, thumb, BorderRadius.all(0.75F), accent);
   }

   private void renderFooter(DrawContext context, float panelX, float panelY, int mx, int my, float open) {
      MatrixStack matrices = context.getMatrices();
      CustomDrawContext draw = CustomDrawContext.of(context);
      float fieldY = panelY + 224.0F;
      float fieldWidth = 90.0F;
      float right = panelX + 11.0F + fieldWidth;

      this.nameField.setX(panelX + 7.0F);
      this.nameField.setY(fieldY);
      this.nameField.setWidth(fieldWidth);
      this.nameField.setHeight(18.0F);
      this.nameField.render(context, mx, my, open);

      this.renderButton(matrices, draw, right + 0.5F, fieldY, 18.0F, 18.0F, new BorderRadius(1.0F, 5.0F, 1.0F, 5.0F), null, "M", new ColorRGBA(222, 222, 222), open, mx, my);
      this.renderButton(matrices, draw, right + 27.0F, fieldY, 56.0F, 18.0F, BorderRadius.all(6.0F), "Случайный", null, new ColorRGBA(222, 222, 222), open, mx, my);
      this.renderButton(matrices, draw, panelX + 15.0F, panelY + 257.0F, 160.0F, 20.0F, BorderRadius.all(6.0F), "Удалить все аккаунты", null, new ColorRGBA(220, 80, 80), open, mx, my);
   }

   private void renderButton(MatrixStack matrices, CustomDrawContext draw, float x, float y, float width, float height, BorderRadius radius, String text, String icon, ColorRGBA content, float open, int mx, int my) {
      boolean hover = MathUtil.isHovered(mx, my, x, y, width, height);
      DrawUtil.drawRoundedRect(matrices, x, y, width, height, radius, new ColorRGBA(255, 255, 255).withAlpha((hover ? 14 : 6) * open));
      DrawUtil.drawRoundedBorder(matrices, x, y, width, height, 1.0F, radius, content.withAlpha((hover ? 70 : 38) * open));
      float iconWidth = icon != null ? Fonts.ICONS.getFont(5.5F).width(icon) + (text != null ? 3.0F : 0.0F) : 0.0F;
      float textWidth = text != null ? Fonts.REGULAR.getFont(5.0F).width(text) : 0.0F;
      float startX = x + ((width - iconWidth - textWidth) / 2.0F);
      if (icon != null) {
         draw.drawText(Fonts.ICONS.getFont(5.5F), icon, startX, y + ((height - Fonts.ICONS.getFont(5.5F).height()) / 2.0F), content.withAlpha(255.0F * open));
      }
      if (text != null) {
         draw.drawText(Fonts.REGULAR.getFont(5.0F), text, startX + (icon != null ? 2.0F : 0.0F), y + ((height - Fonts.REGULAR.getFont(5.0F).height()) / 2.0F), content.withAlpha(255.0F * open));
      }
   }

   @Override
   public boolean mouseClicked(double rawX, double rawY, int button) {
      this.nameField.mouseClicked(rawX, rawY, button);
      float fieldX0 = this.nameField.getX();
      float fieldY0 = this.nameField.getY();
      float fieldWidth = this.nameField.getWidth();
      if (!MathUtil.isHovered(rawX, rawY, fieldX0, fieldY0, fieldWidth, 18.0F)) {
         this.nameField.setFocused(false);
      }
      float fieldX = fieldX0 + fieldWidth;
      if (MathUtil.isHovered(rawX, rawY, 5.0F + fieldX, fieldY0, 18.0F, 18.0F)) {
         this.addAccount(this.nameField.getText());
         return true;
      }
      if (MathUtil.isHovered(rawX, rawY, 27.0F + fieldX, fieldY0, 56.0F, 18.0F)) {
         this.addAccount(randomName());
         return true;
      }
      float panelX = (mw.getScaledWidth() - 190.0F) / 2.0F;
      float panelY = (mw.getScaledHeight() - 250.0F) / 2.0F;
      if (MathUtil.isHovered(rawX, rawY, panelX + 15.0F, panelY + 257.0F, 160.0F, 20.0F)) {
         this.deleteAll();
         return true;
      }
      if (this.hoveredRow == null) {
         return super.mouseClicked(rawX, rawY, button);
      }
      if (button == 1) {
         this.removeRow(this.hoveredRow);
         return true;
      }
      this.dragRow = this.hoveredRow;
      this.dragOffsetY = (float) rawY - this.hoveredRow.y;
      this.dragging = false;
      return true;
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double deltaX, double deltaY) {
      if (this.dragRow == null) {
         return super.mouseDragged(mx, my, button, deltaX, deltaY);
      }
      if (Math.abs(my - this.dragOffsetY - this.dragRow.y) <= 8.0D) {
         return true;
      }
      this.dragging = true;
      return true;
   }

   @Override
   public boolean mouseReleased(double rawX, double rawY, int button) {
      if (this.dragRow == null) {
         return super.mouseReleased(rawX, rawY, button);
      }
      if (!this.dragging) {
         if (this.dragRow.starHovered) {
            this.dragRow.account.setFavorited(!this.dragRow.account.isFavorited());
            AccountManager.INSTANCE.save();
         } else {
            AccountManager.INSTANCE.select(this.dragRow.account);
         }
      } else {
         this.commitOrder();
      }
      this.dragRow = null;
      this.dragging = false;
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scrollTarget += (float) verticalAmount * 29.0F;
      return true;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.nameField.isFocused()) {
         this.nameField.charTyped(chr, modifiers);
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((modifiers & 2) != 0 && keyCode == 86) {
         String clip = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
         if (clip != null) {
            String name = clip.replaceAll("[^a-zA-Z0-9_]", "");
            this.addAccount(name.substring(0, Math.min(16, name.length())));
         }
         return true;
      }
      if (!this.nameField.isFocused()) {
         if (keyCode != 256) {
            return super.keyPressed(keyCode, scanCode, modifiers);
         }
      }
      if (keyCode == 256) {
         mc.setScreen(new MainMenuScreen());
         return true;
      }
      if (keyCode == 257) {
         this.addAccount(this.nameField.getText());
         return true;
      }
      return this.nameField.keyPressed(keyCode, scanCode, modifiers);
   }

   private void reorder(List<Row> visual, float draggedY) {
      int from = visual.indexOf(this.dragRow);
      int favorites = (int) visual.stream().filter(row -> row.account.isFavorited()).count();
      int lo = this.dragRow.account.isFavorited() ? 0 : favorites;
      int hi = this.dragRow.account.isFavorited() ? favorites - 1 : visual.size() - 1;
      int to = Math.max(lo, Math.min(hi, Math.round(draggedY / 29.0F)));
      if (from < 0 || from == to) {
         return;
      }
      visual.remove(from);
      visual.add(to, this.dragRow);
      this.rows.clear();
      this.rows.addAll(visual);
   }

   private void commitOrder() {
      List<Account> ordered = this.rows.stream().map(row -> row.account).filter(account -> AccountManager.INSTANCE.getAccounts().contains(account)).collect(Collectors.toCollection(ArrayList::new));
      AccountManager.INSTANCE.reorder(ordered);
   }

   private void addAccount(String raw) {
      String name = raw == null ? "" : raw.trim();
      if (name.isEmpty() || AccountManager.INSTANCE.findByName(name) != null) {
         return;
      }
      Account account = new Account(name, AccountManager.INSTANCE.getAccounts().isEmpty(), false);
      AccountManager.INSTANCE.getAccounts().add(account);
      AccountManager.INSTANCE.select(account);
      this.rows.add(new Row(account));
      this.nameField.setText("");
      AccountManager.INSTANCE.save();
   }

   private void removeRow(Row row) {
      row.expired = true;
      this.pendingRemoval.add(row.account);
   }

   private void deleteAll() {
      this.rows.forEach(row -> row.expired = true);
      this.pendingRemoval.addAll(AccountManager.INSTANCE.getAccounts());
      AccountManager.INSTANCE.clear();
   }

   private static String randomName() {
      StringBuilder name = new StringBuilder();
      int syllables = 2 + (int) (Math.random() * 3.0D);
      for (int i = 0; i < syllables; i++) {
         char c = "bcdfghjklmnpqrstvwz".charAt((int) (Math.random() * "bcdfghjklmnpqrstvwz".length()));
         name.append(c);
         if (Math.random() < 0.12D) {
            name.append(c);
         }
         char v = "aeiouy".charAt((int) (Math.random() * "aeiouy".length()));
         name.append(v);
         if (Math.random() < 0.1D) {
            name.append(v);
         }
      }
      if (Math.random() < 0.3D) {
         name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
      }
      if (Math.random() < 0.15D) {
         name.append('_');
      }
      if (Math.random() < 0.25D) {
         int digits = 1 + (int) (Math.random() * 3.0D);
         for (int i = 0; i < digits; i++) {
            name.append((char) (48 + (int) (Math.random() * 10.0D)));
         }
      }
      if (name.length() < 5) {
         return randomName();
      }
      return name.length() > 16 ? name.substring(0, 16) : name.toString();
   }

   private static float easeBack(float value) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + (c3 * (value - 1.0F) * (value - 1.0F) * (value - 1.0F)) + (c1 * (value - 1.0F) * (value - 1.0F));
   }

   class Row {
      final Account account;
      final float[] alpha = new float[4];
      float deathSlot = Float.NaN;
      float y;
      boolean starHovered;
      boolean expired;

      Row(Account account) {
         this.account = account;
      }

      void render(DrawContext context, ColorRGBA accent, float panelX, float baseY, float targetSlot, float listTop, float listBottom, int mx, int my, float open) {
         MatrixStack matrices = context.getMatrices();
         CustomDrawContext draw = CustomDrawContext.of(context);
         this.deathSlot = Float.isNaN(this.deathSlot) ? targetSlot : MathUtil.interpolate(this.deathSlot, targetSlot, Math.min(1.0F, 0.9F));
         this.y = baseY + this.deathSlot;
         boolean over = !this.expired && my > listTop && my < listBottom && MathUtil.isHovered(mx, my, panelX + 7.0F, this.y, 168.0F, 25.0F);
         this.starHovered = over && mx > panelX + 156.0F;
         if (over) {
            hoveredRow = this;
         }
         float[] target = new float[]{over ? 1.0F : 0.0F, this.account.isSelected() ? 1.0F : 0.0F, this.account.isFavorited() ? 1.0F : 0.0F, this.expired ? 0.0F : 1.0F};
         for (int i = 0; i < 4; i++) {
            this.alpha[i] = MathUtil.interpolate(this.alpha[i], target[i], Math.min(1.0F, 0.7F));
         }
         float hover = this.alpha[0];
         float select = this.alpha[1];
         float fav = this.alpha[2];
         float a = open * this.alpha[3];
         if (this.y + 25.0F < listTop - 2.0F || this.y > listBottom + 2.0F) {
            return;
         }
         if (select > 0.01F) {
            DrawUtil.drawRoundedRect(matrices, panelX + 7.0F, this.y, 168.0F, 25.0F, BorderRadius.all(6.0F), accent.withAlpha(0.1F * select * a));
         } else if (hover > 0.01F) {
            DrawUtil.drawRoundedRect(matrices, panelX + 7.0F, this.y, 168.0F, 25.0F, BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255).withAlpha(6.0F * hover * a));
         }
         ColorRGBA border = ColorRGBA.lerp(new ColorRGBA(255, 255, 255).withAlpha(8.0F * a), new ColorRGBA(255, 205, 60).withAlpha(30.0F * a), fav);
         DrawUtil.drawRoundedBorder(matrices, panelX + 7.0F, this.y, 168.0F, 25.0F, 0.5F, BorderRadius.all(6.0F), border);
         Identifier skin = DefaultSkinHelper.getSkinTextures(this.account.getUuid()).texture();
         DrawUtil.drawPlayerHeadWithRoundedShader(matrices, skin, panelX + 11.5F, this.y + 4.0F, 16.5F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, (int) (255.0F * a)));
         draw.drawText(Fonts.REGULAR.getFont(6.0F), this.account.getName(), panelX + 36.0F, this.y + ((25.0F - Fonts.REGULAR.getFont(6.0F).height()) / 2.0F), new ColorRGBA(255, 255, 255, (int) (255.0F * a)));
         if (fav > 0.01F || hover > 0.01F) {
            ColorRGBA starColor = ColorRGBA.lerp(new ColorRGBA(255, 255, 255).withAlpha((this.starHovered ? 200 : 45) * hover * a), new ColorRGBA(255, 205, 60, (int) (255.0F * a)), fav);
            this.drawStar(matrices, panelX + 163.0F, this.y + 12.5F, 4.5F, starColor);
         }
      }

      private void drawStar(MatrixStack matrices, float cx, float cy, float radius, ColorRGBA color) {
         int points = 5;
         for (int i = 0; i < points; i++) {
            float outerA1 = (float) Math.toRadians(-90.0D + (i * 360.0D / points));
            float outerA2 = (float) Math.toRadians(-90.0D + ((i + 1) * 360.0D / points));
            float innerA = (float) Math.toRadians(-90.0D + ((i + 0.5D) * 360.0D / points));
            net.minecraft.util.math.Vec2f outer1 = new net.minecraft.util.math.Vec2f(cx + (float) Math.cos(outerA1) * radius, cy + (float) Math.sin(outerA1) * radius);
            net.minecraft.util.math.Vec2f inner = new net.minecraft.util.math.Vec2f(cx + (float) Math.cos(innerA) * radius * 0.45F, cy + (float) Math.sin(innerA) * radius * 0.45F);
            net.minecraft.util.math.Vec2f outer2 = new net.minecraft.util.math.Vec2f(cx + (float) Math.cos(outerA2) * radius, cy + (float) Math.sin(outerA2) * radius);
            DrawUtil.drawLine(matrices, outer1, inner, color);
            DrawUtil.drawLine(matrices, inner, outer2, color);
         }
      }
   }
}