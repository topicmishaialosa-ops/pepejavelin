package org.blade.utils.autobuy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.blade.utils.ConfigIO;

import java.nio.file.Path;
import java.util.List;

public final class AutoBuyConfig {
    private static final Path CONFIG_PATH = ConfigIO.configDir().resolve("autobuy.json");

    private AutoBuyConfig() {
    }

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
        return ConfigIO.read(CONFIG_PATH);
    }

    private static void saveRoot(String key, JsonArray array) {
        JsonObject root = ConfigIO.read(CONFIG_PATH);
        if (root == null) {
            root = new JsonObject();
        }
        root.add(key, array);
        ConfigIO.write(CONFIG_PATH, root);
    }

    private static <T extends ItemBuy> T findByName(List<T> items, String name) {
        for (T item : items) {
            if (item.getDisplayName().equals(name)) {
                return item;
            }
        }
        return null;
    }
}