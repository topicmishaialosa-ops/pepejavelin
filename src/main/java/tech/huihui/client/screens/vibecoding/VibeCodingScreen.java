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
   private static final List<Msg> history = new ArrayList<>();
   private static boolean greeted = false;
   private String input = "";
   private int cursor = 0;
   private boolean focused = true;
   private boolean waiting = false;
   private float openAnim = 0.0F;
   private String lastPrompt = "";
   private int retries = 0;
   private static final int MAX_RETRIES = 4;

   private static final float FONT = 7.0F;
   private static final int MAX_INPUT = 260;

   public VibeCodingScreen() {
      super(Text.literal("VibeCoding"));
      if (!greeted) {
         greeted = true;
         history.add(new Msg(false,
            "Привет! Я сгенерирую скрипт для HuihuiClient.\n"
            + "Опиши что нужно, например: «сделай скрипт на lua, который показывает координаты и FPS».\n"
            + "Код сохраню в файл, а здесь напишу что сделано.\n"
            + "Кнопка «Стактрейс» отправит последнюю ошибку скрипта нейросети для исправления."));
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void close() {
      mc.setScreen(null);
   }

   private record Msg(boolean user, String text) {
   }

   private record Bubble(boolean user, float w, float h, List<String> lines) {
   }

   private Layout layout() {
      float pw = Math.min(880.0F, this.width - 80.0F);
      float ph = Math.min(600.0F, this.height - 80.0F);
      float px = (this.width - pw) / 2.0F;
      float py = (this.height - ph) / 2.0F;
      float headerH = 36.0F;
      float inputH = 36.0F;
      float sendW = 104.0F;
      float gap = 10.0F;
      float inputY = py + ph - inputH - 14.0F;
      float inputX = px + 14.0F;
      float inputW = pw - 28.0F - sendW - gap;
      float sendX = inputX + inputW + gap;
      float sendY = inputY;
      float closeW = 24.0F;
      float closeH = 22.0F;
      float closeX = px + pw - closeW - 12.0F;
      float closeY = py + 7.0F;
      float traceW = 96.0F;
      float traceH = 22.0F;
      float traceX = closeX - traceW - 8.0F;
      float traceY = closeY;
      float clearW = 64.0F;
      float clearH = 22.0F;
      float clearX = traceX - clearW - 8.0F;
      float clearY = closeY;
      float msgTop = py + headerH + 10.0F;
      float msgBottom = inputY - 10.0F;
      float maxTextW = pw - 150.0F;
      return new Layout(px, py, pw, ph, headerH, inputX, inputY, inputW, inputH,
         sendX, sendY, sendW, inputH, closeX, closeY, closeW, closeH,
         traceX, traceY, traceW, traceH, clearX, clearY, clearW, clearH, msgTop, msgBottom, maxTextW);
   }

   private record Layout(float px, float py, float pw, float ph, float headerH,
                         float inputX, float inputY, float inputW, float inputH,
                         float sendX, float sendY, float sendW, float sendH,
                         float closeX, float closeY, float closeW, float closeH,
                         float traceX, float traceY, float traceW, float traceH,
                         float clearX, float clearY, float clearW, float clearH,
                         float msgTop, float msgBottom, float maxTextW) {
      boolean hitClose(double mx, double my) {
         return mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH;
      }

      boolean hitSend(double mx, double my) {
         return mx >= sendX && mx <= sendX + sendW && my >= sendY && my <= sendY + sendH;
      }

      boolean hitInput(double mx, double my) {
         return mx >= inputX && mx <= inputX + inputW && my >= inputY && my <= inputY + inputH;
      }

      boolean hitTrace(double mx, double my) {
         return mx >= traceX && mx <= traceX + traceW && my >= traceY && my <= traceY + traceH;
      }

      boolean hitClear(double mx, double my) {
         return mx >= clearX && mx <= clearX + clearW && my >= clearY && my <= clearY + clearH;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
      super.render(context, mouseX, mouseY, tickDelta);
      this.openAnim += (1.0F - this.openAnim) * 0.18F;
      if (this.openAnim > 0.997F) {
         this.openAnim = 1.0F;
      }
      float a = this.openAnim;
      Layout L = this.layout();
      ColorRGBA accent = this.module.accent.getColor();

      CustomDrawContext ctx = CustomDrawContext.of(context);
      var ms = ctx.getMatrices();

      // затемнение фона
      DrawUtil.drawRect(ms, 0.0F, 0.0F, this.width, this.height, new ColorRGBA(6, 6, 10, (int) (175.0F * a)));

      // панель (стекло + blur)
      DrawUtil.drawBlurHud(ms, L.px - 3.0F, L.py - 3.0F, L.pw + 6.0F, L.ph + 6.0F, 16.0F,
         BorderRadius.all(16.0F), new ColorRGBA(255, 255, 255, 38));
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.pw, L.ph, BorderRadius.all(16.0F),
         new ColorRGBA(17, 17, 24, (int) (240.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.px, L.py, L.pw, L.ph, 1.2F, BorderRadius.all(16.0F),
         accent.withAlpha(150.0F * a));

      // шапка (градиент акцента)
      DrawUtil.drawRoundedRect(ms, L.px, L.py, L.pw, L.headerH, BorderRadius.all(16.0F),
         accent.withAlpha(190.0F * a), new ColorRGBA(22, 22, 30, 0), new ColorRGBA(22, 22, 30, 0),
         accent.brighter(0.28F).withAlpha(120.0F * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(8.5F), "VibeCoding", L.px + 16.0F, L.py + L.headerH / 2.0F - 4.0F,
         new ColorRGBA(255, 255, 255, (int) (255.0F * a)));
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), "чат с нейросетью Z.AI · генерация скриптов",
         L.px + 92.0F, L.py + L.headerH / 2.0F - 1.5F, new ColorRGBA(175, 175, 190, (int) (255.0F * a)));

      // кнопка закрытия
      boolean closeHover = L.hitClose(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.closeX, L.closeY, L.closeW, L.closeH, BorderRadius.all(6.0F),
         closeHover ? accent.withAlpha(160.0F * a) : new ColorRGBA(255, 255, 255, (int) (22.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(8.0F), "✕", L.closeX + L.closeW / 2.0F - 4.0F, L.closeY + 6.0F,
         new ColorRGBA(235, 235, 245, (int) (255.0F * a)));

      // кнопка «Стактрейс»
      boolean traceHover = L.hitTrace(mouseX, mouseY) && !this.waiting;
      DrawUtil.drawRoundedRect(ms, L.traceX, L.traceY, L.traceW, L.traceH, BorderRadius.all(6.0F),
         traceHover ? accent.brighter(0.18F).withAlpha(220.0F * a) : new ColorRGBA(255, 255, 255, (int) (26.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.traceX, L.traceY, L.traceW, L.traceH, 1.0F, BorderRadius.all(6.0F),
         accent.withAlpha((this.waiting ? 60.0F : 150.0F) * a));
      ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Стактрейс", L.traceX + 8.0F, L.traceY + 7.0F,
         new ColorRGBA(225, 228, 245, (int) (255.0F * a)));

      // кнопка «Очистить»
      boolean clearHover = L.hitClear(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.clearX, L.clearY, L.clearW, L.clearH, BorderRadius.all(6.0F),
         clearHover ? new ColorRGBA(255, 90, 90, (int) (60.0F * a)) : new ColorRGBA(255, 255, 255, (int) (16.0F * a)));
      DrawUtil.drawRoundedBorder(ms, L.clearX, L.clearY, L.clearW, L.clearH, 1.0F, BorderRadius.all(6.0F),
         new ColorRGBA(255, 120, 120, (int) (150.0F * a)));
      ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "Очистить", L.clearX + 8.0F, L.clearY + 7.0F,
         new ColorRGBA(240, 200, 200, (int) (255.0F * a)));

      // сообщения
      this.renderMessages(ctx, L, accent, a, mouseX, mouseY);

      // поле ввода
      boolean inputHover = L.hitInput(mouseX, mouseY);
      DrawUtil.drawRoundedRect(ms, L.inputX, L.inputY, L.inputW, L.inputH, BorderRadius.all(9.0F),
         new ColorRGBA(255, 255, 255, (int) ((this.focused ? 16.0F : 9.0F) * a)));
      DrawUtil.drawRoundedBorder(ms, L.inputX, L.inputY, L.inputW, L.inputH, 1.0F, BorderRadius.all(9.0F),
         this.focused ? accent.withAlpha(200.0F * a) : new ColorRGBA(255, 255, 255, (int) (32.0F * a)));

      String shown = this.input;
      int start = 0;
      while (start < shown.length() && Fonts.REGULAR.getWidth(shown.substring(start), FONT) > L.inputW - 16.0F) {
         start++;
      }
      String visible = shown.substring(start);
      float iy = L.inputY + (L.inputH - Fonts.REGULAR.getFont(FONT).height()) / 2.0F;
      if (visible.isEmpty()) {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), "Напиши запрос, например: сделай скрипт на lua…",
            L.inputX + 8.0F, iy, new ColorRGBA(150, 150, 165, (int) (255.0F * a)));
      } else {
         ctx.drawText(Fonts.REGULAR.getFont(FONT), visible, L.inputX + 8.0F, iy,
            new ColorRGBA(255, 255, 255, (int) (245.0F * a)));
      }
      if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
         int ci = Math.max(start, Math.min(this.cursor, shown.length()));
         float cx = L.inputX + 8.0F + Fonts.REGULAR.getWidth(shown.substring(start, ci), FONT);
         DrawUtil.drawLine(ms, new net.minecraft.util.math.Vec2f(cx, L.inputY + 7.0F),
            new net.minecraft.util.math.Vec2f(cx, L.inputY + L.inputH - 7.0F),
            new ColorRGBA(255, 255, 255, (int) (220.0F * a)));
      }

      // кнопка отправки
      boolean sendHover = L.hitSend(mouseX, mouseY) && !this.waiting;
      DrawUtil.drawRoundedRect(ms, L.sendX, L.sendY, L.sendW, L.sendH, BorderRadius.all(9.0F),
         sendHover ? accent.brighter(0.18F).withAlpha(255.0F * a) : accent.withAlpha(220.0F * a));
      String sendLabel = this.waiting ? "…" : "Отправить";
      float sw = Fonts.SEMIBOLD.getWidth(sendLabel, 6.5F);
      ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), sendLabel, L.sendX + L.sendW / 2.0F - sw / 2.0F,
         L.sendY + L.sendH / 2.0F - 3.0F, new ColorRGBA(255, 255, 255, (int) (255.0F * a)));
   }

   private void renderMessages(CustomDrawContext ctx, Layout L, ColorRGBA accent, float a, int mx, int my) {
      float lineH = FONT + 4.0F;
      List<Bubble> bubbles = new ArrayList<>();
      for (Msg m : history) {
         List<String> lines = this.wrap(m.text, L.maxTextW, FONT);
         float bh = lines.size() * lineH + 14.0F;
         float bw = Math.min(L.maxTextW + 24.0F, Math.max(60.0F, this.bubbleWidth(lines, L.maxTextW)));
         bubbles.add(new Bubble(m.user, bw, bh, lines));
      }

      float avail = L.msgBottom - L.msgTop;
      int start = 0;
      float used = 0.0F;
      for (int i = bubbles.size() - 1; i >= 0; i--) {
         used += bubbles.get(i).h + 8.0F;
         if (used > avail) {
            start = i + 1;
            break;
         }
      }

      float y = L.msgBottom;
      for (int i = bubbles.size() - 1; i >= start; i--) {
         Bubble b = bubbles.get(i);
         y -= b.h + 8.0F;
         float bx = b.user ? (L.px + L.pw - 14.0F - b.w) : (L.px + 14.0F);
         ColorRGBA fill = b.user ? accent.withAlpha(42.0F * a) : new ColorRGBA(255, 255, 255, (int) (11.0F * a));
         ColorRGBA border = b.user ? accent.withAlpha(150.0F * a) : new ColorRGBA(255, 255, 255, (int) (30.0F * a));
         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, y, b.w, b.h, BorderRadius.all(10.0F), fill);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), bx, y, b.w, b.h, 1.0F, BorderRadius.all(10.0F), border);
         ctx.drawText(Fonts.REGULAR.getFont(5.0F), b.user ? "вы" : "Z.AI", bx + 8.0F, y + 4.0F,
            b.user ? accent.withAlpha(235.0F * a) : new ColorRGBA(150, 200, 255, (int) (255.0F * a)));
         float ty = y + 12.0F;
         ColorRGBA textColor = new ColorRGBA(232, 232, 242, (int) (255.0F * a));
         for (String line : b.lines) {
            ctx.drawText(Fonts.REGULAR.getFont(FONT), line, bx + 8.0F, ty, textColor);
            ty += lineH;
         }
      }

      if (this.waiting) {
         float bx = L.px + 14.0F;
         float by = L.msgBottom - 22.0F;
         DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, 86.0F, 18.0F, BorderRadius.all(9.0F),
            new ColorRGBA(255, 255, 255, (int) (11.0F * a)));
         ctx.drawText(Fonts.REGULAR.getFont(5.5F), "нейросеть печатает…", bx + 8.0F, by + 5.5F,
            new ColorRGBA(150, 200, 255, (int) (255.0F * a)));
      }
   }

   private float bubbleWidth(List<String> lines, float maxTextW) {
      float max = 0.0F;
      for (String l : lines) {
         max = Math.max(max, Fonts.REGULAR.getWidth(l, FONT));
      }
      return max + 24.0F;
   }

   private List<String> wrap(String text, float maxW, float size) {
      List<String> out = new ArrayList<>();
      for (String paragraph : text.split("\n")) {
         if (paragraph.isEmpty()) {
            out.add("");
            continue;
         }
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
      Layout L = this.layout();
      if (button == 0 && L.hitClose(mx, my)) {
         this.close();
         return true;
      }
      if (button == 0 && L.hitSend(mx, my) && !this.waiting) {
         this.send();
         return true;
      }
      if (button == 0 && L.hitTrace(mx, my) && !this.waiting) {
         this.sendTrace();
         return true;
      }
      if (button == 0 && L.hitClear(mx, my)) {
         history.clear();
         return true;
      }
      this.focused = L.hitInput(mx, my);
      return true;
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused || this.waiting) {
         return false;
      }
      if (chr < 32) {
         return false;
      }
      if (this.input.length() >= MAX_INPUT) {
         return true;
      }
      this.input = this.input.substring(0, this.cursor) + chr + this.input.substring(this.cursor);
      this.cursor++;
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.focused && (keyCode == 257 || keyCode == 335)) {
         this.send();
         return true;
      }
      if (keyCode == 256) {
         if (this.focused) {
            this.focused = false;
         } else {
            this.close();
         }
         return true;
      }
      if (!this.focused) {
         return false;
      }
      switch (keyCode) {
         case 259:
            if (this.cursor > 0) {
               this.input = this.input.substring(0, this.cursor - 1) + this.input.substring(this.cursor);
               this.cursor--;
            }
            return true;
         case 261:
            if (this.cursor < this.input.length()) {
               this.input = this.input.substring(0, this.cursor) + this.input.substring(this.cursor + 1);
            }
            return true;
         case 263:
            if (this.cursor > 0) {
               this.cursor--;
            }
            return true;
         case 262:
            if (this.cursor < this.input.length()) {
               this.cursor++;
            }
            return true;
         case 268:
            this.cursor = 0;
            return true;
         case 269:
            this.cursor = this.input.length();
            return true;
         default:
            return false;
      }
   }

   private void send() {
      String prompt = this.input.trim();
      if (prompt.isEmpty() || this.waiting) {
         return;
      }
      history.add(new Msg(true, prompt));
      this.input = "";
      this.cursor = 0;
      this.lastPrompt = prompt;
      this.retries = 0;
      this.doRequest();
   }

   private void doRequest() {
      this.waiting = true;
      this.module.requestScript(this.lastPrompt,
         (text, saved) -> {
            this.waiting = false;
            history.add(new Msg(false, text));
         },
         (err) -> this.onError(err));
   }

   private void onError(String err) {
      this.retries++;
      if (this.retries <= MAX_RETRIES) {
         history.add(new Msg(false, "запрос не прошёл — повтор через 3 сек…"));
         final String prompt = this.lastPrompt;
         new Thread(() -> {
            try {
               Thread.sleep(3000L);
            } catch (InterruptedException ignored) {
            }
            mc.execute(() -> {
               if (mc.currentScreen == VibeCodingScreen.this) {
                  this.doRequest();
               }
            });
         }, "VibeCoding-Retry").start();
      } else {
         this.waiting = false;
         history.add(new Msg(false, "Ошибка: " + err));
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
         ArrayListDeque<String> history = chatHud.getMessageHistory();
         if (history == null || history.isEmpty()) {
            return null;
         }
         List<String> all = new ArrayList<>(history);
         int n = all.size();
         int from = Math.max(0, n - 400);
         List<String> strs = all.subList(from, n);
         int idx = -1;
         String low;
         for (int i = strs.size() - 1; i >= 0; i--) {
            low = strs.get(i).toLowerCase(java.util.Locale.ROOT);
            for (String m : ERR_MARKERS) {
               if (low.contains(m)) {
                  idx = i;
                  break;
               }
            }
            if (idx >= 0) {
               break;
            }
         }
         if (idx < 0) {
            return null;
         }
         StringBuilder sb = new StringBuilder();
         sb.append(strs.get(idx));
         int taken = 1;
         for (int i = idx + 1; i < strs.size() && taken < 40; i++) {
            String s = strs.get(i);
            if (isContinuation(s)) {
               sb.append('\n').append(s);
               taken++;
            } else {
               break;
            }
         }
         return sb.toString().trim();
      } catch (Exception e) {
         return null;
      }
   }

   private static boolean isContinuation(String s) {
      if (s == null || s.isEmpty()) {
         return false;
      }
      String t = s.trim();
      if (t.isEmpty()) {
         return false;
      }
      String low = t.toLowerCase(java.util.Locale.ROOT);
      if (s.startsWith(" ") || s.startsWith("\t")) {
         return true;
      }
      return low.contains("at ") && (low.contains(".java:") || low.contains(".lua:"))
         || low.contains("caused by") || low.contains("luaerror")
         || low.contains("stack traceback") || low.contains("attempt to")
         || low.contains(".java:") || low.contains(".lua:");
   }

   private void sendTrace() {
      if (this.waiting) {
         return;
      }
      String err = null;
      String lang = "?";
      ScriptErrorCapture.Entry last = ScriptErrorCapture.getLast();
      if (last != null) {
         err = last.full();
         lang = last.lang;
      } else {
         err = captureLastError();
      }
      if (err == null || err.isBlank()) {
         history.add(new Msg(false,
            "Ошибок не найдено. Загрузи проблемный скрипт (чтобы появилась ошибка), затем нажми «Стактрейс»."));
         return;
      }
      history.add(new Msg(true, "стактрейс → ИИ"));
      this.lastPrompt = "Исправь ошибку в моём скрипте (" + lang + "). Вот сообщение об ошибке:\n\n"
         + err + "\n\nВерни ТОЛЬКО исправленный код в блоке ``` с указанием языка и имени файла "
         + "(комментарий '-- name:' для lua/json или '// name:' для java) в первой строке, как в документации. "
         + "Не добавляй пояснений вне блока кода.";
      this.retries = 0;
      this.doRequest();
   }
}
