package tech.huihui.client.modules.api.setting.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import tech.huihui.client.modules.api.setting.Setting;

public class BlockMapSetting extends Setting {
   private final Map<String, Integer> blocks = new HashMap();

   public BlockMapSetting(String name) {
      super(name);
   }

   public Map<String, Integer> getBlocks() {
      return this.blocks;
   }

   public Integer getColor(String id) {
      return this.blocks.get(id);
   }

   public void set(String id, int color) {
      this.blocks.put(id, color);
   }

   public boolean contains(String id) {
      return this.blocks.containsKey(id);
   }

   public void remove(String id) {
      this.blocks.remove(id);
   }

   public void clear() {
      this.blocks.clear();
   }

   public boolean isEmpty() {
      return this.blocks.isEmpty();
   }

   public static String getId(Block block) {
      return Registries.BLOCK.getId(block).toString();
   }

   public void safe(JsonObject propertiesObject) {
      JsonObject map = new JsonObject();
      for (Entry<String, Integer> entry : this.blocks.entrySet()) {
         map.addProperty(entry.getKey(), entry.getValue());
      }
      propertiesObject.add(this.getName(), map);
   }

   public void load(JsonObject propertiesObject) {
      this.blocks.clear();
      JsonElement element = propertiesObject.get(this.getName());
      if (element != null && element.isJsonObject()) {
         JsonObject map = element.getAsJsonObject();
         for (String key : map.keySet()) {
            this.blocks.put(key, map.get(key).getAsInt());
         }
      }
   }
}
