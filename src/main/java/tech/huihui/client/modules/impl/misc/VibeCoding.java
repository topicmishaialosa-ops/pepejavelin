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
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.script.ScriptManager;
import tech.huihui.client.screens.vibecoding.Session;
import tech.huihui.utility.ai.ZaiAi;
import tech.huihui.utility.ai.ZaiAi.Provider;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "VibeCoding",
   category = Category.MISC,
   description = "Чат с нейросетью для генерации скриптов (lua/java/json)"
)
public final class VibeCoding extends Module {

   public static final VibeCoding INSTANCE = new VibeCoding();

   public final ModeSetting provider = new ModeSetting("Провайдер",
      Provider.ZAI.getDisplayName(),
      Provider.ZAI.getDisplayName(),
      Provider.GOOGLE.getDisplayName(),
      Provider.OPENROUTER.getDisplayName(),
      Provider.XAI.getDisplayName(),
      Provider.KIMI.getDisplayName());

   public final StringSetting apiKeyZai = new StringSetting("API-ключ Z.AI", "");
   public final StringSetting apiKeyGoogle = new StringSetting("API-ключ Google", "");
   public final StringSetting apiKeyOpenRouter = new StringSetting("API-ключ OpenRouter", "");
   public final StringSetting apiKeyXai = new StringSetting("API-ключ xAI", "");
   public final StringSetting apiKeyKimi = new StringSetting("API-ключ Kimi", "");

   public final StringSetting modelZai = new StringSetting("Модель Z.AI", "glm-4.7-flash");
   public final StringSetting modelGoogle = new StringSetting("Модель Google", "gemini-2.0-flash");
   public final StringSetting modelOpenRouter = new StringSetting("Модель OpenRouter", "anthropic/claude-sonnet-4");
   public final StringSetting modelXai = new StringSetting("Модель xAI", "grok-3-mini");
   public final StringSetting modelKimi = new StringSetting("Модель Kimi", "moonshot-v1-8k");

   public final BooleanSetting autoLoad = new BooleanSetting("Авто-загрузка скрипта", true);
   public final ModeSetting screenStyle = new ModeSetting("Стиль экрана", "Стиль 1", "Стиль 2");
   public final ColorSetting accent = new ColorSetting("Акцент", new ColorRGBA(124, 92, 255, 255));
   public final ButtonSetting openChat = new ButtonSetting("Открыть чат с нейросетью", VibeCoding::openScreen);

   private static final Pattern CODE_FENCE = Pattern.compile("```([a-zA-Z0-9]*)\\R([\\s\\S]*?)```");
   private static final Pattern NAME_HINT = Pattern.compile("(?m)^\\s*(?://|--)\\s*name:\\s*([A-Za-z0-9_\\-]+)");

   private String docsCache;
   private volatile List<ZaiAi.ModelInfo> cachedModels = new ArrayList<>();
   private volatile boolean detectingModels = false;

   private VibeCoding() {
   }

   public static void openScreen() {
      if (VibeCoding.INSTANCE.screenStyle.is("Стиль 2")) {
         tech.huihui.client.screens.vibecoding.VibeCodingScreen2.open();
      } else {
         tech.huihui.client.screens.vibecoding.VibeCodingScreen.open();
      }
   }

   public Provider getActiveProvider() {
      String name = provider.get();
      for (Provider p : Provider.values()) {
         if (p.getDisplayName().equals(name)) {
            return p;
         }
      }
      return Provider.GOOGLE;
   }

   public String getActiveApiKey() {
      return switch (getActiveProvider()) {
         case ZAI -> apiKeyZai.getValue();
         case GOOGLE -> apiKeyGoogle.getValue();
         case OPENROUTER -> apiKeyOpenRouter.getValue();
         case XAI -> apiKeyXai.getValue();
         case KIMI -> apiKeyKimi.getValue();
      };
   }

   public String getActiveModel() {
      return switch (getActiveProvider()) {
         case ZAI -> modelZai.getValue();
         case GOOGLE -> modelGoogle.getValue();
         case OPENROUTER -> modelOpenRouter.getValue();
         case XAI -> modelXai.getValue();
         case KIMI -> modelKimi.getValue();
      };
   }

   public void setActiveModel(String modelId) {
      if (modelId == null) return;
      switch (getActiveProvider()) {
         case ZAI -> modelZai.setValue(modelId);
         case GOOGLE -> modelGoogle.setValue(modelId);
         case OPENROUTER -> modelOpenRouter.setValue(modelId);
         case XAI -> modelXai.setValue(modelId);
         case KIMI -> modelKimi.setValue(modelId);
      }
   }

   public List<ZaiAi.ModelInfo> getCachedModels() {
      return cachedModels;
   }

   public boolean isDetectingModels() {
      return detectingModels;
   }

   public void autoDetectModels() {
      Provider prov = getActiveProvider();
      String key = getActiveApiKey();
      if (key == null || key.isBlank()) {
         cachedModels = new ArrayList<>();
         return;
      }
      if (detectingModels) return;
      detectingModels = true;
      ZaiAi.fetchModels(prov, key,
         (models) -> mc.execute(() -> {
            cachedModels = models;
            detectingModels = false;
            if (models.isEmpty()) return;
            String current = getActiveModel();
            boolean hasCurrent = current != null && !current.isBlank();
            if (!hasCurrent) {
               String preferred = pickPreferredModel(models);
               if (preferred != null) setActiveModel(preferred);
            }
         }),
         (err) -> mc.execute(() -> {
            detectingModels = false;
         }));
   }

   private static String pickPreferredModel(List<ZaiAi.ModelInfo> models) {
      String current = "";
      String current2 = "";
      for (ZaiAi.ModelInfo m : models) {
         String id = m.id().toLowerCase();
         if (current.isEmpty() && (id.contains("flash") || id.contains("mini") || id.contains("sonnet"))) {
            current = m.id();
         } else if (id.contains("pro") || id.contains("opus") || id.contains("reasoning")) {
            current2 = m.id();
         }
      }
      if (!current.isEmpty()) return current;
      if (!current2.isEmpty()) return current2;
      return models.get(0).id();
   }

    public void requestScript(String prompt, List<Session.Msg> history, BiConsumer<String, List<String>> onResult, Consumer<String> onError) {
       Provider prov = getActiveProvider();
       String key = getActiveApiKey();
       if (key == null || key.isBlank()) {
          onError.accept("Введите API-ключ для " + prov.getDisplayName() + " в настройках VibeCoding");
          return;
       }
       String model = getActiveModel();
       String lang = this.detectLang(prompt);
       String system = this.buildSystemPrompt();

       List<ZaiAi.HistoryMsg> aiHistory = new ArrayList<>();
       if (history != null) {
          for (Session.Msg msg : history) {
             aiHistory.add(new ZaiAi.HistoryMsg(msg.user(), msg.text()));
          }
       }

       ZaiAi.ask(prov, key, model, system, prompt, aiHistory, false, 4096, 0.4, new ZaiAi.Callback() {
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
         + "описанные в ней методы API (api:method(), ctx:method() для Lua; api.method(), ctx.method() для Java), правильные категории модулей и форматы.\n\n"
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
