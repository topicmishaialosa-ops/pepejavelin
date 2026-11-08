package org.blade.feature.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockMapSetting extends Setting<Map<String, Integer>> {
    public BlockMapSetting(String name) {
        super(name, new LinkedHashMap<>());
    }

    public Map<String, Integer> getBlocks() {
        return this.getValue();
    }

    public Integer getColor(String id) {
        return this.getValue().get(id);
    }

    public boolean contains(String id) {
        return this.getValue().containsKey(id);
    }

    public void set(String id, int color) {
        Map<String, Integer> copy = new LinkedHashMap<>(this.getValue());
        copy.put(id, color);
        setValue(copy);
    }

    public void remove(String id) {
        Map<String, Integer> copy = new LinkedHashMap<>(this.getValue());
        copy.remove(id);
        setValue(copy);
    }

    public boolean isEmpty() {
        return this.getValue().isEmpty();
    }

    public static String getId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static Block blockFromId(String id) {
        Identifier location = Identifier.tryParse(id);
        return location == null ? null : BuiltInRegistries.BLOCK.getValue(location);
    }

    @Override
    protected Map<String, Integer> normalize(Map<String, Integer> value) {
        return value == null ? new LinkedHashMap<>() : value;
    }

    @Override
    protected JsonElement writeValue(Map<String, Integer> value) {
        JsonObject map = new JsonObject();
        for (Map.Entry<String, Integer> entry : value.entrySet()) {
            map.add(entry.getKey(), new JsonPrimitive(entry.getValue()));
        }
        return map;
    }

    @Override
    protected Map<String, Integer> readValue(JsonElement element) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (element != null && element.isJsonObject()) {
            JsonObject map = element.getAsJsonObject();
            for (String key : map.keySet()) {
                JsonElement value = map.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    result.put(key, value.getAsInt());
                }
            }
        }
        return result;
    }
}