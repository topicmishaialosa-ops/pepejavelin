package tech.huihui.utility.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public final class ZaiAi {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "VibeCoding-AI");
        thread.setDaemon(true);
        return thread;
    });

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    public enum Provider {
        ZAI("Z.AI", "https://api.z.ai/api/paas/v4/chat/completions", "glm-4.7-flash"),
        GOOGLE("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", "gemini-2.0-flash"),
        OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1/chat/completions", "anthropic/claude-sonnet-4"),
        XAI("xAI (Grok)", "https://api.x.ai/v1/chat/completions", "grok-3-mini"),
        KIMI("Kimi (Moonshot)", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k");

        private final String displayName;
        private final String endpoint;
        private final String defaultModel;

        Provider(String displayName, String endpoint, String defaultModel) {
            this.displayName = displayName;
            this.endpoint = endpoint;
            this.defaultModel = defaultModel;
        }

        public String getDisplayName() { return displayName; }
        public String getEndpoint() { return endpoint; }
        public String getDefaultModel() { return defaultModel; }
    }

    private ZaiAi() {
    }

    public record HistoryMsg(boolean user, String text) {}

    public static void ask(String apiKey, String system, String user, Callback callback) {
        ask(Provider.GOOGLE, apiKey, null, system, user, callback);
    }

    public static void ask(Provider provider, String apiKey, String model, String system, String user, Callback callback) {
        ask(provider, apiKey, model, system, user, false, 4096, 0.4, callback);
    }

    public static void ask(Provider provider, String apiKey, String model, String system, String user,
                           boolean json, int maxTokens, double temperature, Callback callback) {
        ask(provider, apiKey, model, system, user, null, json, maxTokens, temperature, callback);
    }

    public static void ask(Provider provider, String apiKey, String model, String system, String user,
                           List<HistoryMsg> history, boolean json, int maxTokens, double temperature, Callback callback) {
        if (apiKey == null || apiKey.isBlank()) {
            callback.onError("API-ключ не задан для " + provider.getDisplayName());
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                callback.onResult(requestWithRetry(provider, apiKey, model, system, user, history, json, maxTokens, temperature));
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public static void askStream(Provider provider, String apiKey, String model, String system, String user,
                                 List<HistoryMsg> history, int maxTokens, double temperature, StreamCallback callback) {
        if (apiKey == null || apiKey.isBlank()) {
            callback.onError("API-ключ не задан для " + provider.getDisplayName());
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                requestStreamWithRetry(provider, apiKey, model, system, user, history, maxTokens, temperature, callback);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    private static String requestWithRetry(Provider provider, String apiKey, String model, String system, String user,
                                           List<HistoryMsg> history, boolean json, int maxTokens, double temperature) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return request(provider, apiKey, model, system, user, history, json, maxTokens, temperature);
            } catch (Exception e) {
                lastException = e;
                String msg = e.getMessage();
                boolean retryable = msg != null && (msg.contains("429") || msg.contains("503") || msg.contains("504") || msg.contains("529") || msg.contains("timeout") || msg.contains("TIMED_OUT"));
                if (!retryable || attempt >= MAX_RETRIES) throw e;
                long delay = BASE_DELAY_MS * (1L << attempt) + ThreadLocalRandom.current().nextLong(500);
                Thread.sleep(delay);
            }
        }
        throw lastException;
    }

    private static void requestStreamWithRetry(Provider provider, String apiKey, String model, String system, String user,
                                               List<HistoryMsg> history, int maxTokens, double temperature, StreamCallback callback) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                requestStream(provider, apiKey, model, system, user, history, maxTokens, temperature, callback);
                return;
            } catch (Exception e) {
                lastException = e;
                String msg = e.getMessage();
                boolean retryable = msg != null && (msg.contains("429") || msg.contains("503") || msg.contains("504") || msg.contains("529") || msg.contains("timeout") || msg.contains("TIMED_OUT"));
                if (!retryable || attempt >= MAX_RETRIES) throw e;
                long delay = BASE_DELAY_MS * (1L << attempt) + ThreadLocalRandom.current().nextLong(500);
                Thread.sleep(delay);
            }
        }
        throw lastException;
    }

    private static String request(Provider provider, String apiKey, String model, String system, String user,
                                  List<HistoryMsg> history, boolean json, int maxTokens, double temperature) throws Exception {
        String useModel = (model == null || model.isBlank()) ? provider.getDefaultModel() : model;

        if (provider == Provider.GOOGLE) {
            return requestGoogle(apiKey, useModel, system, user, history, maxTokens, temperature);
        } else {
            return requestOpenAI(provider, apiKey, useModel, system, user, history, json, maxTokens, temperature);
        }
    }

    private static void requestStream(Provider provider, String apiKey, String model, String system, String user,
                                      List<HistoryMsg> history, int maxTokens, double temperature, StreamCallback callback) throws Exception {
        String useModel = (model == null || model.isBlank()) ? provider.getDefaultModel() : model;

        if (provider == Provider.GOOGLE) {
            String result = requestGoogle(apiKey, useModel, system, user, history, maxTokens, temperature);
            callback.onToken(result);
            callback.onDone(result);
        } else {
            requestOpenAIStream(provider, apiKey, useModel, system, user, history, maxTokens, temperature, callback);
        }
    }

    private static String requestGoogle(String apiKey, String model, String system, String user,
                                        List<HistoryMsg> history, int maxTokens, double temperature) throws Exception {
        String urlStr = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(90000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();

        if (system != null && !system.isBlank()) {
            JsonObject sysPart = new JsonObject();
            sysPart.addProperty("text", system);
            JsonObject sysContent = new JsonObject();
            sysContent.addProperty("role", "user");
            JsonArray sysParts = new JsonArray();
            sysParts.add(sysPart);
            sysContent.add("parts", sysParts);
            contents.add(sysContent);

            JsonObject modelPart = new JsonObject();
            modelPart.addProperty("text", "OK");
            JsonObject modelContent = new JsonObject();
            modelContent.addProperty("role", "model");
            JsonArray modelParts = new JsonArray();
            modelParts.add(modelPart);
            modelContent.add("parts", modelParts);
            contents.add(modelContent);
        }

        if (history != null) {
            for (HistoryMsg msg : history) {
                JsonObject part = new JsonObject();
                part.addProperty("text", msg.text());
                JsonObject content = new JsonObject();
                content.addProperty("role", msg.user() ? "user" : "model");
                JsonArray parts = new JsonArray();
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
            }
        }

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", user);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        userContent.add("parts", userParts);
        contents.add(userContent);

        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", maxTokens);
        generationConfig.addProperty("temperature", temperature);
        body.add("generationConfig", generationConfig);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        if (code != 200) {
            String error = read(connection.getErrorStream());
            throw new RuntimeException("Google HTTP " + code + ": " + error);
        }

        JsonObject response = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
        JsonArray candidates = response.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("Google вернул пустой ответ");
        }
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonArray parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            result.append(parts.get(i).getAsJsonObject().get("text").getAsString());
        }
        return result.toString();
    }

    private static String requestOpenAI(Provider provider, String apiKey, String model, String system, String user,
                                        List<HistoryMsg> history, boolean json, int maxTokens, double temperature) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(provider.getEndpoint()).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(90000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        if (provider == Provider.OPENROUTER) {
            connection.setRequestProperty("X-Title", "HuihuiClient VibeCoding");
        }
        connection.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        JsonArray messages = new JsonArray();

        if (system != null && !system.isBlank()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", system);
            messages.add(systemMessage);
        }

        if (history != null) {
            for (HistoryMsg msg : history) {
                JsonObject historyMessage = new JsonObject();
                historyMessage.addProperty("role", msg.user() ? "user" : "assistant");
                historyMessage.addProperty("content", msg.text());
                messages.add(historyMessage);
            }
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", user);
        messages.add(userMessage);

        body.add("messages", messages);
        body.addProperty("temperature", temperature);
        body.addProperty("max_tokens", maxTokens);

        if (provider == Provider.ZAI) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
        }

        if (json) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            body.add("response_format", responseFormat);
        }

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        if (code != 200) {
            String error = read(connection.getErrorStream());
            throw new RuntimeException(provider.getDisplayName() + " HTTP " + code + ": " + error);
        }

        JsonObject response = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
        JsonArray choices = response.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException(provider.getDisplayName() + " вернул пустой ответ");
        }
        JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        return message.get("content").getAsString();
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static void requestOpenAIStream(Provider provider, String apiKey, String model, String system, String user,
                                            List<HistoryMsg> history, int maxTokens, double temperature, StreamCallback callback) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(provider.getEndpoint()).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(120000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        if (provider == Provider.OPENROUTER) {
            connection.setRequestProperty("X-Title", "HuihuiClient VibeCoding");
        }
        connection.setDoOutput(true);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", true);
        JsonArray messages = new JsonArray();

        if (system != null && !system.isBlank()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", system);
            messages.add(systemMessage);
        }

        if (history != null) {
            for (HistoryMsg msg : history) {
                JsonObject historyMessage = new JsonObject();
                historyMessage.addProperty("role", msg.user() ? "user" : "assistant");
                historyMessage.addProperty("content", msg.text());
                messages.add(historyMessage);
            }
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", user);
        messages.add(userMessage);

        body.add("messages", messages);
        body.addProperty("temperature", temperature);
        body.addProperty("max_tokens", maxTokens);

        if (provider == Provider.ZAI) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
        }

        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        if (code != 200) {
            String error = read(connection.getErrorStream());
            throw new RuntimeException(provider.getDisplayName() + " HTTP " + code + ": " + error);
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) break;
                    try {
                        JsonObject chunk = JsonParser.parseString(data).getAsJsonObject();
                        JsonArray choices = chunk.getAsJsonArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject delta = choice.getAsJsonObject("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").getAsString();
                                result.append(content);
                                callback.onToken(content);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        callback.onDone(result.toString());
    }

    public interface Callback {
        void onResult(String reply);
        void onError(String message);
    }

    public interface StreamCallback {
        void onToken(String token);
        void onDone(String fullReply);
        void onError(String message);
    }

    public record ModelInfo(String id, String name, String provider) {}

    public static void fetchModels(Provider provider, String apiKey, java.util.function.Consumer<List<ModelInfo>> onResult, java.util.function.Consumer<String> onError) {
        EXECUTOR.execute(() -> {
            try {
                onResult.accept(fetchModelsSync(provider, apiKey));
            } catch (Exception e) {
                onError.accept(e.getMessage());
            }
        });
    }

    private static List<ModelInfo> fetchModelsSync(Provider provider, String apiKey) throws Exception {
        List<ModelInfo> models = new ArrayList<>();

        if (provider == Provider.GOOGLE) {
            String urlStr = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");

            int code = connection.getResponseCode();
            if (code != 200) {
                throw new RuntimeException("Google HTTP " + code + ": " + read(connection.getErrorStream()));
            }

            JsonObject response = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
            JsonArray items = response.getAsJsonArray("items");
            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    String name = item.get("name").getAsString().replace("models/", "");
                    String displayName = item.has("displayName") ? item.get("displayName").getAsString() : name;
                    models.add(new ModelInfo(name, displayName, provider.getDisplayName()));
                }
            }
        } else {
            String endpoint = provider.getEndpoint().replace("/chat/completions", "/models");
            HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            int code = connection.getResponseCode();
            if (code != 200) {
                throw new RuntimeException(provider.getDisplayName() + " HTTP " + code + ": " + read(connection.getErrorStream()));
            }

            JsonObject response = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
            JsonArray data = response.getAsJsonArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JsonObject item = data.get(i).getAsJsonObject();
                    String id = item.get("id").getAsString();
                    String name = item.has("name") ? item.get("name").getAsString() : id;
                    models.add(new ModelInfo(id, name, provider.getDisplayName()));
                }
            }
        }

        return models;
    }
}
