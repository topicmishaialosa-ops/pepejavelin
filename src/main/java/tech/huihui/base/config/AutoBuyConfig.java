package tech.huihui.base.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import tech.huihui.base.autobuy.item.CollectorItemBuy;
import tech.huihui.base.autobuy.item.ItemBuy;

public final class AutoBuyConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public static void loadAutoBuy(List<ItemBuy> items) {
      JsonObject root = readRoot();
      if (root == null) {
         return;
      }

      JsonArray array = root.getAsJsonArray("autobuy");
      if (array == null) {
         return;
      }

      for (JsonElement element : array) {
         try {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            ItemBuy item = findByName(items, name);
            if (item == null) {
               continue;
            }

            if (object.has("price")) {
               item.setPrice(object.get("price").getAsInt());
            }

            if (object.has("enabled")) {
               item.setEnabled(object.get("enabled").getAsBoolean());
            }
         } catch (Exception ignored) {
         }
      }
   }

   public static void saveAutoBuy(List<ItemBuy> items) {
      JsonArray array = new JsonArray();
      for (ItemBuy item : items) {
         JsonObject object = new JsonObject();
         object.addProperty("name", item.getDisplayName());
         object.addProperty("price", item.getPrice());
         object.addProperty("enabled", item.isEnabled());
         array.add(object);
      }

      saveRoot("autobuy", array);
   }

   public static void loadCollect(List<CollectorItemBuy> items) {
      JsonObject root = readRoot();
      if (root == null) {
         return;
      }

      JsonArray array = root.getAsJsonArray("collector");
      if (array == null) {
         return;
      }

      for (JsonElement element : array) {
         try {
            JsonObject object = element.getAsJsonObject();
            String name = object.get("name").getAsString();
            CollectorItemBuy item = findByName(items, name);
            if (item == null) {
               continue;
            }

            if (object.has("count")) {
               item.setCount(object.get("count").getAsInt());
            }

            if (object.has("active")) {
               item.setActive(object.get("active").getAsBoolean());
            }

            if (object.has("price")) {
               item.setPrice(object.get("price").getAsInt());
            }

            if (object.has("enabled")) {
               item.setEnabled(object.get("enabled").getAsBoolean());
            }
         } catch (Exception ignored) {
         }
      }
   }

   public static void saveCollect(List<CollectorItemBuy> items) {
      JsonArray array = new JsonArray();
      for (CollectorItemBuy item : items) {
         JsonObject object = new JsonObject();
         object.addProperty("name", item.getDisplayName());
         object.addProperty("count", item.getCount());
         object.addProperty("active", item.isActive());
         array.add(object);
      }

      saveRoot("collector", array);
   }

   private static JsonObject readRoot() {
      File file = getFile();
      if (!file.exists() || file.length() == 0L) {
         return null;
      }

      try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Exception ignored) {
         return null;
      }
   }

   private static void saveRoot(String key, JsonArray array) {
      File file = getFile();
      JsonObject root = readRoot();
      if (root == null) {
         root = new JsonObject();
      }

      root.add(key, array);
      try {
         File parent = file.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }

         try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(root));
         }
      } catch (Exception ignored) {
      }
   }

   private static File getFile() {
      return new File(ConfigManager.configDirectory, "autobuy.json");
   }

   private static <T extends ItemBuy> T findByName(List<T> items, String name) {
      for (T item : items) {
         if (item.getDisplayName().equals(name)) {
            return item;
         }
      }

      return null;
   }

   private AutoBuyConfig() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}