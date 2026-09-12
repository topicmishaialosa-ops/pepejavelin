package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.ai.ZaiAi;
import org.blade.utils.text.ChatUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VibeCoding extends Feature {
    public static final VibeCoding INSTANCE = new VibeCoding();

    public final ModeSetting provider = register(new ModeSetting("Провайдер",
            ZaiAi.Provider.ZAI.getDisplayName(),
            ZaiAi.Provider.ZAI.getDisplayName(),
            ZaiAi.Provider.GOOGLE.getDisplayName(),
            ZaiAi.Provider.OPENROUTER.getDisplayName(),
            ZaiAi.Provider.XAI.getDisplayName(),
            ZaiAi.Provider.KIMI.getDisplayName()));

    public final TextSetting apiKeyZai = register(new TextSetting("API-ключ Z.AI", "", 200).secret());
    public final TextSetting apiKeyGoogle = register(new TextSetting("API-ключ Google", "", 200).secret());
    public final TextSetting apiKeyOpenRouter = register(new TextSetting("API-ключ OpenRouter", "", 200).secret());
    public final TextSetting apiKeyXai = register(new TextSetting("API-ключ xAI", "", 200).secret());
    public final TextSetting apiKeyKimi = register(new TextSetting("API-ключ Kimi", "", 200).secret());

    public final TextSetting modelZai = register(new TextSetting("Модель Z.AI", "glm-4.7-flash", 100));
    public final TextSetting modelGoogle = register(new TextSetting("Модель Google", "gemini-2.0-flash", 100));
    public final TextSetting modelOpenRouter = register(new TextSetting("Модель OpenRouter", "anthropic/claude-sonnet-4", 100));
    public final TextSetting modelXai = register(new TextSetting("Модель xAI", "grok-3-mini", 100));
    public final TextSetting modelKimi = register(new TextSetting("Модель Kimi", "moonshot-v1-8k", 100));

    public final BooleanSetting autoLoad = register(new BooleanSetting("Авто-загрузка скрипта", true));
    public final ModeSetting screenStyle = register(new ModeSetting("Стиль экрана", "Стиль 1", "Стиль 1", "Стиль 2"));
    public final ColorSetting accent = register(new ColorSetting("Акцент", ColorUtil.rgb(124, 92, 255)));
    public final ButtonSetting openChat = register(new ButtonSetting("Открыть чат с нейросетью", "Открыть", VibeCodingScreen::open));

    private static final Pattern CODE_FENCE = Pattern.compile("```([a-zA-Z0-9]*)\\R([\\s\\S]*?)```");
    private static final Pattern NAME_HINT = Pattern.compile("(?m)^\\s*(?://|--)\\s*name:\\s*([A-Za-z0-9_\\-]+)");

    private String docsCache;
    private volatile List<ZaiAi.ModelInfo> cachedModels = new ArrayList<>();
    private volatile boolean detectingModels = false;

    private VibeCoding() {
        super("VibeCoding", "Чат с нейросетью для генерации скриптов (lua/java/json)", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    public static void openScreen() {
        if (VibeCoding.INSTANCE.screenStyle.is("Стиль 2")) {
            VibeCodingScreen2.open();
        } else {
            VibeCodingScreen.open();
        }
    }

    public ZaiAi.Provider getActiveProvider() {
        String name = this.provider.getValue();
        for (ZaiAi.Provider p : ZaiAi.Provider.values()) {
            if (p.getDisplayName().equals(name)) {
                return p;
            }
        }
        return ZaiAi.Provider.GOOGLE;
    }

    public String getActiveApiKey() {
        return switch (this.getActiveProvider()) {
            case ZAI -> this.apiKeyZai.getValue();
            case GOOGLE -> this.apiKeyGoogle.getValue();
            case OPENROUTER -> this.apiKeyOpenRouter.getValue();
            case XAI -> this.apiKeyXai.getValue();
            case KIMI -> this.apiKeyKimi.getValue();
        };
    }

    public String getActiveModel() {
        return switch (this.getActiveProvider()) {
            case ZAI -> this.modelZai.getValue();
            case GOOGLE -> this.modelGoogle.getValue();
            case OPENROUTER -> this.modelOpenRouter.getValue();
            case XAI -> this.modelXai.getValue();
            case KIMI -> this.modelKimi.getValue();
        };
    }

    public void setActiveModel(String modelId) {
        if (modelId == null) {
            return;
        }
        switch (this.getActiveProvider()) {
            case ZAI -> this.modelZai.setValue(modelId);
            case GOOGLE -> this.modelGoogle.setValue(modelId);
            case OPENROUTER -> this.modelOpenRouter.setValue(modelId);
            case XAI -> this.modelXai.setValue(modelId);
            case KIMI -> this.modelKimi.setValue(modelId);
        }
    }

    public List<ZaiAi.ModelInfo> getCachedModels() {
        return this.cachedModels;
    }

    public boolean isDetectingModels() {
        return this.detectingModels;
    }

    public void autoDetectModels() {
        Minecraft mc = Minecraft.getInstance();
        ZaiAi.Provider prov = this.getActiveProvider();
        String key = this.getActiveApiKey();
        if (key == null || key.isBlank()) {
            this.cachedModels = new ArrayList<>();
            return;
        }
        if (this.detectingModels) {
            return;
        }
        this.detectingModels = true;
        ZaiAi.fetchModels(prov, key,
                (models) -> mc.execute(() -> {
                    this.cachedModels = models;
                    this.detectingModels = false;
                    if (models.isEmpty()) {
                        return;
                    }
                    String current = this.getActiveModel();
                    boolean hasCurrent = current != null && !current.isBlank();
                    if (!hasCurrent) {
                        String preferred = pickPreferredModel(models);
                        if (preferred != null) {
                            this.setActiveModel(preferred);
                        }
                    }
                }),
                (err) -> mc.execute(() -> this.detectingModels = false));
    }

    private static String pickPreferredModel(List<ZaiAi.ModelInfo> models) {
        String current = "";
        String current2 = "";
        for (ZaiAi.ModelInfo m : models) {
            String id = m.id().toLowerCase(java.util.Locale.ROOT);
            if (current.isEmpty() && (id.contains("flash") || id.contains("mini") || id.contains("sonnet"))) {
                current = m.id();
            } else if (id.contains("pro") || id.contains("opus") || id.contains("reasoning")) {
                current2 = m.id();
            }
        }
        if (!current.isEmpty()) {
            return current;
        }
        if (!current2.isEmpty()) {
            return current2;
        }
        return models.get(0).id();
    }

    public void requestScript(String prompt, List<VibeSession.Msg> history, BiConsumer<String, List<String>> onResult, Consumer<String> onError) {
        ZaiAi.Provider prov = this.getActiveProvider();
        String key = this.getActiveApiKey();
        if (key == null || key.isBlank()) {
            onError.accept("Введите API-ключ для " + prov.getDisplayName() + " в настройках VibeCoding");
            return;
        }
        String model = this.getActiveModel();
        String lang = this.detectLang(prompt);
        String system = this.buildSystemPrompt();

        List<ZaiAi.HistoryMsg> aiHistory = new ArrayList<>();
        if (history != null) {
            for (VibeSession.Msg msg : history) {
                aiHistory.add(new ZaiAi.HistoryMsg(msg.user(), msg.text()));
            }
        }

        ZaiAi.ask(prov, key, model, system, prompt, aiHistory, false, 4096, 0.4, new ZaiAi.Callback() {
            @Override
            public void onResult(String reply) {
                Minecraft.getInstance().execute(() -> VibeCoding.this.handleReply(reply, lang, onResult));
            }

            @Override
            public void onError(String message) {
                Minecraft.getInstance().execute(() -> onError.accept(message));
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
        boolean hasDone = filtered.toLowerCase(java.util.Locale.ROOT).contains("сделано");
        String header = hasDone ? filtered : "сделано. " + filtered;

        StringBuilder sb = new StringBuilder(header);
        if (!saved.isEmpty()) {
            sb.append("\nСохранено (").append(saved.size()).append("):");
            for (String s : saved) {
                sb.append("\n  ").append(s);
            }
        }
        onResult.accept(sb.toString(), saved);

        if (this.autoLoad.getValue() && !saved.isEmpty()) {
            ChatUtil.info("Скрипты сохранены: " + String.join(", ", saved));
        }
    }

    private String detectLang(String msg) {
        String l = msg.toLowerCase(java.util.Locale.ROOT);
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
            String l = fenceLang.toLowerCase(java.util.Locale.ROOT);
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
        return sanitizeName("Vibe" + lang.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + lang.substring(1) + index);
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
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("blade").resolve("scripts").resolve(lang);
            Files.createDirectories(dir);
            Path file = dir.resolve(name + "." + lang);
            Files.writeString(file, code, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            ChatUtil.error("VibeCoding: не удалось сохранить " + lang + "/" + name + ": " + e.getMessage());
            return false;
        }
    }

    private String buildSystemPrompt() {
        if (this.docsCache == null) {
            this.docsCache = this.loadDocs();
        }
        return "Ты — ИИ-ассистент по написанию скриптов для Minecraft клиента Blade. "
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
        try (java.io.InputStream in = VibeCoding.class.getResourceAsStream("/org/blade/SCRIPTS.md")) {
            if (in == null) {
                return "(документация недоступна)";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(ошибка загрузки документации: " + e.getMessage() + ")";
        }
    }
}