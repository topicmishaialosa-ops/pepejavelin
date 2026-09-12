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
import tech.huihui.utility.ai.ZaiAi;
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

   private boolean showingModelSelector = false;
   private List<ZaiAi.ModelInfo> availableModels = new ArrayList<>();
   private boolean loadingModels = false;
   private float modelScroll = 0.0F;

   private static final float FONT = 8.0F;
   private static final float FONT_SM = 7.0F;
   private static final float FONT_XS = 6.0F;
   private static final int MAX_INPUT = 500;
   private static final float PAD = 16.0F;
   private static final int MAX_RETRIES = 4;

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
      module.autoDetectModels();
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

   private static final ColorRGBA BG = new ColorRGBA(13, 13, 20);
   private static final ColorRGBA PANEL = new ColorRGBA(18, 18, 28);
   private static final ColorRGBA SIDEBAR = new ColorRGBA(15, 15, 22);
   private static final ColorRGBA SURFACE = new ColorRGBA(24, 24, 36);
   private static final ColorRGBA BORDER = new ColorRGBA(40, 40, 56);
   private static final ColorRGBA TEXT = new ColorRGBA(220, 220, 235);
   private static final ColorRGBA TEXT_DIM = new ColorRGBA(130, 130, 155);
   private static final ColorRGBA TEXT_DARK = new ColorRGBA(90, 90, 110);

   private L layout() {
      float pw = Math.min(1050.0F, this.width - 40.0F);
      float ph = Math.min(700.0F, this.height - 40.0F);
      float px = (this.width - pw) / 2.0F;
      float py = (this.height - ph) / 2.0F;

      float sidebarW = 240.0F;
      float headerH = 44.0F;
      float inputH = 44.0F;
      float inputAreaH = inputH + PAD * 2;

      float chatX = px + sidebarW;
      float chatW = pw - sidebarW;
      float chatY = py;
      float chatH = ph;

      float msgTop = chatY + headerH + PAD;
      float msgBottom = chatY + chatH - inputAreaH - PAD;
      float maxTextW = chatW - PAD * 4;

      float inputX = chatX + PAD;
      float inputY = chatY + chatH - inputAreaH;
      float inputW = chatW - PAD * 2;

      float sendW = 100.0F;
      float sendH = inputH;
      float sendX = inputX + inputW - sendW;
      float sendY = inputY;
      float actualInputW = inputW - sendW - 10.0F;

      float closeW = 28.0F;
      float closeH = 28.0F;
      float closeX = px + pw - closeW - 10.0F;
      float closeY = py + (headerH - closeH) / 2.0F;

      float sidebarHeaderH = 50.0F;
      float newSessionW = sidebarW - 24.0F;
      float newSessionH = 32.0F;
      float newSessionX = px + 12.0F;
      float newSessionY = py + sidebarHeaderH + 8.0F;

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
      this.openAnim += (1.0F - this.openAnim) * 0.18F;
      if (this.openAnim > 0.997F) this.openAnim = 1.0F;
      float a = this.openAnim;
      L L = this.layout();
      ColorRGBA accent = module.accent.getColor();
      CustomDrawContext ctx = CustomDrawContext.of(context);
      var ms = ctx.getMatrices();

      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, BG.withAlpha(200.0F * a));
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.pw, L.ph, BorderRadius.all(12.0F),
         PANEL.withAlpha(250.0F * a));
      DrawUtil.drawRoundedBorder(ms, L.px, L.py, L.pw, L.ph, 1.0F, BorderRadius.all(12.0F),
         BORDER.withAlpha(180.0F * a));

      DrawUtil.drawRect(ms, L.px, L.py, L.sidebarW, L.ph, SIDEBAR.withAlpha(250.0F * a));
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.sidebarW, L.sidebarHeaderH,
         BorderRadius.all(12.0F), new ColorRGBA(0, 0, 0, 0),
         new ColorRGBA(0, 0, 0, 0), new ColorRGBA(0, 0, 0, 0),
         SIDEBAR.withAlpha(250.0F * a));
      DrawUtil.drawRect(ms, L.px, L.py + L.sidebarHeaderH - 1.0F, L.sidebarW, 1.0F,
         BORDER.withAlpha(120.0F * a));

      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "VibeCoding",
         L.px + 14.0F, L.py + (L.sidebarHeaderH - 9.0F) / 2.0F,
         TEXT.withAlpha(255.0F * a));

      boolean newHover = L.hitNewSession(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.newSessionX, L.newSessionY, L.newSessionW, L.newSessionH,
         BorderRadius.all(8.0F),
         newHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (8.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.newSessionX, L.newSessionY, L.newSessionW, L.newSessionH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha(150.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "+  Новая сессия",
         L.newSessionX + 12.0F, L.newSessionY + (L.newSessionH - 7.0F) / 2.0F,
         TEXT.withAlpha(200.0F * a));

      this.renderSessionList(ctx, L, accent, a, mouseX, mouseY);
      this.renderProviderList(ctx, L, accent, a, mouseX, mouseY);
      this.renderSidebarButtons(ctx, L, accent, a, mouseX, mouseY);
      this.renderMessages(ctx, L, accent, a);

      DrawUtil.drawRect(ms, L.chatX, L.py + L.chatH - L.inputH - PAD * 2, L.chatW, 1.0F,
         BORDER.withAlpha(80.0F * a));

      DrawUtil.drawRoundedRect(ms, L.inputX, L.inputY, L.inputW, L.inputH,
         BorderRadius.all(10.0F),
         this.focused ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.inputX, L.inputY, L.inputW, L.inputH, 1.0F,
         BorderRadius.all(10.0F),
         this.focused ? accent.withAlpha(180.0F * a) : BORDER.withAlpha(150.0F * a));

      String shown = this.input;
      int start = 0;
      while (start < shown.length() && Fonts.REGULAR.getWidth(shown.substring(start), FONT) > L.inputW - 20.0F) start++;
      String visible = shown.substring(start);
      float iy = L.inputY + (L.inputH - Fonts.REGULAR.getFont(FONT).height()) / 2.0F;
      if (visible.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), "Напиши запрос...",
            L.inputX + 12.0F, iy, TEXT_DARK.withAlpha(255.0F * a));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), visible, L.inputX + 12.0F, iy,
            TEXT.withAlpha(245.0F * a));
      }
      if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         int ci = Math.max(start, Math.min(this.cursor, shown.length()));
         float cx = L.inputX + 12.0F + Fonts.REGULAR.getWidth(shown.substring(start, ci), FONT);
         DrawUtil.drawLine(ms, new net.minecraft.util.math.Vec2f(cx, L.inputY + 8.0F),
            new net.minecraft.util.math.Vec2f(cx, L.inputY + L.inputH - 8.0F),
            TEXT.withAlpha(200.0F * a));
      }

      boolean sendHover = L.hitSend(mouseX, mouseY) && !this.waiting;
      DrawUtil.drawRoundedRect(ms, L.sendX, L.sendY, L.sendW, L.sendH,
         BorderRadius.all(10.0F),
         sendHover ? accent.brighter(0.15F).withAlpha(255.0F * a) : accent.withAlpha(230.0F * a));
      String sendLabel = this.waiting ? "..." : "Отправить";
      float sw = Fonts.SEMIBOLD.getWidth(sendLabel, FONT_SM);
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), sendLabel,
         L.sendX + L.sendW / 2.0F - sw / 2.0F,
         L.sendY + L.sendH / 2.0F - 3.5F,
         new ColorRGBA(255, 255, 255, (int) (255.0F * a)));

      boolean closeHover = L.hitClose(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.closeX, L.closeY, L.closeW, L.closeH,
         BorderRadius.all(8.0F),
         closeHover ? new ColorRGBA(255, 80, 80, (int) (40.0F * a)) : new ColorRGBA(255, 255, 255, (int) (8.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "x",
         L.closeX + L.closeW / 2.0F - 3.0F, L.closeY + (L.closeH - 8.0F) / 2.0F,
         closeHover ? new ColorRGBA(255, 120, 120, (int) (255.0F * a)) : TEXT_DIM.withAlpha(200.0F * a));

      if (this.showingColorPicker) this.renderColorPicker(ctx, accent, a, mouseX, mouseY);
      if (this.showingApiKeyInput) this.renderApiKeyInput(ctx, accent, a, mouseX, mouseY);
      if (this.showingModelSelector) this.renderModelSelector(ctx, accent, a, mouseX, mouseY);
   }

   private void renderSessionList(CustomDrawContext ctx, L L, ColorRGBA accent, float a, int mx, int my) {
      float y = L.newSessionY + L.newSessionH + 8.0F;
      float itemH = 34.0F;
      float itemW = L.sidebarW - 24.0F;
      float x = L.px + 12.0F;

      for (int i = 0; i < sessions.size(); i++) {
         Session s = sessions.get(i);
         boolean isActive = i == activeSessionIdx;
         boolean hover = mx >= x && mx <= x + itemW && my >= y && my <= y + itemH;

         if (isActive) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, itemH,
               BorderRadius.all(8.0F), accent.withAlpha(25.0F * a));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, itemH,
               1.0F, BorderRadius.all(8.0F), accent.withAlpha(60.0F * a));
         } else if (hover) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, itemH,
               BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255, (int) (8.0F * a)));
         }

         String label = s.getShortLabel();
         ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), label,
            x + 10.0F, y + 8.0F,
            isActive ? accent.withAlpha(255.0F * a) : TEXT.withAlpha(180.0F * a));

         String meta = s.history.size() + " msgs";
         ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), meta,
            x + 10.0F, y + 19.0F,
            TEXT_DIM.withAlpha(140.0F * a));

         y += itemH + 3.0F;
      }
   }

   private void renderProviderList(CustomDrawContext ctx, L L, ColorRGBA accent, float a, int mx, int my) {
      float x = L.px + 12.0F;
      float itemW = L.sidebarW - 24.0F;
      float y = L.newSessionY + L.newSessionH + 8.0F + (sessions.size() * 37.0F) + 14.0F;

      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_XS), "ПРОВАЙДЕРЫ",
         x + 2.0F, y, TEXT_DIM.withAlpha(160.0F * a));
      y += 12.0F;

      Provider active = module.getActiveProvider();
      String activeKey = module.getActiveApiKey();
      boolean hasKey = activeKey != null && !activeKey.isBlank();

      for (Provider prov : Provider.values()) {
         boolean isActive = prov == active;
         boolean hover = mx >= x && mx <= x + itemW && my >= y && my <= y + 28.0F;

         if (isActive) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, 28.0F,
               BorderRadius.all(6.0F), accent.withAlpha(25.0F * a));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, 28.0F,
               1.0F, BorderRadius.all(6.0F), accent.withAlpha(50.0F * a));
         } else if (hover) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, 28.0F,
               BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, (int) (8.0F * a)));
         }

         ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), prov.getDisplayName(),
            x + 10.0F, y + 6.0F,
            isActive ? accent.withAlpha(255.0F * a) : TEXT.withAlpha(180.0F * a));

         if (isActive) {
            String status = hasKey ? "connected" : "no key";
            ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), status,
               x + 10.0F, y + 17.0F,
               hasKey ? new ColorRGBA(100, 220, 140, (int) (160.0F * a)) : new ColorRGBA(255, 140, 100, (int) (160.0F * a)));
         }

         y += 30.0F;
      }

      y += 6.0F;
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_XS), "МОДЕЛЬ",
         x + 2.0F, y, TEXT_DIM.withAlpha(160.0F * a));
      y += 12.0F;

      String currentModel = module.getActiveModel();
      boolean modelHover = mx >= x && mx <= x + itemW && my >= y && my <= y + 28.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, 28.0F,
         BorderRadius.all(6.0F), modelHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, 28.0F,
         1.0F, BorderRadius.all(6.0F), BORDER.withAlpha(150.0F * a));

      String displayModel = currentModel.length() > 22 ? currentModel.substring(0, 22) + "..." : currentModel;
      ctx.drawText(Fonts.REGULAR.getFont(FONT_SM), displayModel,
         x + 10.0F, y + 6.0F, TEXT.withAlpha(200.0F * a));

      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf078",
         x + itemW - 14.0F, y + 6.0F, TEXT_DIM.withAlpha(160.0F * a));

      if (loadingModels) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), "загрузка...",
            x + 10.0F, y + 17.0F, TEXT_DIM.withAlpha(140.0F * a));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), "нажми для выбора",
            x + 10.0F, y + 17.0F, TEXT_DIM.withAlpha(140.0F * a));
      }
   }

   private void renderSidebarButtons(CustomDrawContext ctx, L L, ColorRGBA accent, float a, int mx, int my) {
      float x = L.px + 12.0F;
      float itemW = L.sidebarW - 24.0F;
      float y = L.py + L.ph - 140.0F;

      float btnH = 30.0F;
      float gap = 5.0F;

      boolean keyHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         keyHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha(120.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "API Key",
         x + 12.0F, y + (btnH - 7.0F) / 2.0F, TEXT.withAlpha(200.0F * a));

      y += btnH + gap;
      boolean traceHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH && !this.waiting;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         traceHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha(120.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Stacktrace",
         x + 12.0F, y + (btnH - 7.0F) / 2.0F,
         TEXT.withAlpha(this.waiting ? 80.0F * a : 200.0F * a));

      y += btnH + gap;
      boolean clearHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         clearHover ? new ColorRGBA(255, 80, 80, (int) (25.0F * a)) : new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), new ColorRGBA(255, 100, 100, (int) (80.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Очистить",
         x + 12.0F, y + (btnH - 7.0F) / 2.0F,
         clearHover ? new ColorRGBA(255, 140, 140, (int) (255.0F * a)) : new ColorRGBA(255, 130, 130, (int) (160.0F * a)));

      y += btnH + gap;
      boolean colorHover = mx >= x && mx <= x + itemW && my >= y && my <= y + btnH;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, itemW, btnH,
         BorderRadius.all(8.0F),
         colorHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, itemW, btnH,
         1.0F, BorderRadius.all(8.0F), BORDER.withAlpha(120.0F * a));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 12.0F, y + 8.0F, 14.0F, 14.0F,
         BorderRadius.all(3.0F), accent.withAlpha(255.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Цвет темы",
         x + 32.0F, y + (btnH - 7.0F) / 2.0F, TEXT.withAlpha(200.0F * a));
   }

   private void renderColorPicker(CustomDrawContext ctx, ColorRGBA accent, float a, int mx, int my) {
      var ms = ctx.getMatrices();
      float w = 260.0F;
      float h = 220.0F;
      float x = (this.width - w) / 2.0F;
      float y = (this.height - h) / 2.0F;

      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(0, 0, 0, (int) (150.0F * a)));
      DrawUtil.drawRoundedRect(ms, x, y, w, h, BorderRadius.all(14.0F),
         new ColorRGBA(22, 22, 32, (int) (255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, x, y, w, h, 1.2F, BorderRadius.all(14.0F),
         accent.withAlpha(180.0F * a));

      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Выбери цвет темы",
         x + 16.0F, y + 14.0F, TEXT.withAlpha(255.0F * a));

      float svX = x + 16.0F;
      float svY = y + 36.0F;
      float svW = 180.0F;
      float svH = 140.0F;

      for (int py2 = 0; py2 < (int) svH; py2++) {
         for (int px2 = 0; px2 < (int) svW; px2++) {
            float s = px2 / svW;
            float v = 1.0F - py2 / svH;
            int rgb = hsvToRgb(this.pickerHue, s, v);
            DrawUtil.drawRect(ms, svX + px2, svY + py2, 1.0F, 1.0F,
               new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (255.0F * a)));
         }
      }
      DrawUtil.drawRoundedBorder(ms, svX, svY, svW, svH, 1.0F, BorderRadius.all(4.0F),
         BORDER.withAlpha(200.0F * a));

      float cursorSX = svX + this.pickerSat * svW;
      float cursorSY = svY + (1.0F - this.pickerVal) * svH;
      DrawUtil.drawRoundedRect(ms, cursorSX - 4.0F, cursorSY - 4.0F, 8.0F, 8.0F,
         BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255, (int) (255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, cursorSX - 5.0F, cursorSY - 5.0F, 10.0F, 10.0F,
         1.0F, BorderRadius.all(5.0F), new ColorRGBA(0, 0, 0, (int) (180.0F * a)));

      float hueX = svX + svW + 10.0F;
      float hueY = svY;
      float hueW = 16.0F;
      float hueH = svH;

      for (int hy = 0; hy < (int) hueH; hy++) {
         float hue = hy / hueH;
         int rgb = hsvToRgb(hue, 1.0F, 1.0F);
         DrawUtil.drawRect(ms, hueX, hueY + hy, hueW, 1.0F,
            new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (int) (255.0F * a)));
      }
      DrawUtil.drawRoundedBorder(ms, hueX, hueY, hueW, hueH, 1.0F, BorderRadius.all(4.0F),
         BORDER.withAlpha(200.0F * a));

      float cursorHY = hueY + this.pickerHue * hueH;
      DrawUtil.drawRect(ms, hueX - 2.0F, cursorHY - 1.0F, hueW + 4.0F, 3.0F,
         new ColorRGBA(255, 255, 255, (int) (255.0F * a)));

      int previewRgb = hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal);
      DrawUtil.drawRoundedRect(ms, hueX + hueW + 12.0F, svY, 28.0F, 28.0F,
         BorderRadius.all(6.0F),
         new ColorRGBA((previewRgb >> 16) & 0xFF, (previewRgb >> 8) & 0xFF, previewRgb & 0xFF, (int) (255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, hueX + hueW + 12.0F, svY, 28.0F, 28.0F,
         1.0F, BorderRadius.all(6.0F), BORDER.withAlpha(200.0F * a));

      float applyY = svY + svH + 12.0F;
      float applyW = 100.0F;
      float applyH = 28.0F;
      float applyX = x + w - applyW - 16.0F;
      boolean applyHover = mx >= applyX && mx <= applyX + applyW && my >= applyY && my <= applyY + applyH;
      DrawUtil.drawRoundedRect(ms, applyX, applyY, applyW, applyH, BorderRadius.all(8.0F),
         applyHover ? accent.brighter(0.12F).withAlpha(255.0F * a) : accent.withAlpha(220.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Применить",
         applyX + applyW / 2.0F - Fonts.SEMIBOLD.getWidth("Применить", FONT_SM) / 2.0F,
         applyY + (applyH - 7.0F) / 2.0F,
         new ColorRGBA(255, 255, 255, (int) (255.0F * a)));

      float cancelW = 80.0F;
      float cancelX = applyX - cancelW - 8.0F;
      boolean cancelHover = mx >= cancelX && mx <= cancelX + cancelW && my >= applyY && my <= applyY + applyH;
      DrawUtil.drawRoundedRect(ms, cancelX, applyY, cancelW, applyH, BorderRadius.all(8.0F),
         cancelHover ? new ColorRGBA(255, 80, 80, (int) (30.0F * a)) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Отмена",
         cancelX + cancelW / 2.0F - Fonts.SEMIBOLD.getWidth("Отмена", FONT_SM) / 2.0F,
         applyY + (applyH - 7.0F) / 2.0F, TEXT.withAlpha(220.0F * a));
   }

   private void renderApiKeyInput(CustomDrawContext ctx, ColorRGBA accent, float a, int mx, int my) {
      var ms = ctx.getMatrices();
      float w = 420.0F;
      float h = 160.0F;
      float x = (this.width - w) / 2.0F;
      float y = (this.height - h) / 2.0F;

      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(0, 0, 0, (int) (150.0F * a)));
      DrawUtil.drawRoundedRect(ms, x, y, w, h, BorderRadius.all(14.0F),
         new ColorRGBA(22, 22, 32, (int) (255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, x, y, w, h, 1.2F, BorderRadius.all(14.0F),
         accent.withAlpha(180.0F * a));

      Provider prov = module.getActiveProvider();
      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "API Key \u2014 " + prov.getDisplayName(),
         x + 18.0F, y + 16.0F, TEXT.withAlpha(255.0F * a));

      float fieldX = x + 18.0F;
      float fieldY = y + 40.0F;
      float fieldW = w - 36.0F;
      float fieldH = 32.0F;
      DrawUtil.drawRoundedRect(ms, fieldX, fieldY, fieldW, fieldH, BorderRadius.all(8.0F),
         new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      DrawUtil.drawRoundedBorder(ms, fieldX, fieldY, fieldW, fieldH, 1.0F, BorderRadius.all(8.0F),
         accent.withAlpha(160.0F * a));

      String display = this.apiKeyInput.isEmpty() ? "" : "*".repeat(Math.min(this.apiKeyInput.length(), 50));
      float fy = fieldY + (fieldH - Fonts.REGULAR.getFont(FONT).height()) / 2.0F;
      if (display.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), "Вставь API-ключ...",
            fieldX + 10.0F, fy, TEXT_DARK.withAlpha(255.0F * a));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), display, fieldX + 10.0F, fy,
            TEXT.withAlpha(245.0F * a));
      }

      float btnY = fieldY + fieldH + 12.0F;
      float btnH = 28.0F;
      float saveW = 100.0F;
      float pasteW = 90.0F;
      float cancelW = 80.0F;
      float totalBtns = saveW + pasteW + cancelW + 16.0F;
      float btnStartX = x + w - totalBtns - 18.0F;

      float saveX = btnStartX;
      boolean saveHover = mx >= saveX && mx <= saveX + saveW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, saveX, btnY, saveW, btnH, BorderRadius.all(8.0F),
         saveHover ? accent.brighter(0.12F).withAlpha(255.0F * a) : accent.withAlpha(220.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Сохранить",
         saveX + saveW / 2.0F - Fonts.SEMIBOLD.getWidth("Сохранить", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F,
         new ColorRGBA(255, 255, 255, (int) (255.0F * a)));

      float pasteX = saveX + saveW + 8.0F;
      boolean pasteHover = mx >= pasteX && mx <= pasteX + pasteW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, pasteX, btnY, pasteW, btnH, BorderRadius.all(8.0F),
         pasteHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      DrawUtil.drawRoundedBorder(ms, pasteX, btnY, pasteW, btnH, 1.0F, BorderRadius.all(8.0F),
         BORDER.withAlpha(150.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Вставить",
         pasteX + pasteW / 2.0F - Fonts.SEMIBOLD.getWidth("Вставить", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F,
         TEXT.withAlpha(220.0F * a));

      float cancelX = pasteX + pasteW + 8.0F;
      boolean cancelHover = mx >= cancelX && mx <= cancelX + cancelW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, cancelX, btnY, cancelW, btnH, BorderRadius.all(8.0F),
         cancelHover ? new ColorRGBA(255, 80, 80, (int) (30.0F * a)) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Отмена",
         cancelX + cancelW / 2.0F - Fonts.SEMIBOLD.getWidth("Отмена", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F,
         TEXT.withAlpha(220.0F * a));
   }

   private void renderModelSelector(CustomDrawContext ctx, ColorRGBA accent, float a, int mx, int my) {
      var ms = ctx.getMatrices();
      float w = 420.0F;
      float h = 380.0F;
      float x = (this.width - w) / 2.0F;
      float y = (this.height - h) / 2.0F;

      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(0, 0, 0, (int) (150.0F * a)));
      DrawUtil.drawRoundedRect(ms, x, y, w, h, BorderRadius.all(14.0F),
         new ColorRGBA(22, 22, 32, (int) (255.0F * a)));
      DrawUtil.drawRoundedBorder(ms, x, y, w, h, 1.2F, BorderRadius.all(14.0F),
         accent.withAlpha(180.0F * a));

      Provider prov = module.getActiveProvider();
      ctx.drawText(Fonts.SEMIBOLD.getFont(9.0F), "Модели \u2014 " + prov.getDisplayName(),
         x + 18.0F, y + 16.0F, TEXT.withAlpha(255.0F * a));

      float listX = x + 18.0F;
      float listY = y + 40.0F;
      float listW = w - 36.0F;
      float listH = h - 100.0F;

      DrawUtil.drawRoundedRect(ms, listX, listY, listW, listH, BorderRadius.all(8.0F),
         new ColorRGBA(255, 255, 255, (int) (6.0F * a)));
      DrawUtil.drawRoundedBorder(ms, listX, listY, listW, listH, 1.0F, BorderRadius.all(8.0F),
         BORDER.withAlpha(120.0F * a));

      if (loadingModels) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT_SM), "Загрузка моделей...",
            listX + listW / 2.0F - Fonts.REGULAR.getWidth("Загрузка моделей...", FONT_SM) / 2.0F,
            listY + listH / 2.0F, TEXT_DIM.withAlpha(160.0F * a));
      } else if (availableModels.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT_SM), "Нет моделей. Введи API-ключ.",
            listX + listW / 2.0F - Fonts.REGULAR.getWidth("Нет моделей. Введи API-ключ.", FONT_SM) / 2.0F,
            listY + listH / 2.0F, TEXT_DIM.withAlpha(160.0F * a));
      } else {
         float itemH = 32.0F;
         float totalH = availableModels.size() * itemH;
         float maxScroll = Math.max(0, totalH - listH);
         this.modelScroll = Math.max(0, Math.min(this.modelScroll, maxScroll));

         ctx.enableScissor((int) listX, (int) listY, (int) (listX + listW), (int) (listY + listH));

         String currentModel = module.getActiveModel();
         float iy = listY - this.modelScroll;
         for (int i = 0; i < availableModels.size(); i++) {
            ZaiAi.ModelInfo model = availableModels.get(i);
            if (iy + itemH >= listY && iy <= listY + listH) {
               boolean isCurrent = model.id().equals(currentModel);
               boolean hover = mx >= listX && mx <= listX + listW && my >= iy && my <= iy + itemH;

               if (isCurrent) {
                  DrawUtil.drawRoundedRect(ms, listX + 2.0F, iy, listW - 4.0F, itemH,
                     BorderRadius.all(6.0F), accent.withAlpha(25.0F * a));
               } else if (hover) {
                  DrawUtil.drawRoundedRect(ms, listX + 2.0F, iy, listW - 4.0F, itemH,
                     BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
               }

               String displayName = model.name().length() > 40 ? model.name().substring(0, 40) + "..." : model.name();
               ctx.drawText(Fonts.REGULAR.getFont(FONT_SM), displayName,
                  listX + 10.0F, iy + 6.0F,
                  isCurrent ? accent.withAlpha(255.0F * a) : TEXT.withAlpha(200.0F * a));

               ctx.drawText(Fonts.REGULAR.getFont(FONT_XS), model.id(),
                  listX + 10.0F, iy + 18.0F,
                  TEXT_DIM.withAlpha(140.0F * a));
            }
            iy += itemH;
         }

         ctx.disableScissor();
      }

      float btnY = y + h - 44.0F;
      float btnH = 28.0F;
      float refreshW = 110.0F;
      float closeW = 80.0F;
      float refreshX = x + 18.0F;
      boolean refreshHover = mx >= refreshX && mx <= refreshX + refreshW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, refreshX, btnY, refreshW, btnH, BorderRadius.all(8.0F),
         refreshHover ? SURFACE.withAlpha(255.0F * a) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      DrawUtil.drawRoundedBorder(ms, refreshX, btnY, refreshW, btnH, 1.0F, BorderRadius.all(8.0F),
         BORDER.withAlpha(150.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Обновить",
         refreshX + refreshW / 2.0F - Fonts.SEMIBOLD.getWidth("Обновить", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F, TEXT.withAlpha(220.0F * a));

      float closeX = x + w - closeW - 18.0F;
      boolean closeHover = mx >= closeX && mx <= closeX + closeW && my >= btnY && my <= btnY + btnH;
      DrawUtil.drawRoundedRect(ms, closeX, btnY, closeW, btnH, BorderRadius.all(8.0F),
         closeHover ? new ColorRGBA(255, 80, 80, (int) (30.0F * a)) : new ColorRGBA(255, 255, 255, (int) (10.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_SM), "Закрыть",
         closeX + closeW / 2.0F - Fonts.SEMIBOLD.getWidth("Закрыть", FONT_SM) / 2.0F,
         btnY + (btnH - 7.0F) / 2.0F, TEXT.withAlpha(220.0F * a));
   }

   private void renderMessages(CustomDrawContext ctx, L L, ColorRGBA accent, float a) {
      Session session = activeSession();
      float lineH = FONT + 4.5F;
      List<Bubble> bubbles = new ArrayList<>();
      for (Session.Msg m : session.history) {
         List<String> lines = this.wrap(m.text(), L.maxTextW, FONT);
         float bh = lines.size() * lineH + 20.0F;
         float bw = Math.min(L.maxTextW + 30.0F, Math.max(80.0F, this.bubbleWidth(lines, L.maxTextW)));
         bubbles.add(new Bubble(m.user(), bw, bh, lines));
      }

      float avail = L.msgBottom - L.msgTop;
      int start = 0;
      float used = 0.0F;
      for (int i = bubbles.size() - 1; i >= 0; i--) {
         used += bubbles.get(i).h + 10.0F;
         if (used > avail) { start = i + 1; break; }
      }

      float y = L.msgBottom;
      for (int i = bubbles.size() - 1; i >= start; i--) {
         Bubble b = bubbles.get(i);
         y -= b.h + 10.0F;
         float bx = b.user() ? (L.chatX + L.chatW - PAD - b.w()) : (L.chatX + PAD);

         ColorRGBA fill = b.user() ? accent.withAlpha(25.0F * a) : SURFACE.withAlpha(200.0F * a);
         ColorRGBA borderC = b.user() ? accent.withAlpha(80.0F * a) : BORDER.withAlpha(100.0F * a);

         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, y, b.w(), b.h(),
            BorderRadius.all(10.0F), fill);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, y, b.w(), b.h(),
            1.0F, BorderRadius.all(10.0F), borderC);

         String label = b.user() ? "you" : module.getActiveProvider().getDisplayName();
         ColorRGBA labelColor = b.user() ? accent.withAlpha(200.0F * a) : new ColorRGBA(140, 180, 255, (int) (220.0F * a));
         ctx.drawText(Fonts.SEMIBOLD.getFont(FONT_XS), label, bx + 10.0F, y + 6.0F, labelColor);

         float ty = y + 18.0F;
         ColorRGBA textColor = TEXT.withAlpha(240.0F * a);
         for (String line : b.lines()) {
            ctx.drawText(Fonts.REGULAR.getFont(FONT), line, bx + 10.0F, ty, textColor);
            ty += lineH;
         }
      }

      if (this.waiting) {
         float bx = L.chatX + PAD;
         float by = L.msgBottom - 26.0F;
         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, 120.0F, 22.0F,
            BorderRadius.all(8.0F), SURFACE.withAlpha(200.0F * a));
         ctx.drawText(Fonts.REGULAR.getFont(FONT_SM), "нейросеть печатает...",
            bx + 12.0F, by + 6.5F,
            new ColorRGBA(140, 180, 255, (int) (255.0F * a)));
      }
   }

   private float bubbleWidth(List<String> lines, float maxTextW) {
      float max = 0.0F;
      for (String l : lines) max = Math.max(max, Fonts.REGULAR.getWidth(l, FONT));
      return max + 28.0F;
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
         float svY = y + 36.0F;
         float svW = 180.0F;
         float svH = 140.0F;
         float hueX = svX + svW + 10.0F;
         float hueY = svY;
         float hueW = 16.0F;
         float hueH = svH;
         float applyY = svY + svH + 12.0F;
         float applyW = 100.0F;
         float applyH = 28.0F;
         float applyX = x + w - applyW - 16.0F;
         float cancelW = 80.0F;
         float cancelX = applyX - cancelW - 8.0F;

         if (button == 0) {
            if (mx >= svX && mx <= svX + svW && my >= svY && my <= svY + svH) {
               this.pickerSat = (float) Math.max(0, Math.min(1, (mx - svX) / svW));
               this.pickerVal = (float) Math.max(0, Math.min(1, 1.0F - (my - svY) / svH));
               this.draggingSV = true;
               return true;
            }
            if (mx >= hueX && mx <= hueX + hueW && my >= hueY && my <= hueY + hueH) {
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
         float w = 420.0F;
         float h = 160.0F;
         float x = (this.width - w) / 2.0F;
         float y = (this.height - h) / 2.0F;
         float fieldX = x + 18.0F;
         float fieldY = y + 40.0F;
         float fieldW = w - 36.0F;
         float fieldH = 32.0F;
         float btnY = fieldY + fieldH + 12.0F;
         float btnH = 28.0F;
         float saveW = 100.0F;
         float pasteW = 90.0F;
         float cancelW = 80.0F;
         float totalBtns = saveW + pasteW + cancelW + 16.0F;
         float btnStartX = x + w - totalBtns - 18.0F;
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

      if (this.showingModelSelector) {
         float w = 420.0F;
         float h = 380.0F;
         float mx2 = (this.width - w) / 2.0F;
         float my2 = (this.height - h) / 2.0F;
         float listX = mx2 + 18.0F;
         float listY = my2 + 40.0F;
         float listW = w - 36.0F;
         float listH = h - 100.0F;
         float btnY = my2 + h - 44.0F;
         float btnH = 28.0F;
         float refreshW = 110.0F;
         float closeW = 80.0F;
         float refreshX = mx2 + 18.0F;
         float closeX = mx2 + w - closeW - 18.0F;

         if (button == 0) {
            if (mx >= refreshX && mx <= refreshX + refreshW && my >= btnY && my <= btnY + btnH) {
               this.loadModels();
               return true;
            }
            if (mx >= closeX && mx <= closeX + closeW && my >= btnY && my <= btnY + btnH) {
               this.showingModelSelector = false;
               return true;
            }
            if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH && !loadingModels) {
               float itemH = 32.0F;
               float iy = listY - this.modelScroll;
               for (ZaiAi.ModelInfo model : availableModels) {
                  if (my >= iy && my <= iy + itemH) {
                     module.setActiveModel(model.id());
                     this.showingModelSelector = false;
                     return true;
                  }
                  iy += itemH;
               }
            }
         }
         if (button == 1 || button == 2) {
            this.showingModelSelector = false;
            return true;
         }
         this.showingModelSelector = false;
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
         float itemH = 34.0F;
         float itemW = L.sidebarW - 24.0F;
         float x = L.px + 12.0F;
         float y = L.newSessionY + L.newSessionH + 8.0F;

         for (int i = 0; i < sessions.size(); i++) {
            if (mx >= x && mx <= x + itemW && my >= y && my <= y + itemH) {
               activeSessionIdx = i;
               return true;
            }
            y += itemH + 3.0F;
         }

         float provY = L.newSessionY + L.newSessionH + 8.0F + (sessions.size() * 37.0F) + 14.0F + 12.0F;
         for (Provider prov : Provider.values()) {
            if (mx >= x && mx <= x + itemW && my >= provY && my <= provY + 28.0F) {
               if (!module.provider.get().equals(prov.getDisplayName())) {
                  module.provider.set(prov.getDisplayName());
                  module.autoDetectModels();
               }
               return true;
            }
            provY += 30.0F;
         }

         float modelBtnY = provY + 6.0F + 12.0F + 28.0F;
         if (mx >= x && mx <= x + itemW && my >= modelBtnY && my <= modelBtnY + 28.0F) {
            this.showingModelSelector = true;
            this.loadModels();
            return true;
         }

         float btnY2 = L.py + L.ph - 140.0F;
         float btnH2 = 30.0F;
         float gap = 5.0F;
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
   public boolean mouseScrolled(double mx, double my, double horizontalAmount, double verticalAmount) {
      if (this.showingModelSelector) {
         this.modelScroll -= (float) (verticalAmount * 20.0F);
         return true;
      }
      return super.mouseScrolled(mx, my, horizontalAmount, verticalAmount);
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.showingColorPicker) {
         float w = 260.0F;
         float h = 220.0F;
         float x = (this.width - w) / 2.0F;
         float y = (this.height - h) / 2.0F;
         float svX = x + 16.0F;
         float svY = y + 36.0F;
         float svW = 180.0F;
         float svH = 140.0F;
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
      module.autoDetectModels();
   }

   private void loadModels() {
      Provider prov = module.getActiveProvider();
      String key = module.getActiveApiKey();
      if (key == null || key.isBlank()) {
         Session session = activeSession();
         session.addMessage(false, "Сначала введи API-ключ для " + prov.getDisplayName() + ".");
         return;
      }
      this.loadingModels = true;
      this.availableModels.clear();
      this.availableModels.addAll(module.getCachedModels());
      module.autoDetectModels();
      ZaiAi.fetchModels(prov, key,
         (models) -> {
            mc.execute(() -> {
               this.availableModels = models;
               this.loadingModels = false;
            });
         },
         (err) -> {
            mc.execute(() -> {
               if (this.availableModels.isEmpty()) {
                  Session session = activeSession();
                  session.addMessage(false, "Ошибка загрузки моделей: " + err);
               }
               this.loadingModels = false;
            });
         });
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
       List<Session.Msg> historyForContext = new ArrayList<>();
       for (Session.Msg msg : session.history) {
          if (msg != session.history.get(session.history.size() - 1)) {
             historyForContext.add(msg);
          }
       }
       this.module.requestScript(session.lastPrompt, historyForContext,
          (text, saved) -> {
             this.waiting = false;
             session.addMessage(false, text);
          },
          (err) -> this.onError(session, err));
    }

   private void onError(Session session, String err) {
      session.retries++;
      if (session.retries <= MAX_RETRIES) {
         session.addMessage(false, "запрос не прошёл \u2014 повтор через 3 сек...");
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
