package tech.huihui.client.modules.impl.misc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.script.ScriptManager;
import tech.huihui.client.screens.vibecoding.VibeCodingScreen;
import tech.huihui.utility.ai.ZaiAi;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "VibeCoding",
   category = Category.MISC,
   description = "Чат с нейросетью Z.AI для генерации скриптов (lua/java/json)"
)
public final class VibeCoding extends Module {

   public static final VibeCoding INSTANCE = new VibeCoding();

   public final StringSetting apiKey = new StringSetting("API-ключ Z.AI", "");
   public final BooleanSetting autoLoad = new BooleanSetting("Авто-загрузка скрипта", true);
   public final ColorSetting accent = new ColorSetting("Акцент", new ColorRGBA(124, 92, 255, 255));
   public final ButtonSetting openChat = new ButtonSetting("Открыть чат с нейросетью", VibeCodingScreen::open);

   private static final Pattern CODE_FENCE = Pattern.compile("```([a-zA-Z0-9]*)\\R([\\s\\S]*?)```");
   private static final Pattern NAME_HINT = Pattern.compile("(?m)^\\s*(?://|--)\\s*name:\\s*([A-Za-z0-9_\\-]+)");

   private String docsCache;

   private VibeCoding() {
   }

   /**
    * Запрашивает у нейросети скрипт по запросу, сохраняет код в файлы и возвращает
    * отфильтрованный (без кода) текст ответа + список сохранённых файлов.
    * Колбэки вызываются в основном потоке (через mc.execute).
    */
   public void requestScript(String prompt, BiConsumer<String, List<String>> onResult, Consumer<String> onError) {
      String key = this.apiKey.getValue();
      if (key == null || key.isBlank()) {
         onError.accept("Введите API-ключ Z.AI в настройках VibeCoding");
         return;
      }
      String lang = this.detectLang(prompt);
      String system = this.buildSystemPrompt();
      ZaiAi.askMarkdown(key, system, prompt, new ZaiAi.Callback() {
         @Override
         public void onResult(String reply) {
            mc.execute(() -> VibeCoding.this.handleReply(reply, lang, onResult));
         }

         @Override
         public void onError(String message) {
            mc.execute(() -> onError.accept(message));
         }
      });
   }

   private void handleReply(String reply, String lang, BiConsumer<String, List<String>> onResult) {
      List<String> saved = new ArrayList<>();
      if (reply != null && !reply.isBlank()) {
         Matcher fence = CODE_FENCE.matcher(reply);
         while (fence.find()) {
            String fenceLang = fence.group(1);
            String code = fence.group(2);
            String useLang = resolveLang(fenceLang, lang);
            String name = resolveName(code, useLang, saved.size() + 1);
            if (this.saveScript(useLang, name, code)) {
               saved.add(useLang + "/" + name + "." + useLang);
            }
         }
      }

      String filtered = reply == null ? "" : CODE_FENCE.matcher(reply).replaceAll("").trim();
      boolean hasDone = filtered.toLowerCase().contains("сделано");
      String header = hasDone ? filtered : "сделано. " + filtered;

      StringBuilder sb = new StringBuilder(header);
      if (!saved.isEmpty()) {
         sb.append("\nСохранено (").append(saved.size()).append("):");
         for (String s : saved) {
            sb.append("\n  ").append(s);
         }
      }
      onResult.accept(sb.toString(), saved);

      if (this.autoLoad.isEnabled() && !saved.isEmpty()) {
         for (String s : saved) {
            String[] parts = s.split("/");
            String fname = parts[1].substring(0, parts[1].lastIndexOf('.'));
            ScriptManager.load(parts[0], fname);
         }
      }
   }

   private String detectLang(String msg) {
      String l = msg.toLowerCase();
      if (l.contains("java")) {
         return "java";
      }
      if (l.contains("json")) {
         return "json";
      }
      return "lua";
   }

   private static String resolveLang(String fenceLang, String fallback) {
      if (fenceLang != null) {
         String l = fenceLang.toLowerCase();
         if (l.equals("lua") || l.equals("java") || l.equals("json")) {
            return l;
         }
      }
      return fallback;
   }

   private static String resolveName(String code, String lang, int index) {
      Matcher m = NAME_HINT.matcher(code);
      if (m.find()) {
         String raw = m.group(1).trim();
         if (!raw.isEmpty()) {
            return sanitizeName(raw);
         }
      }
      return sanitizeName("Vibe" + lang.substring(0, 1).toUpperCase() + lang.substring(1) + index);
   }

   private static String sanitizeName(String raw) {
      StringBuilder b = new StringBuilder();
      for (char c : raw.toCharArray()) {
         if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
            b.append(c);
         }
      }
      String s = b.toString();
      return s.isEmpty() ? "Script" : s;
   }

   private boolean saveScript(String lang, String name, String code) {
      try {
         Path dir = ScriptManager.getScriptsDir().toPath().resolve(lang);
         Files.createDirectories(dir);
         Path file = dir.resolve(name + "." + lang);
         Files.writeString(file, code, StandardCharsets.UTF_8);
         return true;
      } catch (Exception e) {
         MessageUtil.displayError("VibeCoding: не удалось сохранить " + lang + "/" + name + ": " + e.getMessage());
         return false;
      }
   }

   private String buildSystemPrompt() {
      if (this.docsCache == null) {
         this.docsCache = this.loadDocs();
      }
      return "Ты — ИИ-ассистент по написанию скриптов для Minecraft клиента HuihuiClient. "
         + "Ниже — ПОЛНАЯ ДОКУМЕНТАЦИЯ по системе скриптов клиента (команды, API, языки Java/Lua/JSON, "
         + "RenderContext, слоты инвентаря и т.д.). Строго следуй этой документации: используй только "
         + "описанные в ней методы API (api.*, ctx.*), правильные категории модулей и форматы.\n\n"
         + "ДОКУМЕНТАЦИЯ:\n" + this.docsCache + "\n\n"
         + "ЗАДАЧА: напиши готовый, рабочий скрипт по запросу пользователя на указанном языке (lua/java/json).\n"
         + "ТРЕБОВАНИЯ К ОТВЕТУ:\n"
         + "1) Сначала дай краткое пояснение по-русски (1-3 предложения), что делает скрипт.\n"
         + "2) Затем дай код в блоках ``` с указанием языка (например ```lua). "
         + "В САМОЙ ПЕРВОЙ СТРОКЕ КОДА укажи имя файла комментарием: "
         + "для lua '-- name: ИмяСкрипта', для java '// name: ИмяСкрипта', для json '// name: ИмяСкрипта'. "
         + "Имя только латиницей, цифрами и _.\n"
         + "3) Можно дать несколько блоков кода (несколько файлов), каждый со своим именем.\n"
         + "4) В КОНЦЕ ответа обязательно напиши отдельной строкой слово 'сделано'.\n"
         + "5) Не дублируй код в пояснении. Пояснение и код — на русском.";
   }

   private String loadDocs() {
      try (java.io.InputStream in = VibeCoding.class.getResourceAsStream("/huihui/SCRIPTS.md")) {
         if (in == null) {
            return "(документация недоступна)";
         }
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception e) {
         return "(ошибка загрузки документации: " + e.getMessage() + ")";
      }
   }
}
