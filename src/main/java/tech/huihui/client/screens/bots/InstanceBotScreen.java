package tech.huihui.client.screens.bots;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import tech.huihui.HuihuiClient;
import tech.huihui.base.bots.InstanceBot;
import tech.huihui.base.bots.InstanceBotManager;
import tech.huihui.base.bots.net.BotConnection;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class InstanceBotScreen extends Screen {
   private static final float RADIUS = 10.0F;
   private static final int PANEL_WIDTH = 620;

   private static final ColorRGBA BG_TOP = new ColorRGBA(9, 9, 13, 235);
   private static final ColorRGBA BG_BOTTOM = new ColorRGBA(18, 18, 26, 245);
   private static final ColorRGBA PANEL = new ColorRGBA(16, 16, 21, 238);
   private static final ColorRGBA PANEL_BORDER = new ColorRGBA(70, 70, 95, 120);
   private static final ColorRGBA ROW = new ColorRGBA(30, 30, 39, 235);
   private static final ColorRGBA ROW_HOVER = new ColorRGBA(46, 46, 60, 235);
   private static final ColorRGBA ROW_BORDER = new ColorRGBA(70, 70, 90, 80);
   private static final ColorRGBA BUTTON = new ColorRGBA(44, 44, 56, 240);
   private static final ColorRGBA BUTTON_HOVER = new ColorRGBA(62, 62, 80, 240);
   private static final ColorRGBA INPUT = new ColorRGBA(24, 24, 31, 240);
   private static final ColorRGBA INPUT_FOCUS = new ColorRGBA(30, 33, 44, 255);
   private static final ColorRGBA INPUT_BORDER = new ColorRGBA(70, 70, 90, 120);
   private static final ColorRGBA GREEN = new ColorRGBA(102, 217, 102, 255);
   private static final ColorRGBA RED = new ColorRGBA(214, 82, 70, 255);
   private static final ColorRGBA ORANGE = new ColorRGBA(255, 180, 100, 255);
   private static final ColorRGBA GRAY = new ColorRGBA(140, 140, 165, 200);
   private static final ColorRGBA GRAY_DIM = new ColorRGBA(110, 110, 135, 170);
   private static final ColorRGBA WHITE = new ColorRGBA(230, 230, 245, 255);

   private static final float TITLE_SIZE = 12.0F;
   private static final float UI_SIZE = 8.5F;
   private static final float BODY_SIZE = 8.0F;
   private static final float HINT_SIZE = 7.0F;

   private final StringBuilder nameBuffer = new StringBuilder();
   private final StringBuilder serverBuffer = new StringBuilder();
   private final StringBuilder chatBuffer = new StringBuilder();
   private boolean skipNextChar;
   private int focusedField = -1;
   private int scroll;
   private String notice = "";
   private long noticeUntil = 0L;

   public InstanceBotScreen() {
      super(Text.literal("Инстанс-боты"));
      this.serverBuffer.append(InstanceBotManager.INSTANCE.getDefaultServer());
   }

   private int panelX() {
      return (this.width - PANEL_WIDTH) / 2;
   }

   private boolean inside(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private void rr(CustomDrawContext draw, float x, float y, float w, float h, ColorRGBA color) {
      draw.drawRoundedRect(x, y, w, h, BorderRadius.all(RADIUS), color);
   }

   private void rr(CustomDrawContext draw, float x, float y, float w, float h, BorderRadius radius, ColorRGBA color) {
      draw.drawRoundedRect(x, y, w, h, radius, color);
   }

   private void border(CustomDrawContext draw, float x, float y, float w, float h, BorderRadius radius, ColorRGBA color) {
      draw.drawRoundedBorder(x, y, w, h, 1.0F, radius, color);
   }

   private void text(CustomDrawContext draw, MsdfFont font, float size, String s, float x, float y, ColorRGBA color) {
      String value = s == null ? "" : s;
      draw.drawText(font.getFont(size), value, x, y, color);
   }

   private void centerText(CustomDrawContext draw, MsdfFont font, float size, String s, float x, float y, float w, ColorRGBA color) {
      float tw = font.getWidth(s, size);
      draw.drawText(font.getFont(size), s, x + (w - tw) / 2.0F, y, color);
   }

   private String trimWidth(MsdfFont font, String s, float maxW, float size) {
      if (s == null || s.isEmpty() || font.getWidth(s, size) <= maxW) {
         return s == null ? "" : s;
      }
      int end = s.length();
      while (end > 0 && font.getWidth(s.substring(0, end), size) > maxW) {
         end--;
      }
      return s.substring(0, end);
   }

   private ColorRGBA accent() {
      var theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      return theme == null ? new ColorRGBA(120, 140, 255, 255) : theme.getColor().withAlpha(255);
   }

   private void setNotice(String message) {
      this.notice = message;
      this.noticeUntil = System.currentTimeMillis() + 2500L;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      CustomDrawContext draw = CustomDrawContext.of(context);

      String focused = InstanceBotManager.INSTANCE.getFocusedName();
      if (focused != null) {
         this.renderFocus(draw, focused);
      } else {
         DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, (float) this.width, (float) this.height,
            BorderRadius.ZERO, BG_TOP, BG_TOP, BG_BOTTOM, BG_BOTTOM);
         this.renderList(draw, context, mouseX, mouseY);
      }
   }

   private void renderFocus(CustomDrawContext draw, String botName) {
      ColorRGBA accent = this.accent();
      DrawUtil.drawRoundedRect(draw.getMatrices(), 0, 0, this.width, 2, BorderRadius.ZERO, accent);

      BotConnection connection = InstanceBotManager.INSTANCE.bot(botName);
      String coords = "x " + fmt(connection == null ? 0.0 : connection.getX()) + "  y "
         + fmt(connection == null ? 0.0 : connection.getY()) + "  z "
         + fmt(connection == null ? 0.0 : connection.getZ());

      float chipW = 300.0F;
      rr(draw, 12, 10, chipW, 34, BorderRadius.all(8.0F), new ColorRGBA(10, 10, 15, 180));
      border(draw, 12, 10, chipW, 34, BorderRadius.all(8.0F), PANEL_BORDER);
      text(draw, Fonts.SEMIBOLD, UI_SIZE, "Фокус: " + botName, 20, 15, WHITE);
      text(draw, Fonts.REGULAR, HINT_SIZE, coords, 20, 27, GRAY);

      List<String> lines = connection == null ? List.of() : connection.getChatLines();
      int show = Math.min(6, lines.size());
      if (show > 0) {
         float y = this.height - 16 - show * 13.0F;
         for (int i = lines.size() - show; i < lines.size(); i++) {
            String line = lines.get(i);
            line = line.replace("\u00a7", "");
            String shown = trimWidth(Fonts.REGULAR, line, this.width - 60, HINT_SIZE);
            text(draw, Fonts.REGULAR, HINT_SIZE, shown, 14, y, GRAY);
            y += 13.0F;
         }
      }

      if (InstanceBotManager.INSTANCE.isChatOpen()) {
         float inputW = Math.min(560.0F, this.width - 40.0F);
         float ix = (this.width - inputW) / 2.0F;
         float iy = this.height - 46;
         BorderRadius boxRadius = BorderRadius.all(8.0F);
         rr(draw, ix, iy, inputW, 28, boxRadius, new ColorRGBA(12, 12, 18, 220));
         border(draw, ix, iy, inputW, 28, boxRadius, accent.withAlpha(180));
         String value = this.chatBuffer.toString();
         if (value.isEmpty()) {
            text(draw, Fonts.REGULAR, BODY_SIZE, "Сообщение боту...", ix + 10, iy + 9, GRAY);
         } else {
            String caret = System.currentTimeMillis() % 900L < 450L ? "|" : "";
            String shown = trimWidth(Fonts.REGULAR, value + caret, inputW - 20, BODY_SIZE);
            text(draw, Fonts.REGULAR, BODY_SIZE, shown, ix + 10, iy + 9, WHITE);
         }
      } else {
         String hint = "WASD — ходьба · мышь — обзор · ЛКМ/ПКМ — атака/использовать · T — чат · ESC — выход";
         float hintW = Fonts.REGULAR.getWidth(hint, HINT_SIZE) + 20;
         rr(draw, this.width - 20 - hintW, this.height - 30, hintW, 20, BorderRadius.all(8.0F), new ColorRGBA(10, 10, 15, 190));
         centerText(draw, Fonts.REGULAR, HINT_SIZE, hint, this.width - 20 - hintW, this.height - 25, hintW, GRAY);
      }
   }

   private String fmt(double value) {
      return String.format(java.util.Locale.US, "%.1f", value);
   }

   private void renderList(CustomDrawContext draw, DrawContext context, int mouseX, int mouseY) {
      int px = this.panelX();
      BorderRadius panelRadius = BorderRadius.all(RADIUS);
      rr(draw, px - 10, 14, PANEL_WIDTH + 20, this.height - 24, panelRadius, PANEL);
      border(draw, px - 10, 14, PANEL_WIDTH + 20, this.height - 24, panelRadius, PANEL_BORDER);

      ColorRGBA accent = this.accent();
      rr(draw, px + 8, 30, 3, 18, BorderRadius.all(1.5F), accent);
      text(draw, Fonts.SEMIBOLD, TITLE_SIZE, "Инстанс-боты", px + 16, 28, WHITE);

      float fieldW = 240.0F;
      float fieldGap = 8.0F;
      this.drawInput(draw, px + 8, 50, fieldW, "Ник бота", this.nameBuffer, this.focusedField == 0, accent);
      this.drawInput(draw, px + 8 + fieldW + fieldGap, 50, fieldW, "Сервер ip:port", this.serverBuffer, this.focusedField == 1, accent);
      float addW = PANEL_WIDTH - 16 - 2.0F * (fieldW + fieldGap);
      float bx = px + 8 + 2.0F * (fieldW + fieldGap);
      boolean addHover = this.inside(mouseX, mouseY, bx, 50, addW, 24);
      rr(draw, bx, 50, addW, 24, BorderRadius.all(7.0F), addHover ? accent.withAlpha(220) : accent.withAlpha(170));
      centerText(draw, Fonts.SEMIBOLD, BODY_SIZE, "Добавить", bx, 56, addW, WHITE);

      text(draw, Fonts.REGULAR, HINT_SIZE,
         "Боты подключаются протоколом прямо из этого клиента — отдельных процессов и окон больше нет.", px + 8, 84, GRAY);
      text(draw, Fonts.REGULAR, HINT_SIZE,
         "Фокус: камера следует за ботом в этом же мире, управление — как в обычной игре. Выход — ESC.", px + 8, 96, GRAY);

      this.drawDivider(draw, px, 118, accent);
      this.drawHeader(draw, px);
      this.drawRows(draw, px, mouseX, mouseY, accent);
      this.drawFooter(draw, px, mouseX, mouseY, accent);

      if (!this.notice.isEmpty() && System.currentTimeMillis() < this.noticeUntil) {
         text(draw, Fonts.SEMIBOLD, BODY_SIZE, this.notice, px + 8, this.height - 60, ORANGE);
      } else {
         this.notice = "";
      }
   }

   private void drawDivider(CustomDrawContext draw, int px, float y, ColorRGBA accent) {
      DrawUtil.drawRoundedRect(draw.getMatrices(), px + 8, y, PANEL_WIDTH - 16, 1.0F, BorderRadius.ZERO, new ColorRGBA(60, 60, 80, 90));
      DrawUtil.drawRoundedRect(draw.getMatrices(), px + 8, y, 40.0F, 1.0F, BorderRadius.ZERO, accent.withAlpha(120));
   }

   private void drawInput(CustomDrawContext draw, float x, float y, float w, String label, StringBuilder buffer, boolean focused, ColorRGBA accent) {
      BorderRadius radius = BorderRadius.all(7.0F);
      rr(draw, x, y, w, 24, radius, focused ? INPUT_FOCUS : INPUT);
      border(draw, x, y, w, 24, radius, focused ? accent.withAlpha(160) : INPUT_BORDER);
      String value = buffer.toString();
      if (value.isEmpty()) {
         text(draw, Fonts.REGULAR, BODY_SIZE, label, x + 8, y + 8, GRAY);
      } else {
         String caret = focused && System.currentTimeMillis() % 900L < 450L ? "|" : "";
         String shown = trimWidth(Fonts.REGULAR, value + caret, w - 16, BODY_SIZE);
         text(draw, Fonts.REGULAR, BODY_SIZE, shown, x + 8, y + 8, WHITE);
      }
   }

   private void drawHeader(CustomDrawContext draw, int px) {
      text(draw, Fonts.SEMIBOLD, BODY_SIZE, "Ник", px + 10, 130, GRAY);
      text(draw, Fonts.SEMIBOLD, BODY_SIZE, "Сервер", px + 170, 130, GRAY);
      text(draw, Fonts.SEMIBOLD, BODY_SIZE, "Статус", px + 350, 130, GRAY);
   }

   private void drawRows(CustomDrawContext draw, int px, int mouseX, int mouseY, ColorRGBA accent) {
      List<InstanceBot> bots = InstanceBotManager.INSTANCE.getBots();
      int rowH = 34;
      int startY = 142;
      int maxRows = Math.max(0, (this.height - 16 - 60 - startY) / rowH);
      if (this.scroll > Math.max(0, bots.size() - maxRows)) {
         this.scroll = Math.max(0, bots.size() - maxRows);
      }

      for (int i = this.scroll; i < bots.size(); i++) {
         int y = startY + (i - this.scroll) * rowH;
         if (y + rowH > this.height - 16 - 54) {
            continue;
         }
         InstanceBot bot = bots.get(i);
         boolean hover = this.inside(mouseX, mouseY, px + 8, y, PANEL_WIDTH - 16, rowH - 4);
         BorderRadius radius = BorderRadius.all(8.0F);
         rr(draw, px + 8, y, PANEL_WIDTH - 16, rowH - 4, radius, hover ? ROW_HOVER : ROW);
         border(draw, px + 8, y, PANEL_WIDTH - 16, rowH - 4, radius, ROW_BORDER);

         text(draw, Fonts.SEMIBOLD, BODY_SIZE, bot.getName(), px + 10, y + 6, WHITE);
         text(draw, Fonts.REGULAR, BODY_SIZE, trimWidth(Fonts.REGULAR, bot.getServer(), 170, BODY_SIZE), px + 170, y + 6, new ColorRGBA(205, 205, 220, 255));

         BotConnection connection = InstanceBotManager.INSTANCE.bot(bot.getName());
         String status;
         ColorRGBA statusColor;
         if (connection == null) {
            status = "Оффлайн";
            statusColor = GRAY;
         } else {
            switch (connection.getState()) {
               case PLAY:
                  status = "В сети";
                  statusColor = GREEN;
                  break;
               case CONNECTING:
               case CONFIG:
                  status = "Подключение...";
                  statusColor = ORANGE;
                  break;
               case ERROR:
                  status = "Ошибка";
                  statusColor = RED;
                  break;
               default:
                  status = "Отключён";
                  statusColor = GRAY;
                  break;
            }
         }
         if (bot.getName().equalsIgnoreCase(InstanceBotManager.INSTANCE.getFocusedName())) {
            statusColor = accent;
         }
         text(draw, Fonts.SEMIBOLD, BODY_SIZE, status, px + 350, y + 6, statusColor);

         float btnStart = px + PANEL_WIDTH - 16 - 196;
         this.rowButton(draw, btnStart + 70, y, 60, "Подключить", this.inside(mouseX, mouseY, btnStart + 70, y, 60, 24), accent);
         if (connection != null && connection.getState() == BotConnection.State.PLAY) {
            this.rowButton(draw, btnStart + 136, y, 60, "Фокус", this.inside(mouseX, mouseY, btnStart + 136, y, 60, 24), accent);
         } else {
            rr(draw, btnStart + 136, y + 2, 60, 24, BorderRadius.all(7.0F), new ColorRGBA(28, 28, 34, 240));
            centerText(draw, Fonts.REGULAR, BODY_SIZE, "—", btnStart + 136, y + 9, 60, GRAY);
         }
         this.rowButton(draw, btnStart + 200, y, 50, "Убрать", this.inside(mouseX, mouseY, btnStart + 200, y, 50, 24), accent);
      }
   }

   private void rowButton(CustomDrawContext draw, float x, float y, float w, String label, boolean hover, ColorRGBA accent) {
      BorderRadius radius = BorderRadius.all(7.0F);
      rr(draw, x, y + 2, w, 24, radius, hover ? BUTTON_HOVER : BUTTON);
      if (hover) {
         border(draw, x, y + 2, w, 24, radius, accent.withAlpha(140));
      }
      centerText(draw, Fonts.SEMIBOLD, BODY_SIZE, label, x, y + 10, w, hover ? WHITE : GRAY);
   }

   private void drawFooter(CustomDrawContext draw, int px, int mouseX, int mouseY, ColorRGBA accent) {
      float fy = this.height - 40;
      text(draw, Fonts.REGULAR, HINT_SIZE, ".botfocusscreen <ник> — включить фокус  ·  .botfocusscreen off — выключить", px + 8, fy + 7, GRAY);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return true;
      }
      int px = this.panelX();
      float x = (float) mouseX;
      float y = (float) mouseY;

      float fieldW = 240.0F;
      float addW = PANEL_WIDTH - 16 - 2.0F * (fieldW + 8.0F);
      float addX = px + 8 + 2.0F * (fieldW + 8.0F);
      if (this.inside(x, y, addX, 50, addW, 24)) {
         this.addBot();
         return true;
      }
      if (this.inside(x, y, px + 8, 50, fieldW, 24)) {
         this.focusedField = 0;
         return true;
      }
      if (this.inside(x, y, px + 8 + fieldW + 8.0F, 50, fieldW, 24)) {
         this.focusedField = 1;
         return true;
      }

      float rowH = 34;
      float startY = 142;
      List<InstanceBot> bots = InstanceBotManager.INSTANCE.getBots();
      for (int i = this.scroll; i < bots.size(); i++) {
         float ry = startY + (i - this.scroll) * rowH;
         if (y < ry || y > ry + rowH - 4) {
            continue;
         }
         InstanceBot bot = bots.get(i);
         float btnStart = px + PANEL_WIDTH - 16 - 196;
         if (this.inside(x, y, btnStart + 70, ry, 60, 24)) {
            InstanceBotManager.INSTANCE.launch(bot);
            return true;
         }
         if (this.inside(x, y, btnStart + 136, ry, 60, 24)) {
            if (InstanceBotManager.INSTANCE.isConnected(bot.getName())) {
               InstanceBotManager.INSTANCE.focusBot(bot.getName());
            }
            return true;
         }
         if (this.inside(x, y, btnStart + 200, ry, 50, 24)) {
            InstanceBotManager.INSTANCE.remove(bot);
            return true;
         }
         break;
      }

      this.focusedField = -1;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return true;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return;
      }
      super.mouseMoved(mouseX, mouseY);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return true;
      }
      List<InstanceBot> bots = InstanceBotManager.INSTANCE.getBots();
      int maxRows = Math.max(0, (this.height - 16 - 60 - 142) / 34);
      this.scroll = (int) Math.max(0, Math.min(Math.max(0, bots.size() - maxRows), this.scroll - Math.round(verticalAmount)));
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         if (InstanceBotManager.INSTANCE.isChatOpen()) {
            if (keyCode == 256) {
               InstanceBotManager.INSTANCE.setChatOpen(false);
            } else if (keyCode == 257 || keyCode == 335) {
               this.sendChat();
            } else if (keyCode == 259) {
               if (this.chatBuffer.length() > 0) {
                  this.chatBuffer.deleteCharAt(this.chatBuffer.length() - 1);
               }
            }
            return true;
         }
         if (keyCode == 84) {
            this.skipNextChar = true;
            InstanceBotManager.INSTANCE.setChatOpen(true);
            return true;
         }
         if (keyCode == 256) {
            InstanceBotManager.INSTANCE.releaseFocusWithMessage();
            return true;
         }
         return true;
      }
      if (this.focusedField >= 0) {
         if (keyCode == 257 || keyCode == 335) {
            if (this.focusedField == 0) {
               this.addBot();
            }
            return true;
         }
         if (keyCode == 259) {
            StringBuilder buffer = this.focusedField == 0 ? this.nameBuffer : this.serverBuffer;
            if (buffer.length() > 0) {
               buffer.deleteCharAt(buffer.length() - 1);
            }
            return true;
         }
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         return true;
      }
      return super.keyReleased(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (InstanceBotManager.INSTANCE.isFocused()) {
         if (InstanceBotManager.INSTANCE.isChatOpen() && Character.isDefined(chr) && !Character.isISOControl(chr)) {
            if (this.skipNextChar) {
               this.skipNextChar = false;
               return true;
            }
            if (this.chatBuffer.length() < 256) {
               this.chatBuffer.append(chr);
            }
         }
         return true;
      }
      if (this.focusedField >= 0 && Character.isDefined(chr) && !Character.isISOControl(chr)) {
         StringBuilder buffer = this.focusedField == 0 ? this.nameBuffer : this.serverBuffer;
         if (buffer.length() < 64) {
            buffer.append(chr);
         }
         return true;
      }
      return super.charTyped(chr, modifiers);
   }

   private void sendChat() {
      String message = this.chatBuffer.toString().trim();
      this.chatBuffer.setLength(0);
      InstanceBotManager.INSTANCE.setChatOpen(false);
      if (!message.isEmpty()) {
         String name = InstanceBotManager.INSTANCE.getFocusedName();
         if (name != null) {
            BotConnection connection = InstanceBotManager.INSTANCE.bot(name);
            if (connection != null) {
               connection.sendChat(message);
            }
         }
      }
   }

   private void addBot() {
      String name = this.nameBuffer.toString().trim();
      String server = this.serverBuffer.toString().trim();
      if (name.isEmpty()) {
         this.setNotice("Введи ник бота");
         return;
      }
      if (InstanceBotManager.INSTANCE.findByName(name) != null) {
         this.setNotice("Бот с таким ником уже есть");
         return;
      }
      InstanceBotManager.INSTANCE.add(name, server);
      if (!server.isEmpty()) {
         InstanceBotManager.INSTANCE.setDefaultServer(server);
      }
      this.nameBuffer.setLength(0);
      this.notice = "";
   }

   @Override
   public void close() {
      InstanceBotManager.INSTANCE.releaseFocus();
      super.close();
      tech.huihui.client.modules.impl.misc.InstanceBot module = tech.huihui.client.modules.impl.misc.InstanceBot.INSTANCE;
      if (module != null && module.isEnabled()) {
         module.setEnabled(false);
      }
   }
}