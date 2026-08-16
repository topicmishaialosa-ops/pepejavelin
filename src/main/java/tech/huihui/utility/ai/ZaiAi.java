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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ZaiAi {
   private static final String ENDPOINT = "https://api.z.ai/api/paas/v4/chat/completions";
   private static final String MODEL = "glm-4.7-flash";
   private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
      Thread thread = new Thread(runnable, "Z.AI");
      thread.setDaemon(true);
      return thread;
   });

   private ZaiAi() {
   }

   public static void ask(String apiKey, String system, String user, Callback callback) {
      if (apiKey == null || apiKey.isBlank()) {
         callback.onError("API-ключ z.ai не задан (настройки BpAuto)");
         return;
      }
      EXECUTOR.execute(() -> {
         try {
            callback.onResult(request(apiKey, system, user));
         } catch (Exception e) {
            callback.onError(e.getMessage());
         }
      });
   }

   private static String request(String apiKey, String system, String user) throws Exception {
      HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(60000);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setDoOutput(true);

      JsonObject body = new JsonObject();
      body.addProperty("model", MODEL);
      JsonArray messages = new JsonArray();
      JsonObject systemMessage = new JsonObject();
      systemMessage.addProperty("role", "system");
      systemMessage.addProperty("content", system);
      messages.add(systemMessage);
      JsonObject userMessage = new JsonObject();
      userMessage.addProperty("role", "user");
      userMessage.addProperty("content", user);
      messages.add(userMessage);
      body.add("messages", messages);
      JsonObject thinking = new JsonObject();
      thinking.addProperty("type", "disabled");
      body.add("thinking", thinking);
      JsonObject responseFormat = new JsonObject();
      responseFormat.addProperty("type", "json_object");
      body.add("response_format", responseFormat);
      body.addProperty("temperature", 0.2);
      body.addProperty("max_tokens", 2048);

      try (OutputStream output = connection.getOutputStream()) {
         output.write(body.toString().getBytes(StandardCharsets.UTF_8));
      }

      int code = connection.getResponseCode();
      if (code != 200) {
         String error = read(connection.getErrorStream());
         throw new RuntimeException("Z.AI HTTP " + code + ": " + error);
      }
      JsonObject json = JsonParser.parseString(read(connection.getInputStream())).getAsJsonObject();
      JsonArray choices = json.getAsJsonArray("choices");
      if (choices == null || choices.isEmpty()) {
         throw new RuntimeException("Z.AI вернул пустой ответ");
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

   public interface Callback {
      void onResult(String reply);

      void onError(String message);
   }
}