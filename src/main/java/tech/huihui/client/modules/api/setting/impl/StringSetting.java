package tech.huihui.client.modules.api.setting.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Generated;
import tech.huihui.client.modules.api.setting.Setting;

public class StringSetting extends Setting {
   private String value;

   public StringSetting(String name, String value) {
      super(name);
      this.value = value;
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(String.valueOf(this.name), this.value);
   }

   public void load(JsonObject propertiesObject) {
      JsonElement element = propertiesObject.get(String.valueOf(this.name));
      if (element != null && element.isJsonPrimitive()) {
         this.value = element.getAsString();
      }
   }

   @Generated
   public String getValue() {
      return this.value;
   }

   @Generated
   public void setValue(String value) {
      this.value = value;
   }
}
