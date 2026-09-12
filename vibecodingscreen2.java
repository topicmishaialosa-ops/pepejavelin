package tech.huihui.client.screens.vibecoding;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.collection.ArrayListDeque;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.impl.misc.VibeCoding;
import tech.huihui.client.script.ScriptErrorCapture;
import tech.huihui.utility.ai.ZaiAi.Provider;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class VibeCodingScreen extends Screen implements IMinecraft {

   public static void open() {
      mc.setScreen(new VibeCodingScreen());
   }

   private final VibeCoding module = VibeCoding.INSTANCE;
   private static final List<Session> sessions = new ArrayList<>();
   private static int activeSessionIdx = -1;

   private String input = "";
   private int cursor = 0;
   private boolean focused = true;
   private boolean waiting = false;
   private float openAnim = 0.0F;

   private boolean showingApiKeyInput = false;
   private String apiKeyInput = "";
   private int apiKeyCursor = 0;

   private boolean showingColorPicker = false;
   private float pickerHue;
   private float pickerSat;
   private float pickerVal;
   private boolean draggingSV = false;
   private boolean draggingHue = false;

   private static final float FONT = 8.0F;
   private static final float FONT_SM = 7.0F;
   private static final float FONT_XS = 6.0F;
   private static final int MAX_INPUT = 500;
   private static final float PAD = 12.0F;
   private static final int MAX_RETRIES = 4;

   // === COSMETICS-STYLE COLORS ===
   private static final ColorRGBA BG_OVERLAY = new ColorRGBA(8, 8, 14, 200);
   private static final ColorRGBA PANEL_BG = new ColorRGBA(16, 16, 24, 255);
   private static final ColorRGBA SIDEBAR_BG = new ColorRGBA(12, 12, 18, 255);
   private static final ColorRGBA CARD_BG = new ColorRGBA(22, 22, 32, 255);
   private static final ColorRGBA CARD_HOVER = new ColorRGBA(30, 30, 45, 255);
   private static final ColorRGBA CARD_ACTIVE = new ColorRGBA(35, 35, 55, 255);
   private static final ColorRGBA BORDER = new ColorRGBA(45, 45, 60, 180);
   private static final ColorRGBA BORDER_ACTIVE = new ColorRGBA(70, 70, 100, 200);
   private static final ColorRGBA TEXT = new ColorRGBA(230, 230, 245, 255);
   private static final ColorRGBA TEXT_DIM = new ColorRGBA(140, 140, 165, 255);
   private static final ColorRGBA TEXT_DARK = new ColorRGBA(90, 90, 115, 255);
   private static final ColorRGBA ACCENT_GLOW = new ColorRGBA(100, 120, 255, 40);

   public VibeCodingScreen() {
      super(Text.literal("VibeCoding"));
      if (sessions.isEmpty()) {
         Session s = new Session("Сессия 1");
         s.addMessage(false,
            "Привет! Я сгенерирую скрипт для HuihuiClient.\n"
            + "Выбери провайдера и введи API-ключ, затем напиши запрос.\n"
            + "Код сохраню в файл, а здесь напишу что сделано.\n"
            + "Кнопка «Stacktrace» отправит последнюю ошибку нейросети для исправления.");
         sessions.add(s);
         activeSessionIdx = 0;
      }
      ColorRGBA c = module.accent.getColor();
      float[] hsv = rgbToHsv(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
      pickerHue = hsv[0];
      pickerSat = hsv[1];
      pickerVal = hsv[2];
   }

   private Session activeSession() {
      if (activeSessionIdx < 0 || activeSessionIdx >= sessions.size()) {
         activeSessionIdx = 0;
      }
      return sessions.get(activeSessionIdx);
   }

   @Override
   public boolean shouldPause() { return false; }

   @Override
   public void close() { mc.setScreen(null); }

   private record Bubble(boolean user, float w, float h, List<String> lines) {}

   private L layout() {
      float pw = Math.min(900.0F, this.width - 32.0F);
      float ph = Math.min(620.0F, this.height - 32.0F);
      float px = (this.width - pw) / 2.0F;
      float py = (this.height - ph) / 2.0F;

      float sidebarW = 200.0F;
      float headerH = 48.0F;
      float inputH = 42.0F;
      float inputAreaH = inputH + PAD * 2;

      float chatX = px + sidebarW;
      float chatW = pw - sidebarW;
      float chatY = py;
      float chatH = ph;

      float msgTop = chatY + headerH + PAD;
      float msgBottom = chatY + chatH - inputAreaH - PAD;
      float maxTextW = chatW - PAD * 4;

      float inputX = chatX + PAD;
      float inputY = chatY + chatH - inputAreaH + PAD / 2;
      float inputW = chatW - PAD * 2;

      float sendW = 90.0F;
      float sendH = 34.0F;
      float sendX = inputX + inputW - sendW;
      float sendY = inputY + (inputH - sendH) / 2.0F;
      float actualInputW = inputW - sendW - 12.0F;

      float closeW = 26.0F;
      float closeH = 26.0F;
      float closeX = px + pw - closeW - 14.0F;
      float closeY = py + (headerH - closeH) / 2.0F;

      float sidebarHeaderH = 44.0F;
      float newSessionW = sidebarW - 24.0F;
      float newSessionH = 30.0F;
      float newSessionX = px + 12.0F;
      float newSessionY = py + sidebarHeaderH + 10.0F;

      return new L(px, py, pw, ph, sidebarW, headerH, chatX, chatW, chatY, chatH,
         msgTop, msgBottom, maxTextW, inputX, inputY, actualInputW, inputH,
         sendX, sendY, sendW, sendH, closeX, closeY, closeW, closeH,
         sidebarHeaderH, newSessionX, newSessionY, newSessionW, newSessionH);
   }

   private record L(float px, float py, float pw, float ph, float sidebarW, float headerH,
                     float chatX, float chatW, float chatY, float chatH,
                     float msgTop, float msgBottom, float maxTextW,
                     float inputX, float inputY, float inputW, float inputH,
                     float sendX, float sendY, float sendW, float sendH,
                     float closeX, float closeY, float closeW, float closeH,
                     float sidebarHeaderH, float newSessionX, float newSessionY,
                     float newSessionW, float newSessionH) {

      boolean hitClose(double mx, double my) {
         return mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH;
      }
      boolean hitSend(double mx, double my) {
         return mx >= sendX && mx <= sendX + sendW && my >= sendY && my <= sendY + sendH;
      }
      boolean hitInput(double mx, double my) {
         return mx >= inputX && mx <= inputX + inputW && my >= inputY && my <= inputY + inputH;
      }
      boolean hitNewSession(double mx, double my) {
         return mx >= newSessionX && mx <= newSessionX + newSessionW
            && my >= newSessionY && my <= newSessionY + newSessionH;
      }
      boolean hitSidebar(double mx, double my) {
         return mx >= px && mx <= px + sidebarW && my >= py && my <= py + ph;
      }
      boolean inChat(double mx, double my) {
         return mx >= chatX && mx <= chatX + chatW && my >= chatY && my <= chatY + chatH;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      this.openAnim += (1.0F - this.openAnim) * 0.15F;
      if (this.openAnim > 0.995F) this.openAnim = 1.0F;
      float a = this.openAnim;
      L L = this.layout();
      ColorRGBA accent = module.accent.getColor();
      CustomDrawContext ctx = CustomDrawContext.of(context);
      var ms = ctx.getMatrices();

      // === BACKGROUND OVERLAY ===
      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, BG_OVERLAY.withAlpha((int)(200.0F * a)));

      // === MAIN PANEL ===
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.pw, L.ph, BorderRadius.all(16.0F),
         PANEL_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.px, L.py, L.pw, L.ph, 1.0F, BorderRadius.all(16.0F),
         BORDER.withAlpha((int)(120.0F * a)));

      // === SIDEBAR ===
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.sidebarW, L.ph,
         BorderRadius.of(16.0F, 0.0F, 0.0F, 16.0F), SIDEBAR_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRect(ms, L.px + L.sidebarW - 1.0F, L.py, 1.0F, L.ph,
         BORDER.withAlpha((int)(80.0F * a)));

      // Sidebar header
      ctx.drawText(Fonts.SEMIBOLD.getFont(10.0F), "VibeCoding",
         L.px + 14.0F, L.py + (L.sidebarHeaderH - 10.0F) / 2.0F,
         TEXT.withAlpha((int)(255.0F * a)));

      // New session button (compact card style)
      boolean newHover = L.hitNewSession(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.newSessionX, L.newSessionY, L.newSessionW, L.newSessionH,
         BorderRadius.all(8.0F),
         newHover ? CARD_HOVER.withAlpha((int)(255.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.newSessionX, L.newSessionY, L.newSessionW, L.newSessionH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha((int)(150.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "+ Новая сессия",
         L.newSessionX + 10.0F, L.newSessionY + (L.newSessionH - 7.0F) / 2.0F,
         TEXT.withAlpha((int)(200.0F * a)));

      this.renderSessionList(ctx, L, accent, a, mouseX, mouseY);
      this.renderSidebarButtons(ctx, L, accent, a, mouseX, mouseY);
      this.renderMessages(ctx, L, accent, a);

      // Chat header line
      DrawUtil.drawRect(ms, L.chatX, L.py + L.headerH, L.chatW, 1.0F,
         BORDER.withAlpha((int)(60.0F * a)));

      // Input area background
      DrawUtil.drawRoundedRect(ms, L.inputX, L.inputY, L.inputW, L.inputH,
         BorderRadius.all(10.0F),
         this.focused ? CARD_BG.withAlpha((int)(255.0F * a)) : new ColorRGBA(255, 255, 255, (int)(5.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.inputX, L.inputY, L.inputW, L.inputH, 1.0F,
         BorderRadius.all(10.0F),
         this.focused ? accent.withAlpha((int)(140.0F * a)) : BORDER.withAlpha((int)(100.0F * a)));

      // Input text
      String shown = this.input;
      int start = 0;
      while (start < shown.length() && Fonts.REGULAR.getWidth(shown.substring(start), FONT) > L.inputW - 24.0F) start++;
      String visible = shown.substring(start);
      float iy = L.inputY + (L.inputH - Fonts.REGULAR.getFont(FONT).height()) / 2.0F;
      if (visible.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), "Напиши запрос...",
            L.inputX + 12.0F, iy, TEXT_DARK.withAlpha((int)(255.0F * a)));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), visible, L.inputX + 12.0F, iy,
            TEXT.withAlpha((int)(245.0F * a)));
      }
      if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         int ci = Math.max(start, Math.min(this.cursor, shown.length()));
         float cx = L.inputX + 12.0F + Fonts.REGULAR.getWidth(shown.substring(start, ci), FONT);
         DrawUtil.drawLine(ms, new net.minecraft.util.math.Vec2f(cx, L.inputY + 10.0F),
            new net.minecraft.util.math.Vec2f(cx, L.inputY + L.inputH - 10.0F),
            TEXT.withAlpha((int)(200.0F * a)));
      }

      // Send button (compact pill style)
      boolean sendHover = L.hitSend(mouseX, mouseY) && !this.waiting;
      DrawUtil.drawRoundedRect(ms, L.sendX, L.sendY, L.sendW, L.sendH,
         BorderRadius.all(8.0F),
         sendHover ? accent.brighter(0.1F).withAlpha((int)(255.0F * a)) : accent.withAlpha((int)(220.0F * a)));
      String sendLabel = this.waiting ? "..." : "Отправить";
      float sw = Fonts.SEMIBOLD.getWidth(sendLabel, FONT_SM);
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), sendLabel,
         L.sendX + L.sendW / 2.0F - sw / 2.0F,
         L.sendY + L.sendH / 2.0F - 3.5F,
         new ColorRGBA(255, 255, 255, (int)(255.0F * a)));

      // Close button (subtle X)
      boolean closeHover = L.hitClose(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.closeX, L.closeY, L.closeW, L.closeH,
         BorderRadius.all(6.0F),
         closeHover ? new ColorRGBA(255, 80, 80, (int)(30.0F * a)) : new ColorRGBA(255, 255, 255, (int)(6.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(8.0F), "×",
         L.closeX + L.closeW / 2.0F - 2.5F, L.closeY + (L.closeH - 8.0F) / 2.0F,
         closeHover ? new ColorRGBA(255, 100, 100, (int)(255.0F * a)) : TEXT_DIM.withAlpha((int)(180.0F * a)));

      if (this.showingColorPicker) this.renderColorPicker(ctx, accent, a, mouseX, mouseY);
      if (this.showingApiKeyInput) this.renderApiKeyInput(ctx, accent, a, mouseX, mouseY);
   }

   private void renderSessionList(CustomDrawContext ctx, L L, ColorRGBA accent, float a, int mx, int my) {
      float y = L.newSessionY + L.newSessionH + 10.0F;
      float itemH = 32.0F;
      float itemW = L.sidebarW - 24.0F;
      float x = L.px + 12.0F;

      for (int i = 0; i < sessions.size(); i++) {
         Session s = sessions.get(i);
         boolean isActive = i == activeSessionIdx;
         boolean hover = mx >= x && mx <= x + itemW && my >= y && my <= y + itemH;

         // Card background
         if (isActive) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, itemH,
               BorderRadius.all(8.0F), accent.withAlpha((int)(20.0F * a)));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, itemH,
               1.0F, BorderRadius.all(8.0F), accent.withAlpha((int)(50.0F * a)));
         } else if (hover) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, itemH,
               BorderRadius.all(8.0F), CARD_HOVER.withAlpha((int)(255.0F * a)));
         }

         String label = s.getShortLabel();
         ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), label,
            x + 10.0F, y + 7.0F,
            isActive ? accent.withAlpha((int)(255.0F * a)) : TEXT.withAlpha((int)(180.0F * a)));

         String meta = s.history.size() + " msgs";
         ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), meta,
            x + 10.0F, y + 18.0F,
            TEXT_DIM.withAlpha((int)(120.0F * a)));

         y += itemH + 4.0F;
      }
   }

   private void renderSidebarButtons(CustomDrawContext ctx, L L, ColorRGBA accent, float a, int mx, int my) {
      float x = L.px + 12.0F;
      float itemW = L.sidebarW - 24.0F;
      float y = L.py + L.ph - 130.0F;

      float btnH = 28.0F;
      float gap = 4.0F;

      // API Key
      boolean keyHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         keyHover ? CARD_HOVER.withAlpha((int)(255.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha((int)(100.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "API Key",
         x + 10.0F, y + (btnH - 7.0F) / 2.0F, TEXT.withAlpha((int)(180.0F * a)));

      y += btnH + gap;
      // Stacktrace
      boolean traceHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH && !this.waiting;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         traceHover ? CARD_HOVER.withAlpha((int)(255.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha((int)(100.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Stacktrace",
         x + 10.0F, y + (btnH - 7.0F) / 2.0F,
         TEXT.withAlpha(this.waiting ? (int)(80.0F * a) : (int)(180.0F * a)));

      y += btnH + gap;
      // Clear
      boolean clearHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         clearHover ? new ColorRGBA(255, 80, 80, (int)(20.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 100, 100, (int)(60.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Очистить",
         x + 10.0F, y + (btnH - 7.0F) / 2.0F,
         clearHover ? new ColorRGBA(255, 140, 140, (int)(255.0F * a)) : new ColorRGBA(255, 120, 120, (int)(150.0F * a)));

      y += btnH + gap;
      // Color picker
      boolean colorHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         colorHover ? CARD_HOVER.withAlpha((int)(255.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha((int)(100.0F * a)));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 10.0F, y + 7.0F, 14.0F, 14.0F,
         BorderRadius.all(4.0F), accent.withAlpha((int)(255.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Цвет темы",
         x + 30.0F, y + (btnH - 7.0F) / 2.0F, TEXT.withAlpha((int)(180.0F * a)));
   }

   private void renderColorPicker(CustomDrawContext ctx, ColorRGBA accent, float a, int mx, int my) {
      var ms = ctx.getMatrices();
      float w = 260.0F;
      float h = 220.0F;
      float x = (this.width - w) / 2.0F;
      float y = (this.height - h) / 2.0F;

      // Backdrop
      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(0, 0, 0, (int)(160.0F * a)));
      // Modal card
      DrawUtil.drawRoundedRect(ms, x, y, w, h, BorderRadius.all(16.0F),
         PANEL_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, x, y, w, h, 1.0F, BorderRadius.all(16.0F),
         accent.withAlpha((int)(160.0F * a)));

      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Выбери цвет темы",
         x + 16.0F, y + 14.0F, TEXT.withAlpha((int)(255.0F * a)));

      float svX = x + 16.0F;
      float svY = y + 38.0F;
      float svW = 170.0F;
      float svH = 130.0F;

      for (int py2 = 0; py2 < (int) svH; py2++) {
         for (int px2 = 0; px2 < (int) svW; px2++) {
            float s = px2 / svW;
            float v = 1.0F - py2 / svH;
            int rgb = hsvToRgb(this.pickerHue, s, v);
            DrawUtil.drawRect(ms, svX + px2, svY + py2, 1.0F, 1.0F,
               new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int)(255.0F * a)));
         }
      }
      DrawUtil.drawRoundedBorder(ms, svX, svY, svW, svH, 1.0F, BorderRadius.all(6.0F),
         BORDER.withAlpha((int)(180.0F * a)));

      float cursorSX = svX + this.pickerSat * svW;
      float cursorSY = svY + (1.0F - this.pickerVal) * svH;
      DrawUtil.drawRoundedRect(ms, cursorSX - 4.0F, cursorSY - 4.0F, 8.0F, 8.0F,
         BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255, (int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, cursorSX - 5.0F, cursorSY - 5.0F, 10.0F, 10.0F,
         1.0F, BorderRadius.all(5.0F), new ColorRGBA(0, 0, 0, (int)(160.0F * a)));

      float hueX = svX + svW + 10.0F;
      float hueY = svY;
      float hueW = 14.0F;
      float hueH = svH;

      for (int hy = 0; hy < (int) hueH; hy++) {
         float hue = hy / hueH;
         int rgb = hsvToRgb(hue, 1.0F, 1.0F);
         DrawUtil.drawRect(ms, hueX, hueY + hy, hueW, 1.0F,
            new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int)(255.0F * a)));
      }
      DrawUtil.drawRoundedBorder(ms, hueX, hueY, hueW, hueH, 1.0F, BorderRadius.all(6.0F),
         BORDER.withAlpha((int)(180.0F * a)));

      float cursorHY = hueY + this.pickerHue * hueH;
      DrawUtil.drawRect(ms, hueX - 2.0F, cursorHY - 1.0F, hueW + 4.0F, 3.0F,
         new ColorRGBA(255, 255, 255, (int)(255.0F * a)));

      int previewRgb = hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal);
      DrawUtil.drawRoundedRect(ms, hueX + hueW + 10.0F, svY, 26.0F, 26.0F,
         BorderRadius.all(6.0F),
         new ColorRGBA((previewRgb >> 16) & 0xFF, (previewRgb >> 8) & 0xFF, previewRgb & 0xFF, (int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, hueX + hueW + 10.0F, svY, 26.0F, 26.0F,
         1.0F, BorderRadius.all(6.0F), BORDER.withAlpha((int)(180.0F * a)));

      float applyY = svY + svH + 12.0F;
      float applyW = 90.0F;
      float applyH = 26.0F;
      float applyX = x + w - applyW - 16.0F;
      boolean applyHover = mx >= applyX && mx <= applyX + applyW && my >= applyY && my <= applyY + applyH;
      DrawUtil.drawRoundedRect(ms, applyX, applyY, applyW, applyH, BorderRadius.all(8.0F),
         applyHover ? accent.brighter(0.1F).withAlpha((int)(255.0F * a)) : accent.withAlpha((int)(220.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Применить",
         applyX + applyW / 2.0F - Fonts.SEMIBOLD.getWidth("Применить", FONT_SM) / 2.0F,
         applyY + (applyH - 7.0F) / 2.0F,
         new ColorRGBA(255, 255, 255, (int)(255.0F * a)));

      float cancelW = 70.0F;
      float cancelX = applyX - cancelW - 8.0F;
      boolean cancelHover = mx >= cancelX && mx <= cancelX + cancelW && my >= applyY && my <= applyY + applyH;
      DrawUtil.drawRoundedRect(ms, cancelX, applyY, cancelW, applyH, BorderRadius.all(8.0F),
         cancelHover ? new ColorRGBA(255, 80, 80, (int)(25.0F * a)) : new ColorRGBA(255, 255, 255, (int)(8.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Отмена",
         cancelX + cancelW / 2.0F - Fonts.SEMIBOLD.getWidth("Отмена", FONT_SM) / 2.0F,
         applyY + (applyH - 7.0F) / 2.0F, TEXT.withAlpha((int)(220.0F * a)));
   }

   private void renderApiKeyInput(CustomDrawContext ctx, ColorRGBA accent, float a, int mx, int my) {
      var ms = ctx.getMatrices();
      float w = 400.0F;
      float h = 150.0F;
      float x = (this.width - w) / 2.0F;
      float y = (this.height - h) / 2.0F;

      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(0, 0, 0, (int)(160.0F * a)));
      DrawUtil.drawRoundedRect(ms, x, y, w, h, BorderRadius.all(16.0F),
         PANEL_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, x, y, w, h, 1.0F, BorderRadius.all(16.0F),
         accent.withAlpha((int)(160.0F * a)));

      Provider prov = module.getActiveProvider();
      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "API Key — " + prov.getDisplayName(),
         x + 16.0F, y + 14.0F, TEXT.withAlpha((int)(255.0F * a)));

      float fieldX = x + 16.0F;
      float fieldY = y + 38.0F;
      float fieldW = w - 32.0F;
      float fieldH = 30.0F;
      DrawUtil.drawRoundedRect(ms, fieldX, fieldY, fieldW, fieldH, BorderRadius.all(8.0F),
         CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, fieldX, fieldY, fieldW, fieldH, 1.0F, BorderRadius.all(8.0F),
         accent.withAlpha((int)(140.0F * a)));

      String display = this.apiKeyInput.isEmpty() ? "" : "*".repeat(Math.min(this.apiKeyInput.length(), 50));
      float fy = fieldY + (fieldH - Fonts.REGULAR.getFont(FONT).height()) / 2.0F;
      if (display.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), "Вставь API-ключ...",
            fieldX + 10.0F, fy, TEXT_DARK.withAlpha((int)(255.0F * a)));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), display, fieldX + 10.0F, fy,
            TEXT.withAlpha((int)(245.0F * a)));
      }

      float btnY = fieldY + fieldH + 12.0F;
      float btnH = 26.0F;
      float saveW = 90.0F;
      float pasteW = 80.0F;
      float cancelW = 70.0F;
      float totalBtns = saveW + pasteW + cancelW + 16.0F;
      float btnStartX = x + w - totalBtns - 16.0F;

      float saveX = btnStartX;
      boolean saveHover = mx >= saveX && mx <= saveX + saveW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, saveX, btnY, saveW, btnH, BorderRadius.all(8.0F),
         saveHover ? accent.brighter(0.1F).withAlpha((int)(255.0F * a)) : accent.withAlpha((int)(220.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Сохранить",
         saveX + saveW / 2.0F - Fonts.SEMIBOLD.getWidth("Сохранить", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F,
         new ColorRGBA(255, 255, 255, (int)(255.0F * a)));

      float pasteX = saveX + saveW + 8.0F;
      boolean pasteHover = mx >= pasteX && mx <= pasteX + pasteW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, pasteX, btnY, pasteW, btnH, BorderRadius.all(8.0F),
         pasteHover ? CARD_HOVER.withAlpha((int)(255.0F * a)) : CARD_BG.withAlpha((int)(255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, pasteX, btnY, pasteW, btnH, 1.0F, BorderRadius.all(8.0F),
         BORDER.withAlpha((int)(120.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Вставить",
         pasteX + pasteW / 2.0F - Fonts.SEMIBOLD.getWidth("Вставить", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F,
         TEXT.withAlpha((int)(220.0F * a)));

      float cancelX = pasteX + pasteW + 8.0F;
      boolean cancelHover = mx >= cancelX && mx <= cancelX + cancelW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, cancelX, btnY, cancelW, btnH, BorderRadius.all(8.0F),
         cancelHover ? new ColorRGBA(255, 80, 80, (int)(25.0F * a)) : new ColorRGBA(255, 255, 255, (int)(8.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Отмена",
         cancelX + cancelW / 2.0F - Fonts.SEMIBOLD.getWidth("Отмена", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F, TEXT.withAlpha((int)(220.0F * a)));
   }

   private void renderMessages(CustomDrawContext ctx, L L, ColorRGBA accent, float a) {
      Session session = activeSession();
      float lineH = FONT + 4.0F;
      List<Bubble> bubbles = new ArrayList<>();
      for (Session.Msg m : session.history) {
         List<String> lines = this.wrap(m.text(), L.maxTextW, FONT);
         float bh = lines.size() * lineH + 18.0F;
         float bw = Math.min(L.maxTextW + 24.0F, Math.max(80.0F, this.bubbleWidth(lines, L.maxTextW)));
         bubbles.add(new Bubble(m.user(), bw, bh, lines));
      }

      float avail = L.msgBottom - L.msgTop;
      int start = 0;
      float used = 0.0F;
      for (int i = bubbles.size() - 1; i >= 0; i--) {
         used += bubbles.get(i).h + 8.0F;
         if (used > avail) { start = i + 1; break; }
      }

      float y = L.msgBottom;
      for (int i = bubbles.size() - 1; i >= start; i--) {
         Bubble b = bubbles.get(i);
         y -= b.h + 8.0F;
         float bx = b.user() ? (L.chatX + L.chatW - PAD - b.w()) : (L.chatX + PAD);

         ColorRGBA fill = b.user() ? accent.withAlpha((int)(18.0F * a)) : CARD_BG.withAlpha((int)(200.0F * a));
         ColorRGBA borderC = b.user() ? accent.withAlpha((int)(60.0F * a)) : BORDER.withAlpha((int)(80.0F * a));

         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, y, b.w(), b.h(),
            BorderRadius.all(10.0F), fill);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, y, b.w(), b.h(),
            1.0F, BorderRadius.all(10.0F), borderC);

         String label = b.user() ? "you" : module.getActiveProvider().getDisplayName();
         ColorRGBA labelColor = b.user() ? accent.withAlpha((int)(180.0F * a)) : new ColorRGBA(140, 180, 255, (int)(200.0F * a));
         ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_XS), label, bx + 10.0F, y + 5.0F, labelColor);

         float ty = y + 16.0F;
         ColorRGBA textColor = TEXT.withAlpha((int)(240.0F * a));
         for (String line : b.lines()) {
            ctx.drawText(Fonts.REGULAR.getFont(FONT), line, bx + 10.0F, ty, textColor);
            ty += lineH;
         }
      }

      if (this.waiting) {
         float bx = L.chatX + PAD;
         float by = L.msgBottom - 24.0F;
         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, 110.0F, 20.0F,
            BorderRadius.all(8.0F), CARD_BG.withAlpha((int)(200.0F * a)));
         ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), "нейросеть печатает...",
            bx + 10.0F, by + 5.5F,
            new ColorRGBA(140, 180, 255, (int)(255.0F * a)));
      }
   }

   private float bubbleWidth(List<String> lines, float maxTextW) {
      float max = 0.0F;
      for (String l : lines) max = Math.max(max, Fonts.REGULAR.getWidth(l, FONT));
      return max + 24.0F;
   }

   private List<String> wrap(String text, float maxW, float size) {
      List<String> out = new ArrayList<>();
      for (String paragraph : text.split("\n")) {
         if (paragraph.isEmpty()) { out.add(""); continue; }
         StringBuilder line = new StringBuilder();
         for (String word : paragraph.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (Fonts.REGULAR.getWidth(test, size) > maxW && line.length() > 0) {
               out.add(line.toString());
               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(test);
            }
         }
         out.add(line.toString());
      }
      return out;
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      L L = this.layout();

      if (this.showingColorPicker) {
         float w = 260.0F;
         float h = 220.0F;
         float x = (this.width - w) / 2.0F;
         float y = (this.height - h) / 2.0F;
         float svX = x + 16.0F;
         float svY = y + 38.0F;
         float svW = 170.0F;
         float svH = 130.0F;
         float hueX = svX + svW + 10.0F;
         float hueY = svY;
         float hueW = 14.0F;
         float hueH = svH;
         float applyY = svY + svH + 12.0F;
         float applyW = 90.0F;
         float applyH = 26.0F;
         float applyX = x + w - applyW - 16.0F;
         float cancelW = 70.0F;
         float cancelX = applyX - cancelW - 8.0F;

         if (button == 0) {
            if (mx >= svX && mx <= svX + svW && my >= svY && my <= svY + svH) {
               this.pickerSat = (float) Math.max(0, Math.min(1, (mx - svX) / svW));
               this.pickerVal = (float) Math.max(0, Math.min(1, 1.0F - (my - svY) / svH));
               this.draggingSV = true;
               return true;
            }
            if (mx >= hueX && mx <= hueX + hueW && my >= hueY && my <= svY + hueH) {
               this.pickerHue = (float) Math.max(0, Math.min(1, (my - hueY) / hueH));
               this.draggingHue = true;
               return true;
            }
            if (mx >= applyX && mx <= applyX + applyW && my >= applyY && my <= applyY + applyH) {
               int rgb = hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal);
               int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
               module.accent.setColor(new ColorRGBA(r, g, b, 255));
               this.showingColorPicker = false;
               return true;
            }
            if (mx >= cancelX && mx <= cancelX + cancelW && my >= applyY && my <= applyY + applyH) {
               this.showingColorPicker = false;
               return true;
            }
         }
         return true;
      }

      if (this.showingApiKeyInput) {
         float w = 400.0F;
         float h = 150.0F;
         float x = (this.width - w) / 2.0F;
         float y = (this.height - h) / 2.0F;
         float fieldX = x + 16.0F;
         float fieldY = y + 38.0F;
         float fieldW = w - 32.0F;
         float fieldH = 30.0F;
         float btnY = fieldY + fieldH + 12.0F;
         float btnH = 26.0F;
         float saveW = 90.0F;
         float pasteW = 80.0F;
         float cancelW = 70.0F;
         float totalBtns = saveW + pasteW + cancelW + 16.0F;
         float btnStartX = x + w - totalBtns - 16.0F;
         float saveX = btnStartX;
         float pasteX = saveX + saveW + 8.0F;
         float cancelX = pasteX + pasteW + 8.0F;

         if (button == 0) {
            if (mx >= saveX && mx <= saveX + saveW && my >= btnY && my <= btnY + btnH) {
               this.saveApiKey();
               return true;
            }
            if (mx >= pasteX && mx <= pasteX + pasteW && my >= btnY && my <= btnY + btnH) {
               if (mc.keyboard != null) {
                  String clip = mc.keyboard.getClipboard();
                  if (clip != null && !clip.isEmpty()) {
                     this.apiKeyInput = clip.trim();
                     this.apiKeyCursor = this.apiKeyInput.length();
                  }
               }
               return true;
            }
            if (mx >= cancelX && mx <= cancelX + cancelW && my >= btnY && my <= btnY + btnH) {
               this.showingApiKeyInput = false;
               return true;
            }
            if (mx >= fieldX && mx <= fieldX + fieldW && my >= fieldY && my <= fieldY + fieldH) {
               return true;
            }
         }
         this.showingApiKeyInput = false;
         return true;
      }

      if (button == 0 && L.hitClose(mx, my)) { this.close(); return true; }
      if (button == 0 && L.hitSend(mx, my) && !this.waiting) { this.send(); return true; }
      if (button == 0 && L.hitNewSession(mx, my)) {
         Session s = new Session("Сессия " + (sessions.size() + 1));
         s.addMessage(false, "Новая сессия начата. Напиши запрос.");
         sessions.add(s);
         activeSessionIdx = sessions.size() - 1;
         return true;
      }

      if (button == 0 && L.hitSidebar(mx, my)) {
         float itemH = 32.0F;
         float itemW = L.sidebarW - 24.0F;
         float x = L.px + 12.0F;
         float y = L.newSessionY + L.newSessionH + 10.0F;

         for (int i = 0; i < sessions.size(); i++) {
            if (mx >= x && mx <= x + itemW && my >= y && my <= y + itemH) {
               activeSessionIdx = i;
               return true;
            }
            y += itemH + 4.0F;
         }

         float btnY2 = L.py + L.ph - 130.0F;
         float btnH2 = 28.0F;
         float gap = 4.0F;
         float itemW2 = L.sidebarW - 24.0F;

         if (mx >= x && mx <= x + itemW2 && my >= btnY2 && my <= btnY2 + btnH2) {
            this.showingApiKeyInput = true;
            this.apiKeyInput = module.getActiveApiKey() != null ? module.getActiveApiKey() : "";
            this.apiKeyCursor = this.apiKeyInput.length();
            return true;
         }
         btnY2 += btnH2 + gap;
         if (mx >= x && mx <= x + itemW2 && my >= btnY2 && my <= btnY2 + btnH2 && !this.waiting) {
            this.sendTrace();
            return true;
         }
         btnY2 += btnH2 + gap;
         if (mx >= x && mx <= x + itemW2 && my >= btnY2 && my <= btnY2 + btnH2) {
            Session session = activeSession();
            session.history.clear();
            return true;
         }
         btnY2 += btnH2 + gap;
         if (mx >= x && mx <= x + itemW2 && my >= btnY2 && my <= btnY2 + btnH2) {
            ColorRGBA c = module.accent.getColor();
            float[] hsv = rgbToHsv(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F);
            pickerHue = hsv[0];
            pickerSat = hsv[1];
            pickerVal = hsv[2];
            this.showingColorPicker = true;
            return true;
         }
         return true;
      }

      if (button == 0 && L.inChat(mx, my)) this.focused = L.hitInput(mx, my);
      return true;
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      this.draggingSV = false;
      this.draggingHue = false;
      return super.mouseReleased(mx, my, button);
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.showingColorPicker) {
         float w = 260.0F;
         float h = 220.0F;
         float x = (this.width - w) / 2.0F;
         float y = (this.height - h) / 2.0F;
         float svX = x + 16.0F;
         float svY = y + 38.0F;
         float svW = 170.0F;
         float svH = 130.0F;
         float hueX = svX + svW + 10.0F;
         float hueY = svY;
         float hueH = svH;

         if (this.draggingSV) {
            this.pickerSat = (float) Math.max(0, Math.min(1, (mx - svX) / svW));
            this.pickerVal = (float) Math.max(0, Math.min(1, 1.0F - (my - svY) / svH));
            return true;
         }
         if (this.draggingHue) {
            this.pickerHue = (float) Math.max(0, Math.min(1, (my - hueY) / hueH));
            return true;
         }
      }
      return super.mouseDragged(mx, my, button, dx, dy);
   }

   private void saveApiKey() {
      Provider prov = module.getActiveProvider();
      switch (prov) {
         case ZAI -> module.apiKeyZai.setValue(this.apiKeyInput);
         case GOOGLE -> module.apiKeyGoogle.setValue(this.apiKeyInput);
         case OPENROUTER -> module.apiKeyOpenRouter.setValue(this.apiKeyInput);
         case XAI -> module.apiKeyXai.setValue(this.apiKeyInput);
         case KIMI -> module.apiKeyKimi.setValue(this.apiKeyInput);
      }
      this.showingApiKeyInput = false;
      Session session = activeSession();
      session.addMessage(false, "API-ключ " + prov.getDisplayName() + " сохранён.");
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (this.showingApiKeyInput) {
         if (chr < 32 || this.apiKeyInput.length() >= 200) return false;
         this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor) + chr + this.apiKeyInput.substring(this.apiKeyCursor);
         this.apiKeyCursor++;
         return true;
      }
      if (!this.focused || this.waiting) return false;
      if (chr < 32 || this.input.length() >= MAX_INPUT) return false;
      this.input = this.input.substring(0, this.cursor) + chr + this.input.substring(this.cursor);
      this.cursor++;
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.showingApiKeyInput) {
         if (keyCode == 256) { this.showingApiKeyInput = false; return true; }
         if (keyCode == 257 || keyCode == 335) { this.saveApiKey(); return true; }
         if (keyCode == 259) {
            if (this.apiKeyCursor > 0) {
               this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor - 1) + this.apiKeyInput.substring(this.apiKeyCursor);
               this.apiKeyCursor--;
            }
            return true;
         }
         if (keyCode == 260 && hasControlDown() && mc.keyboard != null) {
            String clip = mc.keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) {
               this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor) + clip + this.apiKeyInput.substring(this.apiKeyCursor);
               this.apiKeyCursor += clip.length();
            }
            return true;
         }
         return false;
      }

      if (this.focused && (keyCode == 257 || keyCode == 335)) { this.send(); return true; }
      if (keyCode == 256) {
         if (this.focused) this.focused = false;
         else this.close();
         return true;
      }
      if (!this.focused) return false;
      switch (keyCode) {
         case 259:
            if (this.cursor > 0) {
               this.input = this.input.substring(0, this.cursor - 1) + this.input.substring(this.cursor);
               this.cursor--;
            }
            return true;
         case 261:
            if (this.cursor < this.input.length())
               this.input = this.input.substring(0, this.cursor) + this.input.substring(this.cursor + 1);
            return true;
         case 263: if (this.cursor > 0) this.cursor--; return true;
         case 262: if (this.cursor < this.input.length()) this.cursor++; return true;
         case 268: this.cursor = 0; return true;
         case 269: this.cursor = this.input.length(); return true;
         default: return false;
      }
   }

   private void send() {
      String prompt = this.input.trim();
      if (prompt.isEmpty() || this.waiting) return;
      Provider prov = module.getActiveProvider();
      String key = module.getActiveApiKey();
      if (key == null || key.isBlank()) {
         Session session = activeSession();
         session.addMessage(false, "Сначала введи API-ключ для " + prov.getDisplayName() + ".");
         return;
      }
      Session session = activeSession();
      session.addMessage(true, prompt);
      session.lastPrompt = prompt;
      session.retries = 0;
      this.input = "";
      this.cursor = 0;
      this.doRequest(session);
   }

   private void doRequest(Session session) {
      this.waiting = true;
      this.module.requestScript(session.lastPrompt,
         (text, saved) -> {
            this.waiting = false;
            session.addMessage(false, text);
         },
         (err) -> this.onError(session, err));
   }

   private void onError(Session session, String err) {
      session.retries++;
      if (session.retries <= MAX_RETRIES) {
         session.addMessage(false, "запрос не прошёл — повтор через 3 сек...");
         new Thread(() -> {
            try { Thread.sleep(3000L); } catch (InterruptedException ignored) {}
            mc.execute(() -> {
               if (mc.currentScreen == VibeCodingScreen.this) this.doRequest(session);
            });
         }, "VibeCoding-Retry").start();
      } else {
         this.waiting = false;
         session.addMessage(false, "Ошибка: " + err);
      }
   }

   private static final String[] ERR_MARKERS = {
      "ошибка загрузки", "ошибка компиляции", "annotation processing", "note:",
      "luaerror", "stack traceback", "jsonsyntaxexception", "jsonparseexception",
      "expected", "exception", "caused by", "error:"
   };

   private String captureLastError() {
      try {
         ChatHud chatHud = this.mc.inGameHud.getChatHud();
         ArrayListDeque<String> hist = chatHud.getMessageHistory();
         if (hist == null || hist.isEmpty()) return null;
         List<String> all = new ArrayList<>(hist);
         int n = all.size();
         int from = Math.max(0, n - 400);
         List<String> strs = all.subList(from, n);
         int idx = -1;
         for (int i = strs.size() - 1; i >= 0; i--) {
            String low = strs.get(i).toLowerCase(java.util.Locale.ROOT);
            for (String m : ERR_MARKERS) {
               if (low.contains(m)) { idx = i; break; }
            }
            if (idx >= 0) break;
         }
         if (idx < 0) return null;
         StringBuilder sb = new StringBuilder();
         sb.append(strs.get(idx));
         int taken = 1;
         for (int i = idx + 1; i < strs.size() && taken < 40; i++) {
            String s = strs.get(i);
            if (isContinuation(s)) { sb.append('\n').append(s); taken++; }
            else break;
         }
         return sb.toString().trim();
      } catch (Exception e) { return null; }
   }

   private static boolean isContinuation(String s) {
      if (s == null || s.isEmpty()) return false;
      String t = s.trim();
      if (t.isEmpty()) return false;
      String low = t.toLowerCase(java.util.Locale.ROOT);
      if (s.startsWith(" ") || s.startsWith("\t")) return true;
      return low.contains("at ") && (low.contains(".java:") || low.contains(".lua:"))
         || low.contains("caused by") || low.contains("luaerror")
         || low.contains("stack traceback") || low.contains("attempt to")
         || low.contains(".java:") || low.contains(".lua:");
   }

   private void sendTrace() {
      if (this.waiting) return;
      String err = null;
      String lang = "?";
      ScriptErrorCapture.Entry last = ScriptErrorCapture.getLast();
      if (last != null) { err = last.full(); lang = last.lang; }
      else err = captureLastError();
      Session session = activeSession();
      if (err == null || err.isBlank()) {
         session.addMessage(false, "Ошибок не найдено. Загрузи проблемный скрипт, затем нажми «Stacktrace».");
         return;
      }
      session.addMessage(true, "стактрейс -> ИИ");
      session.lastPrompt = "Исправь ошибку в моём скрипте (" + lang + "). Вот сообщение об ошибке:\n\n"
         + err + "\n\nВерни ТОЛЬКО исправленный код в блоке ``` с указанием языка и имени файла "
         + "(комментарий '-- name:' для lua/json или '// name:' для java) в первой строке, как в документации. "
         + "Не добавляй пояснений вне блока кода.";
      session.retries = 0;
      this.doRequest(session);
   }

   private static float[] rgbToHsv(float r, float g, float b) {
      float max = Math.max(r, Math.max(g, b));
      float min = Math.min(r, Math.min(g, b));
      float d = max - min;
      float h = 0;
      if (d > 0.0001F) {
         if (max == r) h = ((g - b) / d) % 6.0F;
         else if (max == g) h = (b - r) / d + 2.0F;
         else h = (r - g) / d + 4.0F;
         h /= 6.0F;
         if (h < 0) h += 1.0F;
      }
      float s = max == 0 ? 0 : d / max;
      return new float[]{h, s, max};
   }

   private static int hsvToRgb(float h, float s, float v) {
      int i = (int) (h * 6.0F);
      float f = h * 6.0F - i;
      float p = v * (1.0F - s);
      float q = v * (1.0F - f * s);
      float t = v * (1.0F - (1.0F - f) * s);
      float r, g, b;
      switch (i % 6) {
         case 0: r = v; g = t; b = p; break;
         case 1: r = q; g = v; b = p; break;
         case 2: r = p; g = v; b = t; break;
         case 3: r = p; g = q; b = v; break;
         case 4: r = t; g = p; b = v; break;
         default: r = v; g = p; b = q; break;
      }
      return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
   }
}
